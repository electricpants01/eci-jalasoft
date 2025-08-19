package eci.technician.helpers.api;

import android.net.Uri;
import android.util.Log;

import eci.technician.helpers.AppAuth;

public class ApiHelperBuilder<T> {
    private Uri.Builder builder;
    private String method = ApiHelper.REQUEST_METHOD_GET;
    private Object data;
    private Class<T> clazz;
    private boolean authorized;
    private String basicAuthUserName;
    private String basicAuthPassword;
    private static final String TAG = "ApiHelperBuilder";

    public ApiHelperBuilder(Class<T> clazz) {
        this(AppAuth.getInstance().getServerAddress(), clazz);
    }

    public ApiHelperBuilder(String url, Class<T> clazz) {
        builder = Uri.parse(url).buildUpon();
        this.clazz = clazz;
    }

    public ApiHelperBuilder<T> addPath(String path) {
        builder.appendPath(path);
        return this;
    }

    public ApiHelperBuilder<T> addParameter(String key, String value) {
        builder.appendQueryParameter(key, value);
        return this;
    }

    public ApiHelperBuilder<T> setMethodPost(Object data) {
        method = ApiHelper.REQUEST_METHOD_POST;
        this.data = data;
        return this;
    }

    public ApiHelperBuilder<T> setAuthorized(boolean authorized) {
        this.authorized = authorized;
        return this;
    }

    public ApiHelperBuilder<T> setBasicAuthCredentials(String username, String password) {
        this.basicAuthUserName = username;
        this.basicAuthPassword = password;
        return this;
    }

    public ApiHelper<T> build() {
        Log.d(TAG, builder.toString());
        return new ApiHelper<T>(builder.toString(), method, data, clazz, authorized, basicAuthUserName, basicAuthPassword);
    }
}
