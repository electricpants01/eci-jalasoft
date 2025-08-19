package com.jumptech.tracklib.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jumptech.jumppod.R;
import com.jumptech.ui.LineActivity;

import java.util.List;

/**
 * This class contains all common methods in regards to device permissions
 */
public class PermissionUtil {

    public static final int REQUEST_PERMISSION = 1100;
    public static final int REQUEST_CAMERA = 1000;
    public static final int REQUEST_GPS = 1001;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1002;
    public static final int REQUEST_CAMERA_FOR_SCANNER = 1003;
    public static final int REQUEST_TRACK_PERMISSION = 111;
    public static final int REQUEST_TRACK_BACKGROUND_PERMISSION = 222;
    public static final int REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION = 333;
    public static final int REQUEST_FINE_AND_COARSE_LOCATION = 444;

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity context, List<String> permissions, int requestCode) {
        ActivityCompat.requestPermissions(context, permissions.toArray(new String[permissions.size()]), requestCode);
    }

    public static void requestPermission(Activity context, String permission, int requestCode) {
        ActivityCompat.requestPermissions(context, new String[]{permission}, requestCode);
    }

    public static void displayGrantPermissionMessage(final Activity activity, int grantPermissionMsg, int titlePermissionMsg, int positiveButton, int negativeButtonMessage) {
        new AlertDialog.Builder(activity)
                .setTitle(titlePermissionMsg)
                .setMessage(grantPermissionMsg)
                .setNegativeButton(negativeButtonMessage, null)
                .setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        IntentHelper.goToSettings(activity);
                    }
                })
                .create()
                .show();
    }

    public static void displaySimpleGrantPermissionMessage(final Activity activity, int grantPermissionMsg) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setMessage(grantPermissionMsg)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    /**
     * This method allows verify permissions for camera usage either take photo or use camera as scanner
     * notice that if grantResults has 2 elements, at time to request permissions it should follow
     * this order [CAMERA, WRITE_EXTERNAL_STORAGE]
     *
     * @param grantResults array with grant-permissions result
     */
    public static Integer checkForCameraPermission(int[] grantResults) {
        Integer result = null;
        if (grantResults.length == 1
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            result = R.string.support_should_allow_camera_msg;
        } else if (grantResults.length == 2) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                result = R.string.support_should_allow_camera_msg;
            } else if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                result = R.string.support_should_allow_storage_msg;
            }
        }
        return result;
    }

    public static String getReadableLocationPermission(Context context) {
        if (PermissionUtil.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && !PermissionUtil.hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                return context.getResources().getString(R.string.location_when_in_use);
            }
            return context.getResources().getString(R.string.location_always);
        }
        return context.getResources().getString(R.string.location_no_access);
    }

}
