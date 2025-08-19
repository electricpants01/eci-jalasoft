package com.jumptech.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.view.View.OnClickListener;

import com.jumptech.jumppod.R;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.PhotoRepository;
import com.jumptech.tracklib.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

public class SignatureTool {
	protected static final int SIGNATURE_PHOTO_REQUEST = 1337;
	
	public static void createCameraButton(final Activity activity, View button){
		final int imageMax = new TrackPreferences(activity).getImageMax(); 
		if(imageMax == 0) button.setVisibility(View.GONE);
		else button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
					if(imageMax == PhotoRepository.photoNewCount(activity)) {
						new AlertDialog.Builder(activity)
						.setCancelable(false)
						.setPositiveButton(android.R.string.ok, null)
						.setMessage(activity.getString(R.string.maxPhotosReached, imageMax))
						.create()
						.show();
					} else {
						if (PermissionUtil.hasPermission(activity, Manifest.permission.CAMERA)
								&& PermissionUtil.hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
							startSignaturePhoto(activity);
						} else {
							List<String> permissions = new ArrayList<String>();
							if (!PermissionUtil.hasPermission(activity, Manifest.permission.CAMERA)) permissions.add(Manifest.permission.CAMERA);
							if (!PermissionUtil.hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
							PermissionUtil.requestPermission(activity, permissions, PermissionUtil.REQUEST_CAMERA);
						}
					}
			}
		});
	}

	public static void startSignaturePhoto(Activity activity) {
		activity.startActivityForResult(new Intent(activity, SignaturePhotoActivity.class), SIGNATURE_PHOTO_REQUEST);
	}

}
