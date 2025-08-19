package com.jumptech.tracklib.utils.scan.integrator;

import android.app.Application;

import com.socketmobile.capture.android.Capture;

/**
 * Defines the application for zxing-android library
 */
public class ZxingClientApp extends Application {

    /**
     * Stores the observers for scanner
     */
    private ScannerObserver scannerObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        scannerObserver = new ScannerObserver();

        Capture.builder(getApplicationContext())
                .enableLogging(true)
                .build();
    }

    public ScannerObserver getObserver() {
        return scannerObserver;
    }
}
