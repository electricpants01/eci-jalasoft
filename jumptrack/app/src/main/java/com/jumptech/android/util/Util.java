package com.jumptech.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextPaint;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.IntegerRes;
import androidx.core.content.ContextCompat;

import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Prompt;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Util {
	private static final String TAG = Util.class.getName();

	public static final String CALENDAR_STRING_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public enum MapsPackageNames{
		GoogleMaps, Waze
	}

	public static String googleMapsPackageName = "com.google.android.apps.maps";
	public static String wazePackageName = "com.waze";

    /**
     * Default constructor
     */
    private Util() {}

    public static final void close(Closeable... os) {
        for (Closeable o : os) {
            try {
                if (o != null) o.close();
            } catch (Exception e) {
                Log.e(TAG, TrackException.MSG_EXCEPTION, e);
            }
        }
    }

	public static final int hashCode(Object... objects) {
		int hashCode = 0;
		for(Object o : objects) {
			hashCode += (o == null ? 0 : o.hashCode());
		}
		return hashCode;
	}

	public static final boolean equals (Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if ((o1 == null) || (o2 == null)) {
			return false;
		}
		return o1.equals(o2);
	}

	public static void deleteFilesInDir(String path) {
		deleteFilesInDir(path, null);
	}

	public static void deleteFilesInDir(String path, List<String> filesToDelete) {
		Log.v(TAG, "delete directory: " + path);
		File fileDir = new File(path);
		if (fileDir.exists() && fileDir.isDirectory()) {
			File[] files = fileDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (!file.getAbsolutePath().contains("gaClientId")
							&& (filesToDelete != null && filesToDelete.contains(file.getAbsolutePath()))) {
						Log.i(TAG, "Deleting file: " + file.getAbsolutePath());
						file.delete();
					} else {
						Log.i(TAG, "Skipping file: " + file.getAbsolutePath());
					}
				}
			}
		}
	}

	public static void lockOrientation(Context context) {
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		else if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	public static String toString(Calendar c) {
		SimpleDateFormat sdf = new SimpleDateFormat(CALENDAR_STRING_FORMAT, Locale.US);
		sdf.setTimeZone(c.getTimeZone());
		return sdf.format(c.getTime());
	}

	/**
	 *
	 * @param datetimezone
	 * @param defaultTimeZone Provides default time zone for strings missing time zone encoding.
	 * @return
	 * @throws ParseException
	 */
	public static Calendar parseCalendar(final String datetimezone, final TimeZone defaultTimeZone) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(CALENDAR_STRING_FORMAT, Locale.US);
		Calendar c = Calendar.getInstance();
		if(datetimezone.length() >= 28) c.setTimeZone(TimeZone.getTimeZone("GMT" + datetimezone.substring(23, 28)));
		else if(defaultTimeZone != null) c.setTimeZone(defaultTimeZone);
		else throw new ParseException("missing timezone", 28);
		c.setTime(sdf.parse(datetimezone));
		return c;
	}

	public static boolean toBoolean(String str) {
		if (str != null) {
			str = str.toUpperCase(Locale.getDefault()).trim();
			return "Y".equals(str) || "YES".equals(str) || "TRUE".equals(str) || "1".equals(str);
		}
		return false;
	}

	public static String toString(Boolean b) {
		return b ? "1" : "0";
	}

	public static String toString(Object... os) {
		StringBuilder sb = new StringBuilder().append("[");
		for(Object o : os) {
			if(sb.length() > 1) sb.append(", ");
			sb.append("" + o);
		}
		sb.append("]");
		return sb.toString();
	}

	public static String getDeviceName() {
	  String man = Build.MANUFACTURER;
	  String model = Build.MODEL;
	  return model.startsWith(man) ? model : man + " " + model;
	}

	@SuppressLint({ "WorldReadableFiles", "WorldWriteableFiles" })
	public static File getSharedFile(Context context, String filename) throws IOException {
		context.openFileOutput(filename, Context.MODE_PRIVATE).close();
		return context.getFileStreamPath(filename);
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnected();
	}

    /**
     * This method encodes a text value to send through URL values
     *
     * @param value a text value to be encoded
     * @return an encoded text value
     */
    public static String EncodeValueWithURLEncoder(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "exception", e);
            return value;
        } catch (NullPointerException e) {
            Log.e(TAG, "exception", e);
            return value;
        }
    }

    /**
     * Reduces the text size according the max width
     *
     * @param paint TextPaint of the TextView
     * @param maxWidth max width
     * @param text text to display in TextView
     * @return new size in pixels
     */
    public static float getFitTextSize(TextPaint paint, float maxWidth, String text) {
        float nowWidth = paint.measureText(text);
        float factor = maxWidth / nowWidth;
        float newSize = paint.getTextSize();
        if (factor < 1) {
            newSize = maxWidth / nowWidth * newSize;
        }
        return newSize;
    }

    public static String formatTime(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return format.format(newDate);
        } catch (ParseException e) {
           Log.e(TAG, "formatTime", e);
        }
        return null;
    }

    public static void changeTextViewColor(TextView textView, @ColorRes int color) {
    	textView.setTextColor(ContextCompat.getColor(textView.getContext(), color));
	}

	public static void changeTextViewColorDependingOnOrder(TextView textView, boolean ordered) {
		if (ordered) {
			Util.changeTextViewColor(textView, Prompt.Style.NORMAL.getColor());
		} else {
			Util.changeTextViewColor(textView, Prompt.Style.ERROR.getColor());
		}
	}

	public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
		try {
			packageManager.getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

}
