package com.jumptech.networking;

import com.google.gson.JsonElement;
import com.jumptech.networking.responses.AuthBody;
import com.jumptech.networking.responses.AuthResponse;
import com.jumptech.networking.responses.EulaBody;
import com.jumptech.tracklib.comms.CommandPrompt;
import com.jumptech.tracklib.data.Gdr;
import com.jumptech.tracklib.data.Product;

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface JTMobileApi {

    @POST("/rhMobile?format=RAW&type=auth")
    Call<AuthResponse> login(@Body AuthBody authBody, @Query("v") int version);

    @POST("/rhMobile?format=RAW&type=eula")
    Call<Void> eula(@Body EulaBody eulaBody, @Query("v") int version);

    @GET("/rhMobile?format=RAW&type=gdr")
    Call<List<Gdr>> gdr(@Query("level") String level, @Query("path") String path, @Query("key") String key, @Query("v") int version);

    @GET("/rhMobile?format=RAW&type=route")
    Call<JsonElement> route(@Query("routeKey") long routeKey, @Query("v") int version);

    @POST("/rhMobile?format=RAW&type=product")
    Call<List<Product>> product(@Body String[] search, @Query("v") int version);

    @POST("/rhMobile?format=RAW&type=signature")
    Call<CommandPrompt> signature(@Body JsonElement json, @Query("v") int version);

    @POST("/rhMobile?format=RAW&type=stop-order")
    Call<Void> stopOrder(@Body List<Long> stopKeys, @Query("routeKey") long routeKey, @Query("v") int version);

    @Headers( {"Content-Type: application/octet-stream"})
    @POST("/rhMobile?format=FILE&type=crumb")
    Call<Void> crumbs(@Body RequestBody file, @Query("routeKey") long routeKey, @Query("finished") boolean finished, @Query("v") int version);

    @POST("/rhMobile?format=FILE&type=photo")
    Call<Void> photo(@Body RequestBody photo, @Query("sigId") String sigId, @Query("photoId") int photoId, @Query("photoPending") boolean pending, @Query("v") int version);
}
