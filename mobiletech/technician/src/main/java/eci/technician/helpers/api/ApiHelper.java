package eci.technician.helpers.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import eci.technician.helpers.AppAuth;
import eci.technician.helpers.InputStreamConverter;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ApiHelper<T> {
    private static String EXCEPTION = "Exception logger";
    public static final String REQUEST_METHOD_GET = "GET";
    public static final String REQUEST_METHOD_POST = "POST";

    private static final String TAG = "API Helper Response";

    private String url;
    private String method;
    private Object data;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String result;
    private int errorCode;
    private String errorMessage;
    private boolean success = true;
    private Class<T> clazz;
    private boolean authorized;
    private String basicAuthUserName;
    private String basicAuthPassword;

    ApiHelper(String url, String method, Object data, Class<T> clazz, boolean authorized, String basicAuthUsername, String basicAuthPassword) {
        this.url = url;
        this.method = method;
        this.data = data;
        this.clazz = clazz;
        this.authorized = authorized;
        this.basicAuthUserName = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
    }

    private static Gson createGson() {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS Z").create();
    }


    public void runAsync(@NonNull final ApiRequestListener<T> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = getConnectionWithHeader();

                    int responseCode = connection.getResponseCode();
                    if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300) {
                        result = InputStreamConverter.convert(connection.getInputStream());
                        try {
                            Object o = new Gson().fromJson(result, Object.class);
                        } catch (JsonSyntaxException e) {
                            Log.e(TAG, EXCEPTION, e);
                        }
                    } else {
                        errorCode = responseCode;
                        errorMessage = InputStreamConverter.convert(connection.getErrorStream());
                        try {
                            JsonElement jsonElement = createGson().fromJson(errorMessage, JsonElement.class);
                            errorMessage = jsonElement.getAsJsonObject().get("message").getAsString();
                        } catch (Exception e) {
                            Log.e(TAG, EXCEPTION, e);
                        }
                        success = false;
                    }
                } catch (Exception ex) {
                    errorCode = 0;
                    errorMessage = ex.getMessage();
                    success = false;
                }

                if (success) {
                    Log.d(TAG, String.format("%s: %s", url, result));
                } else {
                    Log.d(TAG, "Error: " + errorMessage);
                    Log.d(TAG, String.format("%s: Error: %s", url, errorMessage));
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            try {
                                T result = createGson().fromJson(ApiHelper.this.result, clazz);
                                listener.complete(success, result, 0, null);
                            } catch (Exception e) {
                                Log.e(TAG, EXCEPTION, e);
                                listener.complete(false, null, 0, e.getMessage());
                            }
                        } else {
                            listener.complete(success, null, errorCode, errorMessage);
                        }
                        // FIXME: 2/27/2017 Change without if-else
                    }
                });
            }
        }).start();
    }

    private HttpURLConnection getConnectionWithHeader() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);

            connection.setRequestProperty("Content-Type", "application/json");
            if (authorized) {
                connection.setRequestProperty("Authorization", "Bearer " + AppAuth.getInstance().getToken());
            } else {
                if (basicAuthUserName != null && basicAuthPassword != null) {
                    String credentialsString =  basicAuthUserName + ":" + basicAuthPassword;
                    String basicAuth = "Basic " + Base64.encodeToString(credentialsString.getBytes(), Base64.NO_WRAP);
                    connection.setRequestProperty ("Authorization", basicAuth);
                }
            }

            String charset = "UTF-8";
            connection.setRequestProperty("Accept-Charset", charset);
            if (data != null) {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                String json = createGson().toJson(data);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                bw.write(json);
                bw.flush();
                bw.close();
            }

            return connection;
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public T runSync() throws ApiErrorException {
        try {
            HttpURLConnection connection = getConnectionWithHeader();

            int responseCode = connection.getResponseCode();
            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < 300) {
                result = InputStreamConverter.convert(connection.getInputStream());
                try {
                    Object o = new Gson().fromJson(result, Object.class);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, EXCEPTION, e);
                }
            } else {
                errorCode = responseCode;
                errorMessage = InputStreamConverter.convert(connection.getErrorStream());
                try {
                    JsonElement jsonElement = createGson().fromJson(errorMessage, JsonElement.class);
                    errorMessage = jsonElement.getAsJsonObject().get("message").getAsString();
                } catch (Exception e) {
                    Log.e(TAG, EXCEPTION, e);
                }
                success = false;
            }
        } catch (Exception ex) {
            errorCode = 0;
            errorMessage = ex.getMessage();
            success = false;
        }

        if (success) {
            Log.d(TAG, String.format("%s: %s", url, result));
        } else {
            Log.d(TAG, "Error: " + errorMessage);
            Log.d(TAG, String.format("%s: Error: %s", url, errorMessage));
        }

        if (success) {
            try {
                return createGson().fromJson(this.result, clazz);
            } catch (Exception e) {
                Log.e(TAG, EXCEPTION, e);
                throw new ApiErrorException(0, e.getMessage());
            }
        } else {
            throw new ApiErrorException(errorCode, errorMessage);
        }
        // FIXME: 3/1/2017 Make 1 method for sync and async
    }

    public interface ApiRequestListener<T> {
        void complete(boolean success, T result, int errorCode, String errorMessage);
    }
}
