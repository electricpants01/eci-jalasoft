package com.jumptech.ui;

public class BundleTrack {
	private final static String HEADER = "com.jumptech.tracklib.";
	
	public static final String KEY_STOP = HEADER + "keyStop";
	public static final String KEY_SIGNATURE = HEADER + "keySignature";
	public static final String KEY_DELIVERY = HEADER + "keyDelivery";
	public static final String KEY_LINE = HEADER + "keyLine";
	public static final String KEY_NEW_UNSCHEDULED = HEADER + "newUnscheduled";

	public static final String GPS_ENABLED = HEADER + "gpsEnabled";
	public static final String GPS_MIN_DISTANCE = HEADER + "gpsMinDistance";
	public static final String GPS_MIN_TIME = HEADER + "gpsMinTime";

	public static final String BUNDLE_INTENT_FROM_ROUTE = "intentBundleFromRoute";
}
