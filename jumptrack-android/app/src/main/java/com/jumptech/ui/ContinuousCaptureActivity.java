package com.jumptech.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.utils.scan.integrator.ZxingClientApp;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ContinuousCaptureActivity extends Activity {

    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;

    /** Stores the application context */
    private ZxingClientApp mApplication;

    private final Handler handler = new Handler();
    private static final long DELAY_BEEP = 2000;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || result.getText().equals(lastText)) {
                return;
            }
            barcodeView.pause();
            lastText = result.getText();
            Log.i(TAG, "barcodeResult: " + lastText);
            barcodeView.setStatusText(lastText);
            beepManager.playBeepSoundAndVibrate();
            mApplication.getObserver().getInput().emit(lastText);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_capture);

        barcodeView = findViewById(R.id.barcode_scanner);
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(this);

        mApplication = (ZxingClientApp) getApplicationContext();
        mApplication.getObserver().getOutput().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                if (data instanceof Boolean) {
                    boolean finish = (Boolean) data;
                    if (finish) {
                        Log.i(TAG, "Scan finished");
                        ContinuousCaptureActivity.this.finish();
                    } else {
                        Log.i(TAG, "Scan continue");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                barcodeView.resume();
                                lastText = null;
                            }
                        }, DELAY_BEEP);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        barcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        mApplication.getObserver().getOutput().deleteObservers();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
