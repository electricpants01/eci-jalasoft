package com.jumptech.tracklib.db;

import android.content.ContentValues;

import com.jumptech.android.util.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContentBuilder {
	private ContentValues _cv = new ContentValues();
	
	public ContentBuilder put(String key, boolean b) {
		_cv.put(key, b ? 1 : 0);
		return this;
	}
	
	public ContentBuilder put(String key, Date date) {
		_cv.put(key, new SimpleDateFormat(Util.CALENDAR_STRING_FORMAT, Locale.US).format(date));
		return this;
	}
	
	public ContentBuilder put(String key, Integer value) {
		_cv.put(key, value);
		return this;
	}
	
	public ContentBuilder put(String key, Long value) {
		_cv.put(key, value);
		return this;
	}

	public ContentBuilder put(String key, String value) {
		_cv.put(key, value);
		return this;
	}

	public ContentValues values() {
		return _cv;
	}


}
