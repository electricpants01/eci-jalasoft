package com.jumptech.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.jumptech.jumppod.BuildConfig;
import com.jumptech.jumppod.R;

public class Preferences {

    private static final String TAG = Preferences.class.getSimpleName();

    public static String defaultBaseUrl = BuildConfig.DEBUG ? "test.myjumptrack.com" : "comsat.myjumptrack.com";

    private static void saveString(int keyResource, String value, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(keyResource), value);
        editor.apply();
    }

    private static String getString(int keyResource, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(context.getString(keyResource), null);
    }

    private static void saveLong(int keyResource, long value, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(context.getString(keyResource), value);
        editor.apply();
    }

    private static long getLong(int keyResource, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(context.getString(keyResource), 0);
    }

    private static void saveBool(int keyResource, boolean value, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(keyResource), value);
        editor.apply();
    }


    private static boolean getBool(int keyResource, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(keyResource), false);
    }

    private static void clear(int keyResource, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(context.getString(keyResource));
        editor.apply();
    }

    public static void setPendingMessage(String message, Context context) {
        saveString(R.string.pending_message_key, message, context);
    }

    public static String getPendingMessage(Context context) {
        return getString(R.string.pending_message_key, context);
    }

    public static void clearPendingMessage(Context context) {
        clear(R.string.pending_message_key, context);
    }

    public static void setIsDialogDenied(Context context, boolean isDialogDenied){
        saveBool(R.string.location_is_dialog_denied_key, isDialogDenied, context);
    }

    public static boolean getIsDialogDenied(Context context){
        try {
            return getBool(R.string.location_is_dialog_denied_key, context);
        }catch (Exception e){
            return false;
        }
    }

    public static void setIgnoreCameraScan(boolean value, Context context) {
        saveBool(R.string.ignore_camera_scan_key, value, context);
    }

    public static boolean ignoreCameraScan(Context context) {
        return getBool(R.string.ignore_camera_scan_key, context);
    }

    public static void setCurrentDeliveryKey(long value, Context context) {
        saveLong(R.string.current_delivery_key, value, context);
    }

    public static long getCurrentDeliveryKey(Context context) {
        return getLong(R.string.current_delivery_key, context);
    }

    public static void clearCurrentDeliveryKey(Context context) {
        clear(R.string.current_delivery_key, context);
    }

    public static void setBaseUrl(String baseURL, Context context) {
        saveString(R.string.base_url_key, baseURL, context);
    }

    public static String baseUrl(Context context) {
        String protocol = "https://";
        String baseUrl = getString(R.string.base_url_key, context);
        if (TextUtils.isEmpty(baseUrl)) {
            return protocol + defaultBaseUrl;
        }
        return baseUrl.startsWith("http") ? baseUrl : protocol + baseUrl;
    }

    public static boolean isDefaultBaseUrl(Context context) {
        return baseUrl(context).contains(defaultBaseUrl);
    }

}
