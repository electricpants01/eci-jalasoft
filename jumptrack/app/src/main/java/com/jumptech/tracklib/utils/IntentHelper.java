package com.jumptech.tracklib.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class IntentHelper {

    public static void goToSettings(Activity originActivity){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", "com.jumptech.jumppod", null);
        intent.setData(uri);
        originActivity.startActivity(intent);
    }
}
