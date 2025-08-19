package com.jumptech.networking.responses;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.google.gson.annotations.SerializedName;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.BuildConfig;
import com.jumptech.jumppod.R;

public class AuthBody {

    @SerializedName("login")
    private String username;
    @SerializedName("passwd")
    private String password;
    private String platform;
    private String device;
    private String branding;
    private NotificationBody notification;
    private EulaBody eula;

    public AuthBody(String username, String password, Context context) {
        this.username = username;
        this.password = password;
        this.platform = "Android " + Build.VERSION.RELEASE;
        this.device = Util.getDeviceName();
        this.branding = context.getString(R.string.app_name);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String token = sharedPreferences.getString(context.getString(R.string.fcm_token_key), "");
        this.notification = new NotificationBody(BuildConfig.NotificationType, token);

        this.eula = new EulaBody(context.getString(R.string.eulaName, context.getString(R.string.app_name)), context.getString(R.string.eulaVersion));
    }
}
