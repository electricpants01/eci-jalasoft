package com.jumptech.networking;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.UtilTrack;
import com.jumptech.networking.responses.AuthBody;
import com.jumptech.networking.responses.AuthResponse;
import com.jumptech.networking.responses.ConfigResponse;
import com.jumptech.networking.responses.EulaBody;
import com.jumptech.tracklib.comms.CommandPrompt;
import com.jumptech.tracklib.comms.TrackAuthException;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.Gdr;
import com.jumptech.tracklib.data.Line;
import com.jumptech.tracklib.data.Plate;
import com.jumptech.tracklib.data.Product;
import com.jumptech.tracklib.data.Prompt;
import com.jumptech.tracklib.data.Route;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.CrumbRepository;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.LineRepository;
import com.jumptech.tracklib.repository.PhotoRepository;
import com.jumptech.tracklib.repository.PlateRepository;
import com.jumptech.tracklib.repository.PromptRepository;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.repository.SignatureRepository;
import com.jumptech.tracklib.repository.UtilRepository;
import com.jumptech.tracklib.room.entity.Crumb;
import com.jumptech.ui.LoginActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JTRepository {

    private static final String TAG = JTRepository.class.getSimpleName();

    private static final int VERSION = 11;

    public static void authenticate(String username, String password, final AppCompatActivity context, final OnAuthResponse listener) {
        AuthBody authBody = new AuthBody(username, password, context);
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<AuthResponse> call = mobileApi.login(authBody, VERSION);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    AuthResponse authResponse = response.body();
                    if (TextUtils.isEmpty(authResponse.getAuthorization())) {
                        listener.error(context.getString(R.string.authenticationError));
                    } else {
                        listener.success(authResponse);
                    }
                } else if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    listener.error(context.getString(R.string.authenticationIncorrect));
                } else {
                    listener.error(context.getString(R.string.authenticationError));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                listener.error(context.getString(R.string.authenticationError));
            }
        });
    }

    public static void eula(final AppCompatActivity context, final OnServiceResponse listener) {
        String eulaName = context.getString(R.string.eulaName, context.getString(R.string.app_name));
        String eulaVersion = context.getString(R.string.eulaVersion);
        EulaBody eulaBody = new EulaBody(eulaName, eulaVersion);

        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<Void> call = mobileApi.eula(eulaBody, VERSION);
        call.enqueue(new ProgressCallback<Void>(context) {
            @Override
            public void onResponse(Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.success();
                } else {
                    listener.error(context.getString(R.string.eulaRequestError));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                listener.error(context.getString(R.string.eulaRequestError));
            }
        });
    }

    public static void loadList(Gdr gdr, Gdr.Path path, final AppCompatActivity context, final OnGdrResponse listener) {
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<List<Gdr>> call = mobileApi.gdr(gdr.getLevel().name(), path.name(), gdr.getKey() == null ? "" : String.valueOf(gdr.getKey()), VERSION);
        call.enqueue(new Callback<List<Gdr>>() {
            @Override
            public void onResponse(Call<List<Gdr>> call, Response<List<Gdr>> response) {
                if (response.isSuccessful()) {
                    List<Gdr> gdrs = response.body();
                    listener.success(gdrs.toArray(new Gdr[gdrs.size()]));
                } else if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    new TrackPreferences(context).clearAuthToken();
                    listener.error(response.message());
                } else {
                    listener.error(null);
                }
            }

            @Override
            public void onFailure(Call<List<Gdr>> call, Throwable t) {
                listener.error(t.getMessage());
            }
        });
    }

    public static void route(Gdr gdr, final AppCompatActivity context, final OnServiceResponse listener) {
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<JsonElement> call = mobileApi.route(gdr.getKey(), VERSION);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful()) {
                    JsonElement json = response.body();
                    Prompt prompt = PromptRepository.storeFetch(context, json);
                    if (prompt == null) {
                        listener.success();
                    } else {
                        listener.error(prompt.getMessage());
                    }
                } else if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    new TrackPreferences(context).clearAuthToken();
                    listener.error(context.getString(R.string.deliveryStopListFromServerError));
                } else {
                    listener.error(context.getString(R.string.deliveryStopListFromServerError));
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                listener.error(context.getString(R.string.deliveryStopListFromServerError));
            }
        });
    }

    public static Prompt routeSynchronous(long routeKey, Context context) throws TrackException {
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<JsonElement> call = mobileApi.route(routeKey, VERSION);
        try {
            Response<JsonElement> response = call.execute();
            JsonElement json = response.body();
            JsonObject config = json.getAsJsonObject().getAsJsonObject("config");
            ConfigResponse configResponse = new Gson().fromJson(config, ConfigResponse.class);
            new TrackPreferences(context).set(configResponse);
            return PromptRepository.storeFetch(context, json);
        } catch (IOException e) {
            throw new TrackException(e);
        }
    }

    public static void searchProduct(String search, final FragmentActivity context, final OnProductResponse listener) {
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<List<Product>> call = mobileApi.product(new String[]{search}, VERSION);
        call.enqueue(new ProgressCallback<List<Product>>(context) {
            @Override
            public void onResponse(Response<List<Product>> response) {
                if (response.isSuccessful()) {
                    List<Product> products = response.body();
                    if (products!=null && !products.isEmpty()) {
                        listener.success(products.get(0));
                    } else {
                        listener.error(null);
                    }
                } else {
                    if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        new TrackPreferences(context).clearAuthToken();
                    }
                    listener.error(null);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                listener.error(null);
            }
        });
    }

    public static CommandPrompt signature(Stop stop, Context context) throws TrackException {
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<CommandPrompt> call = mobileApi.signature(buildDelivery(context, stop), VERSION);
        try {
            Response<CommandPrompt> response = call.execute();
            if (!response.isSuccessful()) {
                if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    new TrackPreferences(context).clearAuthToken();
                    context.startActivity(new Intent(context, LoginActivity.class));
                    throw new TrackAuthException();
                }
                throw new TrackException();
            }
            return response.body();
        } catch (IOException e) {
            throw new TrackException(e);
        }
    }

    private static JsonElement buildDelivery(final Context currentContext, Stop stop) {
            Gson gson = new GsonBuilder()
                    .registerTypeHierarchyAdapter(Stop.class, new JsonSerializer<Stop>() {
                        @Override
                        public JsonElement serialize(Stop stop, Type type, JsonSerializationContext context) {
                            JsonObject jo = new JsonObject();
                            //TODO unscheduled reference stop
                            {
                                Signature sig = SignatureRepository.fetchSignature(currentContext, stop.signature_key);
                                //TODO signature_reference
                                jo.addProperty("unscheduled-base", stop.baseDeliveryKey);
                                jo.addProperty("sig-id", sig._reference);
                                jo.addProperty("note", sig._note);
                                try {
                                    jo.addProperty("image", Base64.encodeToString(IOUtils.toByteArray(new FileInputStream(new File(sig._path))), Base64.NO_WRAP));
                                } catch (IOException ioe) {
                                    Log.e(TAG, "Problem encoding image", ioe);
                                }
                                jo.addProperty("signer", sig._signee);
                                jo.addProperty("signed", UtilTrack.formatServer(sig._signed));
                                jo.addProperty("crumb", sig._crumb);
                            }
                            jo.addProperty("photo-pending", PhotoRepository.photos(currentContext, stop.signature_key).length > 0);
                            {
                                List<Delivery> deliveries = DeliveryRepository.getAllDeliveriesByStopKey(currentContext, stop.key);
                                jo.add("deliverys", context.serialize(deliveries));
                            }
                            return jo;
                        }
                    })
                    .registerTypeHierarchyAdapter(Delivery.class, new JsonSerializer<Delivery>() {
                        @Override
                        public JsonElement serialize(Delivery delivery, Type type, JsonSerializationContext context) {
                            List<Line> lines = LineRepository.fetchLines(currentContext, delivery.id, null ,null);
                            JsonObject jo = new JsonObject();
                            if (delivery.id > 0) jo.addProperty("key", delivery.id);
                            else jo.addProperty("delivery-cd", delivery.display);
                            jo.add("type", context.serialize(delivery.type));
                            jo.add("lines", context.serialize(lines));
                            return jo;
                        }
                    })
                    .registerTypeHierarchyAdapter(Line.class, new JsonSerializer<Line>() {
                        @Override
                        public JsonElement serialize(Line line, Type type, JsonSerializationContext context) {
                            JsonObject jo = new JsonObject();
                            //if server line
                            if (line._key > 0) jo.addProperty("key", line._key);
                                //else unscheduled
                            else {
                                jo.addProperty("name", line._name);
                                jo.addProperty("product-no", line._product_no);
                                jo.addProperty("uom", line._uom);
                                jo.addProperty("desc", line._desc);
                            }
                            jo.addProperty("qty-accept", line._qty_accept);
                            jo.addProperty("scanned", line._scanning);
                            jo.add("partial-reason", context.serialize(line._partial_reason));

                            if (line._scanning) {
                                List<String> plateScanned = new ArrayList<>();
                                {
                                    List<Plate> plates = PlateRepository.getPlateByLineId(currentContext,line._key);
                                    for( Plate plate: plates){
                                        plateScanned.add(plate.plate);
                                    }
                                }
                                if (plateScanned.size() > 0) {
                                    jo.add("license-plate", context.serialize(plateScanned));
                                } else {
                                    jo.add("license-plate", context.serialize(UtilRepository.splitNotNull(line._scan, ";")));
                                }
                            }

                            return jo;
                        }
                    })
                    .setPrettyPrinting()
                    .create();

            return gson.toJsonTree(stop);
    }

    public static void stopOrder(List<Long> stopKeys, Context context) throws TrackException {
        Route route = RouteRepository.fetchRoute(context);

        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        Call<Void> call = mobileApi.stopOrder(stopKeys, route._key, VERSION);
        try {
            Response response = call.execute();
            if (!response.isSuccessful()) {
                if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    new TrackPreferences(context).clearAuthToken();
                    throw new TrackAuthException();
                }
                throw new TrackException();
            }
        } catch (IOException e) {
            throw new TrackException(e);
        }
    }

    public static void crumbs(long routeKey, boolean finished, Context context) throws TrackException {
        try {
            JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
            long totalCrumbs = CrumbRepository.INSTANCE.getTotalCrumbs(context);
            String fileName = UUID.randomUUID().toString() + ".csv";
            if (totalCrumbs > 0) {
                List<Crumb> crumbList = CrumbRepository.INSTANCE.getCrumbs(CrumbRepository.CRUMB_SIZE, context);
                String allData = crumbList.stream().map(Crumb::getEncodedCSV).collect(Collectors.joining());
                File file = new File(context.getFilesDir(), fileName);
                FileUtils.write(file, allData, Charset.defaultCharset(), true);
                byte[] buf = FileUtils.readFileToByteArray(file);
                RequestBody requestBody = RequestBody.create(buf, MediaType.parse("application/octet-stream"));
                Call<Void> call = mobileApi.crumbs(requestBody, routeKey, finished, VERSION);
                Response<Void> response = call.execute();
                if (!response.isSuccessful()) {
                    if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        new TrackPreferences(context).clearAuthToken();
                        throw new TrackAuthException();
                    }
                    throw new TrackException();
                } else {
                    CrumbRepository.INSTANCE.deleteCrumbs(crumbList, context);
                    crumbs(routeKey, finished, context);
                }
                file.delete();
            }
        } catch (Exception ioException) {
            throw new TrackException();
        }
    }

    public static void photo(String sigReference, String path, boolean pending, Context context) throws TrackException {
        JTMobileApi mobileApi = RetrofitService.createService(JTMobileApi.class, context);
        File originalFile = new File(path);
        try {
            InputStream in = new FileInputStream(originalFile);
            byte[] buf = new byte[in.available()];
            while (in.read(buf) != -1) ;

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), buf);
            Call<Void> call = mobileApi.photo(requestBody, sigReference, path.hashCode(), pending, VERSION);
            Response response = call.execute();
            if (!response.isSuccessful()) {
                if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    new TrackPreferences(context).clearAuthToken();
                    throw new TrackAuthException();
                }
                throw new TrackException();
            }
        } catch (IOException e) {
            throw new TrackException(e);
        }
    }

    public interface OnAuthResponse {
        void success(AuthResponse authResponse);

        void error(String message);
    }

    public interface OnGdrResponse {
        void success(Gdr[] gdrs);

        void error(String message);
    }

    public interface OnServiceResponse {
        void success();

        void error(String message);
    }

    public interface OnProductResponse {
        void success(Product product);

        void error(String message);
    }
}
