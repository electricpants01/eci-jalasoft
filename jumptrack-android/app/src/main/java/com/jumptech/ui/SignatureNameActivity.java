package com.jumptech.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jumptech.jumppod.R;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.db.Business;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.SignatureRepository;
import com.jumptech.tracklib.utils.factories.AlertDialogFactory;
import com.jumptech.tracklib.utils.factories.AsyncProgressTaskFactory;

import org.apache.commons.lang.StringUtils;

import java.io.File;

public class SignatureNameActivity extends Activity {

    public static final String TAG = SignatureNameActivity.class.getSimpleName();
    private Signature signature;

    /**
     * Indicates if this activity was started according to recovered process
     */
    private boolean isRecovered = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signature_name_activity);

        final EditText receivedByText = (EditText) findViewById(R.id.receivedByText);
        final Button doneButton = (Button) findViewById(R.id.acceptRefuseButton);

        receivedByText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        signature = new TrackPreferences(this).getSignatureInProgress();
        if (signature == null) {
            signature = SignatureRepository.newSignature(this);
            new TrackPreferences(this).setSignatureInProgress(signature);
            isRecovered = false;
        } else {
            isRecovered = true;
        }

        ((TextView) findViewById(R.id.receivedByCompanyNameLabel)).setText(DeliveryRepository.signingName(this));

        Bitmap bitmap = BitmapFactory.decodeFile(signature._path);

        if (null != bitmap) {
            ((ImageView) findViewById(R.id.signatureImage)).setImageBitmap(bitmap);
        } else {
            doneButton.setClickable(false);

            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(R.string.unableToSaveSig)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            backNavigation();
                        }
                    })
                    .create()
                    .show();
        }

        doneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doneButton.setEnabled(false);
                final String name = StringUtils.trimToNull(receivedByText.getText().toString());
                if (name != null) {
                    final Business business = Business.from(SignatureNameActivity.this);
                    try {
                        business.signatureFinalize(name);
                        AsyncProgressTaskFactory.createSyncQuietTask(business).execute();
                        new TrackPreferences(SignatureNameActivity.this).removeSignatureInProgress();
                        startActivity(new Intent(SignatureNameActivity.this, RouteActivity.class));
                        finish();
                    } catch (TrackException e) {
                        Log.e("ndb", getString(R.string.unablePersistSignature), e);
                        AlertDialogFactory.createInfoDialog(SignatureNameActivity.this, R.string.unablePersistSignature).show();
                        doneButton.setEnabled(true);
                    }
                } else {
                    AlertDialogFactory.createInfoDialog(SignatureNameActivity.this, R.string.emptySignatureName).show();
                    doneButton.setEnabled(true);
                }
            }
        });
    }

    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(R.string.signatureProcessCancel)
                .setPositiveButton(R.string.cancelSignatureLabel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!new File(signature._path).delete()) {
                            Log.e(TAG, "Error removing signature discarded");
                        }
                        backNavigation();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

    /**
     * Determines the back navigation event
     */
    private void backNavigation() {
        new TrackPreferences(SignatureNameActivity.this).removeSignatureInProgress();
        TrackPreferences preferences = new TrackPreferences(this);
        if (isRecovered) {
            if(preferences.getLastScreenBeforeSignature().equals(TrackPreferences.enumLastScreenBeforeSignature.DELIVERY_STOP.name())){
                startActivity(new Intent(this,StopActivity.class).putExtra(BundleTrack.KEY_STOP, preferences.getStopKey()));
                finish();
            }else if(preferences.getLastScreenBeforeSignature().equals(TrackPreferences.enumLastScreenBeforeSignature.DELIVERY.name())){
                startActivity(new Intent(this, DeliveryActivity.class).putExtra(BundleTrack.KEY_DELIVERY, preferences.getDeliveryKey()));
                finish();
            }else{
                startActivity(new Intent(this, RouteActivity.class));
                finish();
            }
        } else {
            SignatureNameActivity.super.onBackPressed();
        }
    }
}
