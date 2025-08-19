package com.jumptech.tracklib.repository;

import android.content.ContentValues;
import android.content.Context;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.TrackDB;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UtilRepository {

    private static final String DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    static String[] filterNull(Object... values) {
        List<String> filtered = new ArrayList<>();
        for (Object value : values) if (value != null) filtered.add("" + value);
        return filtered.toArray(new String[filtered.size()]);
    }

    static Integer dbBoolean(Boolean b) {
        if (b == null) return null;
        return b ? 1 : 0;
    }

    public static void clearAllExceptRoute(Context context) {
        TrackDB.getInstance(context).getDeliveryDao().nukeTable();
        TrackDB.getInstance(context).getLineDao().nukeTable();
        TrackDB.getInstance(context).getPhotoDao().nukeTable();
        TrackDB.getInstance(context).getPlateDao().nukeTable();
        TrackDB.getInstance(context).getPromptDao().nukeTable();
        TrackDB.getInstance(context).getSignatureDao().nukeTable();
        TrackDB.getInstance(context).getSiteDao().nukeTable();
        TrackDB.getInstance(context).getStopDao().nukeTable();
        TrackDB.getInstance(context).getWindowDao().nukeTable();
        TrackDB.getInstance(context).getWindowTimeDao().nukeTable();
    }

    public static String[] splitNotNull(String s, String sep) {
        if (s == null) return new String[]{};
        return s.split(sep);
    }

    public static ContentValues getPrimitiveAsContent(JsonObject obj) {
        ContentValues cv = new ContentValues();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                cv.put("_" + entry.getKey().replaceAll("-", "_"), entry.getValue().getAsString());
            }
        }
        return cv;
    }

    public static String format(Date date) {
        return new SimpleDateFormat(DATETIME, Locale.US).format(date);
    }

    public static Date parseDate(String date) throws ParseException {
        if (date == null) return null;
        return new SimpleDateFormat(DATETIME, Locale.US).parse(date);
    }
}
