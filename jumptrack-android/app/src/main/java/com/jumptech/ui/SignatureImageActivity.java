package com.jumptech.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Image;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.SignatureRepository;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.UUID;

public class SignatureImageActivity extends Activity {

	private static final String TAG = SignatureImageActivity.class.getSimpleName();

	private SignaturePad mSignaturePad;
	private Button mClearButton;
	private Button mSaveButton;
	private TrackPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signaturepad_activity);

		preferences = new TrackPreferences(this);
		preferences.setSignatureImageInProgress();
		mSignaturePad = (SignaturePad) findViewById(R.id.signature_pad);
		mSignaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
			@Override
			public void onStartSigning() {
			}

			@Override
			public void onSigned() {
				mSaveButton.setEnabled(true);
				mClearButton.setEnabled(true);
			}

			@Override
			public void onClear() {
				mSaveButton.setEnabled(false);
				mClearButton.setEnabled(false);
			}
		});

		mClearButton = (Button) findViewById(R.id.clear_button);
		mSaveButton = (Button) findViewById(R.id.save_button);

		mClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mSignaturePad.clear();
			}
		});

		mSaveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mSaveButton.setEnabled(false);
				mClearButton.setEnabled(false);
				saveSignature(mSignaturePad.getTransparentSignatureBitmap(true));
			}
		});
	}

	private void saveSignature(Bitmap bitmap) {
		String signatureFile = null;
		try {
			signatureFile = Image.formatPNG(bitmap, Util.getSharedFile(this, UUID.randomUUID() + ".png").getAbsolutePath(), 200);
		} catch(IOException e) {
			Log.e(TAG, "unable to get target file", e);
		}
		if (StringUtils.isEmpty(signatureFile)) {
			new AlertDialog.Builder(this)
			.setCancelable(false)
			.setMessage(R.string.emptySig)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.create()
			.show();
		} else {
			SignatureRepository.updateImage(this, Signature.NEW, signatureFile);
			startActivity(new Intent(this, SignatureNameActivity.class));
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		startActivity(new Intent(this, StopActivity.class).putExtra(BundleTrack.KEY_STOP, preferences.getStopKey()));
		finish();
	}
}
