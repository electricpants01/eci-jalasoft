package com.jumptech.tracklib.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.networking.responses.ConfigResponse;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.ui.BundleTrack;

public class TrackPreferences {
    private SharedPreferences _pref;
    private Gson gson;

    //configuration options from server
    private static final String DRIVER_IMAGE_MAX = "image-max";
    private static final String DRIVER_IMAGE_SHORT_PIXEL = "image-short-pixel";
    private static final String GPS_ENABLED = "gps-enabled";
    private static final String STOP_SORT_ENABLED = "stop-sort-enabled";
    private static final String SYNC_RATE = "sync-rate";
    private static final String UNSCHEDULED_DROPOFF_ENABLED = "unscheduled-dropoff-enabled";
    private static final String UNSCHEDULED_PICKUP_ENABLED = "unscheduled-pickup-enabled";
    private static final String PARTIAL_REASONS = "partial-reasons";

    //runtime
    private static final String AUTH_LOGIN = "auth-login";
    private static final String AUTH_TOKEN = "auth-token";
    private static final String LAST_SERVER_TIME = "last-server-time";
    private static final String SCANNER = "scanner";
    private static final String EULA_PROMPT = "eula-prompt";
    private static final String SIGNATURE_INPROGRESS = "signature-in-progress";
    private static final String IS_ROUTES_INPROGRESS = "is-route-in-progress";
    private static final String IS_DELIVERY_STOP_INPROGRESS = "is-delivery-stop-in-progress";
    private static final String IS_DELIVERY_INPROGRESS = "is-delivery-in-progress";
    private static final String IS_SIGNATURE_IMAGE_INPROGRESS = "is-signature-image-in-progress";
    private static final String DEFAUL_MAP_APP = "default-map-app";

    private static final String LAST_SCREEN_BEFORE_SIGNATURE_KEY = "last-screen-before-signature";
    public enum enumLastScreenBeforeSignature{
        DELIVERY,
        DELIVERY_STOP
    }

    public void set(ConfigResponse config) {
        Editor edit = _pref.edit();
        edit.putInt(DRIVER_IMAGE_MAX, config.getDriverImage().max);
        edit.putInt(DRIVER_IMAGE_SHORT_PIXEL, config.getDriverImage().shortSidePixel);
        if (config.getGps() != null)
            edit.putBoolean(GPS_ENABLED, config.getGps().enabled);
        edit.putBoolean(STOP_SORT_ENABLED, config.isStopSortEnable());
        if (config.getSyncRate() != null)
            edit.putInt(SYNC_RATE, config.getSyncRate());
        edit.putBoolean(UNSCHEDULED_DROPOFF_ENABLED, config.isUnscheduledDropoff());
        edit.putBoolean(UNSCHEDULED_PICKUP_ENABLED, config.isUnscheduledPickup());
        {
            StringBuilder sb = null;
            for (String partial : config.getPartialReasons()) {
                if (sb == null) sb = new StringBuilder();
                else sb.append(";");
                sb.append(partial);
            }
            edit.putString(PARTIAL_REASONS, sb == null ? "" : sb.toString());
        }
        edit.apply();
    }

    public TrackPreferences(Context context) {
        _pref = context.getSharedPreferences("TrackPref", Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public int getImageMax() {
        return _pref.getInt(DRIVER_IMAGE_MAX, 0);
    }

    public int getImageShortPixel() {
        return _pref.getInt(DRIVER_IMAGE_SHORT_PIXEL, 200);
    }

    public boolean getGpsEnabled() {
        return _pref.getBoolean(GPS_ENABLED, false);
    }

    public boolean getStopSortEnabled() {
        return _pref.getBoolean(STOP_SORT_ENABLED, false);
    }

    public int getSyncRate() {
        return _pref.getInt(SYNC_RATE, 300);
    }

    public boolean getUnscheduledPickupEnabled() {
        return _pref.getBoolean(UNSCHEDULED_PICKUP_ENABLED, false);
    }

    public boolean getUnscheduledDropoffEnabled() {
        return _pref.getBoolean(UNSCHEDULED_DROPOFF_ENABLED, false);
    }

    public String[] getPartialReasons() {
        return _pref.getString(PARTIAL_REASONS, "").split(";");
    }

    public long getLastServerTime() {
        return _pref.getLong(LAST_SERVER_TIME, 0);
    }

    public String getLogin() {
        return _pref.getString(AUTH_LOGIN, null);
    }

    public String getAuthToken() {
        return _pref.getString(AUTH_TOKEN, null);
    }

    public void clearAuthToken() {
        _pref.edit().remove(AUTH_TOKEN).apply();
    }

    public void setAuth(String login, String token) {
        _pref.edit().putString(AUTH_LOGIN, login).putString(AUTH_TOKEN, token).apply();
    }

    public void setSignatureInProgress(Signature signature) {
        _pref.edit().putString(SIGNATURE_INPROGRESS, gson.toJson(signature)).apply();
    }

    public Signature getSignatureInProgress() {
        String json = _pref.getString(SIGNATURE_INPROGRESS, null);
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        return gson.fromJson(json, Signature.class);
    }

    public void removeSignatureInProgress() {
        _pref.edit().remove(SIGNATURE_INPROGRESS).apply();
    }


    public Scanner getScanner() {
        return Scanner.valueOf(_pref.getString(SCANNER, Scanner.CAMERA.toString()));
    }

    public void setScanner(Scanner scanner) {
        _pref.edit().putString(SCANNER, scanner.toString()).apply();
    }

    public void setIsEulaPrompt(boolean isEulaPrompt) {
        _pref.edit().putBoolean(EULA_PROMPT, isEulaPrompt).apply();
    }

    public Boolean getIsEulaPrompt() {
        return _pref.getBoolean(EULA_PROMPT, true);
    }


    public Long getStopKey(){
        return _pref.getLong(BundleTrack.KEY_STOP, -1);
    }
    public Long getDeliveryKey(){
        return _pref.getLong(BundleTrack.KEY_DELIVERY,-1);
    }

    public void setRoutesInProgress(){
        updateInProgressFlags(true,false,false,false);
    }

    public void setDeliveryStopInProgress(Long stopKey){
        _pref.edit().putLong(BundleTrack.KEY_STOP, stopKey).apply();
        updateInProgressFlags(false,true,false,false);
    }

    public void setDeliveryInProgress(Long deliveryKey){
        _pref.edit().putLong(BundleTrack.KEY_DELIVERY,deliveryKey).apply();
        updateInProgressFlags(false,false,true,false);
    }

    public void setSignatureImageInProgress(){
        updateInProgressFlags(false,false,false,true);
    }

    public void setLastScreenBeforeSignatureImage(String lastScreen){
       _pref.edit().putString(LAST_SCREEN_BEFORE_SIGNATURE_KEY, lastScreen).apply();
    }

    public String getLastScreenBeforeSignature(){
        return _pref.getString(LAST_SCREEN_BEFORE_SIGNATURE_KEY,"");
    }

    private void updateInProgressFlags(Boolean route, Boolean deliveryStop, Boolean delivery, Boolean signatureImage){
        _pref.edit().putBoolean(IS_ROUTES_INPROGRESS, route)
                .putBoolean(IS_DELIVERY_STOP_INPROGRESS, deliveryStop)
                .putBoolean(IS_DELIVERY_INPROGRESS, delivery)
                .putBoolean(IS_SIGNATURE_IMAGE_INPROGRESS, signatureImage).apply();
    }

    public Boolean isRoutesInProgress(){
        return  _pref.getBoolean(IS_ROUTES_INPROGRESS,false);
    }
    public Boolean isSignatureImageInProgress(){
        return _pref.getBoolean(IS_SIGNATURE_IMAGE_INPROGRESS, false);
    }

    public Boolean isDeliveryStopInProgress(){
        return _pref.getBoolean(IS_DELIVERY_STOP_INPROGRESS, false);
    }

    public Boolean isDeliveryInProgress(){
        return _pref.getBoolean(IS_DELIVERY_INPROGRESS, false);
    }

    public void restartNavigationProgress(){
        _pref.edit().remove(BundleTrack.KEY_DELIVERY)
                    .remove(BundleTrack.KEY_STOP).apply();
        updateInProgressFlags(true,false,false,false);
    }

    public void setDefaulMapApp(String packageName){
        _pref.edit().putString(DEFAUL_MAP_APP, packageName).apply();
    }

    public String getDefaulMapApp(){
        return _pref.getString(DEFAUL_MAP_APP,"");
    }
}
