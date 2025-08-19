package com.jumptech.networking.interceptors;

import com.jumptech.jumppod.UtilTrack;

import java.io.IOException;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    private String authToken;
    private String versionName;
    private String locationPermission;

    public AuthenticationInterceptor(String token, String versionName, String locationPermission) {
        this.authToken = token;
        this.versionName = versionName;
        this.locationPermission = locationPermission;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request.Builder builder = original.newBuilder()
                .header("Authorization", authToken == null ? "" : authToken)
                .header("X-Track-App", versionName)
                .header("X-Track-DateTime", UtilTrack.formatServer(new Date()))
                .header("X-Track-GPS-Rights", locationPermission);

        Request request = builder.build();
        return chain.proceed(request);
    }
}
