package com.jumptech.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.jumptech.android.util.Util;
import com.jumptech.jumppod.model.Image;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.PhotoRepository;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

public class SignaturePhotoActivity extends Activity {

	/**
	 * Log tag
	 */
	private static final String TAG = SignaturePhotoActivity.class.getSimpleName();

	private static final int CAMERA_REQUEST = 1000;
	
	private File _photoPath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_photoPath = new File(getExternalFilesDir(null),UUID.randomUUID() + ".jpeg");
		try {
			_photoPath.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, TrackException.MSG_EXCEPTION, e);
		}
		Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", _photoPath);
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		startActivityForResult(intent, CAMERA_REQUEST);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA_REQUEST) {
			try {
				if (resultCode == RESULT_OK) {
					String dstFile = Util.getSharedFile(this, UUID.randomUUID() + ".jpeg").getAbsolutePath();
					Image.formatPhoto(dstFile, _photoPath.getAbsolutePath(), new TrackPreferences(this).getImageShortPixel(), getContentResolver(), Calendar.getInstance().getTimeInMillis());
					PhotoRepository.insertPhoto(this, dstFile);

					finishActivity(Activity.RESULT_OK);
				}
			} catch (Exception e) {
				Log.e("ndb", "photo issue", e);
			}
			finish();
		}
	}

}
