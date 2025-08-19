package com.jumptech.networking.responses;

import com.google.gson.annotations.SerializedName;

public class NotificationBody {
    private String type;
    @SerializedName("key")
    private String token;

    public NotificationBody(String type, String token) {
        this.type = type;
        this.token = token;
    }
}
