package com.jumptech.jumppod.model;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.provider.MediaStore;
import android.util.Log;

import com.jumptech.android.util.Util;
import com.jumptech.android.util.UtilImage;
import com.jumptech.tracklib.comms.TrackException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Image {
	
	private static final String TAG = Image.class.getName();

	/**
	 * Default constructor
	 */
	private Image() {}

	// * Method for converting cropped signature to a 2:1 aspect ratio scaled image
	public static String formatPNG(Bitmap src, String saveToFile, int desiredWidth) {
		Log.i(TAG, "formatPNG(" + saveToFile + ", " + desiredWidth);
		int height = desiredWidth / 2;
		Bitmap finalBmp = null;
		{
			//Bitmap src = UtilImage.decodeFileToTarget(filepath, height, desiredWidth);

			//Log.i(TAG, "transform image from " + src.getWidth() + "x" + src.getHeight() + "px");
			if(src == null || isBlank(src)) {
				return null;
			}
			
			Bitmap tmp = null;
			//when source bitmap is smaller than target
			if(src.getHeight() < height && src.getWidth() < desiredWidth) {
				tmp = Bitmap.createBitmap(desiredWidth, height, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(tmp);
				canvas.drawColor(Color.WHITE);
				Paint alphaPaint = new Paint();
				alphaPaint.setAlpha(255);
				canvas.drawBitmap(src, desiredWidth / 2 - src.getWidth() / 2, height / 2 - src.getHeight() / 2, alphaPaint);
			}
			//when signature is very skinny
			else if(src.getHeight() > src.getWidth() / 2) {
				tmp = Bitmap.createBitmap(src.getHeight() * 2, src.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(tmp);
				canvas.drawColor(Color.WHITE);
				Paint alphaPaint = new Paint();
				alphaPaint.setAlpha(255);
				canvas.drawBitmap(src, src.getHeight() - src.getWidth() / 2, 0, alphaPaint);

			}
			//else signature is wide
			else {
				tmp = Bitmap.createBitmap(src.getWidth(), src.getWidth() / 2, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(tmp);
				canvas.drawColor(Color.WHITE);
				Paint alphaPaint = new Paint();
				alphaPaint.setAlpha(255);
				canvas.drawBitmap(src, 0, src.getWidth() / 4 - src.getHeight() / 2, alphaPaint);

			}
			src.recycle();
			finalBmp = Bitmap.createScaledBitmap(tmp, desiredWidth, height, true);
			tmp.recycle();
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(saveToFile);
			finalBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
			finalBmp.recycle();
			return saveToFile;
		}
		catch(Exception e) {
			Log.e(TAG, "unable to write file", e);
			return null;
		}
		finally {
			Util.close(fos);
		}

	}

    public static Bitmap getScaledBitmapAR(Bitmap inBmp, int width, BitmapFactory.Options options) {
        int origWidth = inBmp.getWidth(); //480
        int origHeight = inBmp.getHeight(); //800
        int newWidth;
        int newHeight;
        float scaleWidth = width;
        float scaleHeight = width;
        float scaleFactor;
        if (origWidth <= origHeight) {
            scaleWidth = (float) origWidth / width; //wdt 480/200 = 2.4 - case 1
            scaleFactor = scaleWidth;
        } else {
            scaleHeight = (float) origHeight / width; // ht 480/200 = 2.4 - case 2
            scaleFactor = scaleHeight;
        }
        options.outWidth = newWidth = Math.round(origWidth / scaleFactor); // case 1/2 : 200 width - 333 ht .
        options.outHeight = newHeight = Math.round(origHeight / scaleFactor);
        return Bitmap.createScaledBitmap(inBmp, newWidth, newHeight, true);
    }

	private static boolean isBlank(Bitmap b){
		for(int i = 0; i < b.getWidth(); i ++){
			for(int j = 0; j < b.getHeight(); j++){
				if(b.getPixel(i, j) != Color.TRANSPARENT){
					Log.v("Image", "Found begginning of image at " + i + "," + j);
					return false;
				}
			}
		}
		return true;
	}

	public static void formatPhoto(String destFile, String srcFile, int shortSidePixel, ContentResolver content, long currentTime) throws FileNotFoundException, IOException {
		int rotation = getAngleRotation(srcFile, content, currentTime);
		final BitmapFactory.Options options = new BitmapFactory.Options();
		final BitmapFactory.Options newOptions = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(srcFile, options);
		float distribution = 1;
		float otherSide;

		if (options.outWidth > options.outHeight) {
			distribution = (float) options.outWidth / (float) options.outHeight;
			otherSide = distribution * (float) shortSidePixel;
			options.inSampleSize = calculateInSampleSize(options, (int) otherSide, shortSidePixel);
		} else {
			distribution = (float) options.outHeight / (float) options.outWidth;
			otherSide = distribution * (float) shortSidePixel;
			options.inSampleSize = calculateInSampleSize(options, shortSidePixel, (int) otherSide);
		}

		options.inJustDecodeBounds = false;
		Log.i(TAG, "Photo Dimensions: " + options.outWidth + "x" + options.outHeight + " , inSampleSize: " + options.inSampleSize);
		Bitmap bitmapDecode = BitmapFactory.decodeFile(srcFile, options);
		Log.i(TAG, "Photo Dimensions new Sample: " + bitmapDecode.getWidth() + "x" + bitmapDecode.getHeight());
		Bitmap bitmapRotatedAndScalated = rotateBitmap(getScaledBitmapAR(bitmapDecode, shortSidePixel, newOptions), rotation, newOptions);
		Log.i(TAG, "Photo Dimensions Scaled and rotated: " + bitmapDecode.getWidth() + "x" + bitmapDecode.getHeight());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(destFile));
			bitmapRotatedAndScalated.compress(Bitmap.CompressFormat.JPEG, 80, fos);
		} finally {
			if (fos != null) fos.close();
			new File(srcFile).delete();
		}
	}

	/**
	 * Calculates the sample size, this is the number of pixels in either dimension
	 * that correspond to a single pixel in the decoded bitmap
	 *
	 * @param options options for BitmapFactory
	 * @param reqWidth new width expected
	 * @param reqHeight new height expected
     * @return the sample size
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

	/**
	 * Calculates the angle that will rotate the image
	 *
	 * @param filePath file path of the image
	 * @param content the content model
	 * @param captureTime current time
     * @return the angle to rotate
     */
    public static int getAngleRotation(String filePath, ContentResolver content, long captureTime) {
        int rotation = -1;
        int orientation;
        long fileSize = new File(filePath).length();

        Cursor mediaCursor = content.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.ImageColumns.ORIENTATION, MediaStore.MediaColumns.SIZE}, MediaStore.MediaColumns.DATE_ADDED + ">=?", new String[]{String.valueOf(captureTime / 1000 - 1)}, MediaStore.MediaColumns.DATE_ADDED + " desc");

        if (mediaCursor != null && captureTime != 0 && mediaCursor.getCount() != 0) {
            while (mediaCursor.moveToNext()) {
                long size = mediaCursor.getLong(1);
                if (size == fileSize) {
                    rotation = mediaCursor.getInt(0);
                    break;
                }
            }
        } 
        if (rotation == -1) {
            ExifInterface exif;
            try {
                exif = new ExifInterface(filePath);
                orientation = Integer.valueOf(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotation = 270;
                        break;
                    default:
                        rotation = 0;
                }
            } catch (IOException e) {
                Log.e(TAG, TrackException.MSG_EXCEPTION, e);
                rotation = 0;
            }
        }
        return rotation;
    }

	/**
	 * Rotates a Bitmap
	 *
	 * @param bitmap bitmap to rotate
	 * @param rotationAngle angle of rotation
	 * @param options options of the bitmap
     * @return bitmap rotated
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int rotationAngle, BitmapFactory.Options options) {
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, options.outWidth, options.outHeight, matrix, true);
    }
}
