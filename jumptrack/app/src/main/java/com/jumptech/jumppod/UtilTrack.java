package com.jumptech.jumppod;

import android.annotation.SuppressLint;

import com.jumptech.android.util.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UtilTrack {
	static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Default constructor
     */
    private UtilTrack() {}

	@SuppressLint("SimpleDateFormat")
	public static Calendar parseTrackDate(String datetimezone) throws ParseException {
		if(datetimezone == null) return null;
		Calendar c = null;
		//Track 1.4.1 and lower
		if(datetimezone.length() <= 20) {
			c = Calendar.getInstance();
			c.setTime(new SimpleDateFormat(DATETIME, Locale.US).parse(datetimezone));
		}
		else c = Util.parseCalendar(datetimezone, TimeZone.getDefault());

		return c;
	}
	
	public static String formatServer(Date date) {
		return new SimpleDateFormat(DATETIME, Locale.US).format(date);
	}

	@SuppressLint("SimpleDateFormat")
	public static String format(Date date) {
		return new SimpleDateFormat("MM/dd/yyyy h:mm:ss a").format(date);
	}
	
}
