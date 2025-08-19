package com.jumptech.tracklib.utils.scan;

import android.app.Activity;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Collection;

/**
 * This class performs Scan launcher
 */
public class ScanLauncher {

    /** Class tag */
    private static final String TAG = ScanLauncher.class.getName();

    /**
     * Starts the scanner feature from an screen activity
     *
     * @param screenActivity an AppCompatActivity instance
     */
    public static void LaunchScanner(Activity screenActivity, Collection<String> formats) {
        LaunchScanner(new IntentIntegrator(screenActivity), formats);
    }

    /**
     * Starts the scanner feature from an screen activity
     *
     * @param screenActivity an AppCompatActivity instance
     */
    public static void LaunchScanner(AppCompatActivity screenActivity, Collection<String> formats) {
        LaunchScanner(new IntentIntegrator(screenActivity), formats);
    }

    /**
     * Starts the scanner feature from an screen activity
     *
     * @param screenActivity an AppCompatActivity instance
     */
    public static void LaunchScanner(AppCompatActivity screenActivity) {
        LaunchScanner(new IntentIntegrator(screenActivity), IntentIntegrator.ALL_CODE_TYPES);
    }

    /**
     * Starts the scanner feature from a fragment activity
     *
     * @param fragment a Fragment instance
     */
    public static void LaunchScanner(Fragment fragment) {
        LaunchScanner(IntentIntegrator.forSupportFragment(fragment), IntentIntegrator.ALL_CODE_TYPES);
    }

    /**
     * Starts the scanner feature
     *
     * @param integrator an IntentIntegrator instance
     */
    private static void LaunchScanner(IntentIntegrator integrator, Collection<String> formats) {
        try {
            integrator.setDesiredBarcodeFormats(formats);
            integrator.setPrompt("Scan a barcode");
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
        } catch (Exception e) {
            Log.e(TAG, "LaunchScanner exception: ", e);
        }
    }
}
