package com.jumptech.tracklib.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jumptech.tracklib.data.WindowTime;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Window;

import java.util.ArrayList;
import java.util.List;

public class WindowRepository {


    public static String getDisplayFromWindowId(Context context, Integer windowId) {
        Cursor cursor = TrackDB.getInstance(context).getWindowDao().getDisplay(windowId);
        if (cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        return null;
    }

    public static List<WindowTime> getWindowTimeList(Context context, long windowId) {
        String queryString = "SELECT wt._startSec, wt._endSec FROM window w JOIN windowtime wt WHERE w._id = '" + windowId + "' AND wt._window_id=w._id ";
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, null);
        Cursor cursor = TrackDB.getInstance(context).getGenericDao().genericQuery(query);
        List<WindowTime> results = new ArrayList<>();
        while (cursor.moveToNext()) {
            results.add(windowTime(cursor));
        }
        return results;
    }

    private static WindowTime windowTime(Cursor cursor) {
        WindowTime windowTime = new WindowTime();
        int c = -1;
        windowTime.setStartSec(cursor.getLong(++c));
        windowTime.setEndSec(cursor.getLong(++c));
        return windowTime;
    }

    /**
     * Processes the available Stop Window information and stores in local DDBB
     *
     * @param cv   the Stop content values to be updated before store it
     * @param stop the entire Stop source data
     */
    public static void processStopWindow(Context context, ContentValues cv, JsonObject stop) {
        JsonElement window = stop.get("window");
        if (window != null) {
            Long windowId = WindowRepository.storeWindow(context, window.getAsJsonObject());
            cv.put("_window_id", windowId);
        }
    }

    /**
     * Stores the Stop Window and its window-times
     *
     * @param window the Stop's Window source data
     */
    public static long storeWindow(Context context, JsonObject window) {
        ContentValues cv = UtilRepository.getPrimitiveAsContent(window);
        Window windowEntity = new Window();
        for (String key : cv.keySet()) {
            switch (key) {
                case "_id":
                    windowEntity.setId(cv.getAsInteger(key));
                    break;
                case "_display":
                    windowEntity.setDisplay(cv.getAsString(key));
                    break;
            }
        }
        long windowId = TrackDB.getInstance(context).getWindowDao().insert(windowEntity);
        JsonArray windowTimes = window.get("times").getAsJsonArray();
        com.jumptech.tracklib.room.entity.WindowTime windowTimeEntity;
        for (JsonElement windowTime : windowTimes) {
            ContentValues cvTimes = UtilRepository.getPrimitiveAsContent(windowTime.getAsJsonObject());
            cvTimes.put("_window_id", windowId);
            windowTimeEntity = new com.jumptech.tracklib.room.entity.WindowTime();
            windowTimeEntity.setWindowId((int) windowId);
            for (String key : cvTimes.keySet()) {
                switch (key) {
                    case "_id":
                        windowTimeEntity.setId(cvTimes.getAsInteger(key));
                        break;
                    case "_startSec":
                        windowTimeEntity.setStartSec(cvTimes.getAsLong(key));
                        break;
                    case "_endSec":
                        windowTimeEntity.setEndSec(cvTimes.getAsLong(key));
                        break;
                }
            }
            TrackDB.getInstance(context).getWindowTimeDao().insert(windowTimeEntity);
        }
        return windowId;
    }
}
