package com.jumptech.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.jumptech.tracklib.comms.TrackException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class UtilImage {

	/**
	 * Log tag
	 */
	private static final String TAG = UtilImage.class.getName();

	/**
	 * Default constructor
	 */
	private UtilImage() {}

	/**
	 * Overwrites the image at the specified path with one of the target size. Full image at original scaling is used; filler bars of specified color fill gaps equally on sides. 
	 * 
	 * @param imagePath Existing absolute file path
	 * @param targetWidth px
	 * @param targetHeight px
	 * @param fillColor Color.*
	 * @throws FileNotFoundException
	 */
	public static void scalePhoto(String imagePath, int targetWidth, int targetHeight, int fillColor) throws FileNotFoundException {
		//grab current image size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, options);
		
		Log.i(TAG, "camera raw " + options.outWidth + " x " + options.outHeight);
		
		//if there is nothing to do
		if(options.outWidth == targetWidth && options.outHeight == targetHeight) return;
		
		//load bitmap
		Bitmap bitmap = null;
		{
			BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
			scaleOptions.inJustDecodeBounds = false;
			scaleOptions.inPurgeable = false;
			
			int scaleFactor = Math.max(options.outWidth/targetWidth, options.outHeight/targetHeight);
			scaleFactor /= 2; //scale to twice what we really need to improve quality
			//if we need to scale down before resize
			if(scaleFactor > 1){
				Log.i(TAG, "need to scale at " + scaleFactor);
				scaleOptions.inSampleSize = scaleFactor;
			}
			Log.i(TAG, "image exists " + new File(imagePath).exists());
			bitmap = BitmapFactory.decodeFile(imagePath, scaleOptions);
		}
		
		Log.i(TAG, "postload raw " + bitmap.getWidth() + " x " + bitmap.getHeight());
		
		//if we need to resize after loaded scale
		if(bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight) {
			//scale down to meet target in both directions
			int viewWidth = targetWidth;
			int viewHeight = targetHeight;
			{
				float scaleWidth = bitmap.getWidth() / targetWidth;
				float scaleHeight = bitmap.getHeight() / targetHeight;
				//if our limit factor is width
				if(scaleWidth > scaleHeight) viewHeight = Math.round(bitmap.getHeight() / scaleWidth);
				else viewWidth = Math.round(bitmap.getWidth() / scaleHeight);
				
				bitmap = Bitmap.createScaledBitmap(bitmap, viewWidth, viewHeight, false);
			}
				
			//if we need to center scaled result to match size
			if(bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight) {
				Bitmap bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565);

				Canvas canvas = new Canvas(bmp);
				canvas.drawColor(fillColor);
				Paint alphaPaint = new Paint();
				alphaPaint.setAlpha(255);
				canvas.drawBitmap(bitmap,
						(targetWidth - viewWidth )/2,
						(targetHeight - viewHeight)/2,
						alphaPaint);
				bitmap = bmp;
			}
		}
		
		FileOutputStream fos = null;
		try {
			File f = new File(imagePath);
			f.delete();
			fos = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
		}
		finally {
			Util.close(fos);
		}
	}
	
	/**
	 * 
	 * @param path
	 * @param height
	 * @param width
	 * @return scaled bitmap or null if couldn't be loaded
	 */
	public static Bitmap decodeFileToTarget(String path, int height, int width) {
		try {
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(path, options);
	    options.inSampleSize = Math.min(options.outHeight/height, options.outWidth/width);
	    options.inJustDecodeBounds = false;
	    Bitmap bm = BitmapFactory.decodeFile(path, options);
			return bm;
		}
		catch(OutOfMemoryError oom) {
			Log.e(TAG, TrackException.MSG_EXCEPTION, oom);
			return null;
		}
	}
}
