package com.jumptech.networking;

import android.content.Context;

import com.jumptech.android.util.Preferences;
import com.jumptech.jumppod.R;
import com.jumptech.networking.interceptors.AuthenticationInterceptor;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.utils.PermissionUtil;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitService {

    public static HttpLoggingInterceptor logging = new HttpLoggingInterceptor();


    public static OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS);

    private static Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl("https://" + Preferences.defaultBaseUrl)
            .addConverterFactory(GsonConverterFactory.create());

    private static Retrofit retrofit = builder.build();

    public static <S> S createService(Class<S> serviceClass, Context context) {
        String authToken = new TrackPreferences(context).getAuthToken();
        String baseUrl = Preferences.baseUrl(context);
        String versionName = context.getString(R.string.versionName);
        logging.level(HttpLoggingInterceptor.Level.BODY);
        builder.baseUrl(baseUrl);
        httpClient.interceptors().clear();
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(authToken, versionName, PermissionUtil.getReadableLocationPermission(context));
        httpClient.addInterceptor(interceptor);
        httpClient.addInterceptor(logging);
        builder.client(httpClient.build());
        retrofit = builder.build();
        return retrofit.create(serviceClass);
    }
}
