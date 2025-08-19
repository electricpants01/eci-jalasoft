package com.jumptech.tracklib.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jumptech.android.util.Util;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.TrackDB;

import java.util.List;

public class StopRepository {

    public static int storeStops(Context context, JsonElement stopsJson) {
        int sort = 0;
        if (stopsJson == null) return 0;
        JsonArray stops = stopsJson.getAsJsonArray();
        for (JsonElement stop : stops) {
            storeStop(context, stop.getAsJsonObject(), ++sort);
        }
        return stops.size();
    }

    private static void storeStop(Context context, JsonObject stop, int sort) {
        JsonObject site = stop.get("site").getAsJsonObject();
        int keySite = site.get("key").getAsInt();
        SiteRepository.storeSite(context, site);

        DeliveryRepository.storeDelivery(context, stop.get("deliverys").getAsJsonArray(), stop.get("key").getAsInt());
        com.jumptech.tracklib.room.entity.Stop stopEntity = new com.jumptech.tracklib.room.entity.Stop();
        ContentValues cv = UtilRepository.getPrimitiveAsContent(stop);
        stopEntity.setSiteKey((long) keySite);
        stopEntity.setSort(sort);
        WindowRepository.processStopWindow(context, cv, stop);
        for (String key : cv.keySet()) {
            switch (key) {
                case "_key":
                    stopEntity.setKey(cv.getAsInteger(key));
                    break;
                case "_planned":
                    stopEntity.setPlanned(cv.getAsBoolean(key));
                    break;
                case "_base_delivery_key":
                    stopEntity.setBaseDeliveryKey(cv.getAsLong(key));
                    break;
                case "_signature_key":
                    stopEntity.setSignatureKey(cv.getAsLong(key));
                    break;
                case "_window_id":
                    stopEntity.setWindowId(cv.getAsInteger(key));
                    break;
            }
        }
        TrackDB.getInstance(context).getStopDao().insert(stopEntity);
    }

    public static Cursor inTransitStops(Context context) {
        return TrackDB.getInstance(context).getStopDao().inTransitionStop();
    }

    public static long insertEmptyRow(Context context) {
        return TrackDB.getInstance(context).getStopDao().insertEmptyRow();
    }

    public static long addStop(Context context, int siteKey, Long signatureKey, Long baseDeliveryKey) {
        long rowid = StopRepository.insertEmptyRow(context);
        long stopKey = -rowid;
        TrackDB.getInstance(context).getStopDao().deleteByRowID(rowid);
        com.jumptech.tracklib.room.entity.Stop stop = TrackDB.getInstance(context).getStopDao().getFromId(stopKey);
        if (stop != null) {
            stop.setSiteKey((long) siteKey);
            stop.setBaseDeliveryKey(baseDeliveryKey);
            stop.setSignatureKey(signatureKey);
            TrackDB.getInstance(context).getStopDao().update(stop);
        } else {
            TrackDB.getInstance(context).getStopDao().insertStop(stopKey, siteKey, baseDeliveryKey, signatureKey);
        }
        return stopKey;
    }

    public static Cursor stopQuery(Context context, Boolean finished, Long stopKey, Boolean uploaded) {

        String queryString = "select p._key _id, p._site_key, s._name, s._address, count(*) delivery_count, p._base_delivery_key, p._signature_key, p._planned, p._sort " +
                ", p._window_id " +
                "from " + TrackDB.TBL_STOP + " p " +
                "join " + TrackDB.TBL_SITE + " s on p._site_key = s._key " +
                "join " + TrackDB.TBL_DELIVERY + " d on p._key = d._stop_key " +
                (uploaded != null
                        ? "join " + TrackDB.TBL_SIGNATURE + " g on p._signature_key = g._id and g._uploaded = ? "
                        : ""
                ) +
                "where 1=1 " +
                (finished != null ? ("and p._signature_key " + (finished ? "notnull" : "isnull") + " ") : "") +
                (stopKey != null ? "and p._key = ? " : "") +
                "group by p._key " +
                "order by p._signature_key, p._sort ";

        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, UtilRepository.filterNull(UtilRepository.dbBoolean(uploaded), stopKey));
        return TrackDB.getInstance(context).getGenericDao().genericQuery(query);
    }

    public static boolean stopHasChange(Context context, long stopKey) {
        String queryString = "select 1 " +
                "from " + TrackDB.TBL_STOP + " p " +
                "join " + TrackDB.TBL_DELIVERY + " d on p._key = d._stop_key " +
                "join " + TrackDB.TBL_LINE + " l on d._key = l._delivery_key " +
                "where p._key = ? " +
                "and p._signature_key isnull " +
                "and (l._scanning = 1 or l._qty_accept != CASE WHEN (SELECT COUNT(*) FROM plate WHERE _line_key = l._key) > 0 THEN 0 ELSE l._qty_target END) " +
                "union all select 1 from " + TrackDB.TBL_PHOTO + " where _signature_id = ? " +
                "union all select 1 from " + TrackDB.TBL_SIGNATURE + " where _id = ? ";
        //, new String[]{"" + stopKey, "" + Signature.NEW, "" + Signature.NEW}
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, UtilRepository.filterNull(stopKey, Signature.NEW, Signature.NEW));
        Cursor cursor = TrackDB.getInstance(context).getGenericDao().genericQuery(query);
        boolean result = cursor.moveToFirst();
        Util.close(cursor);
        return result;
    }

    public static Cursor completedStops(Context context) {
        String queryString = "select p._key _id, p._site_key, s._name, s._address, count(*) delivery_count, p._base_delivery_key, p._signature_key, p._planned, p._sort, p._window_id " +
                "from " + TrackDB.TBL_STOP + " p " +
                "join " + TrackDB.TBL_SITE + " s on p._site_key = s._key " +
                "join " + TrackDB.TBL_DELIVERY + " d on p._key = d._stop_key " +
                "join " + TrackDB.TBL_SIGNATURE + " g on p._signature_key = g._id and g._uploaded = 1 " +
                "where p._signature_key notnull " +
                "group by p._key " +
                "order by p._signature_key, p._sort ";
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, null);
        return TrackDB.getInstance(context).getGenericDao().genericQuery(query);
    }

    public static Cursor notSyncedStops(Context context) {
        String queryString = "select p._key _id, p._site_key, s._name, s._address, count(*) delivery_count, p._base_delivery_key, p._signature_key, p._planned, p._sort, p._window_id " +
                "from " + TrackDB.TBL_STOP + " p " +
                "join " + TrackDB.TBL_SITE + " s on p._site_key = s._key " +
                "join " + TrackDB.TBL_DELIVERY + " d on p._key = d._stop_key " +
                "join " + TrackDB.TBL_SIGNATURE + " g on p._signature_key = g._id and g._uploaded = 0 " +
                "where p._signature_key notnull " +
                "group by p._key " +
                "order by p._signature_key, p._sort ";
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, null);
        return TrackDB.getInstance(context).getGenericDao().genericQuery(query);
    }

    public static Stop fetchStop(Context context, long stopKey) {
        Cursor cursor = stopQuery(context, null, stopKey, null);
        Stop stop = cursor.moveToFirst() ? stop(cursor) : null;
        return (stop != null && stop.getWindow_id() != null) ? obtainWindowTime(context, stop) : stop;
    }

    public static Stop obtainWindowTime(Context context, Stop stop) {
        stop.setWindowDisplay(WindowRepository.getDisplayFromWindowId(context, stop.getWindow_id()));
        stop.setWindowTimeList(WindowRepository.getWindowTimeList(context, stop.getWindow_id()));
        return stop;
    }

    public static void routeOrder(Context context, List<Stop> stops) {
        for (int i = 0; i < stops.size(); ++i) {
            TrackDB.getInstance(context).getStopDao().updateSort(stops.get(i).key, i);
        }
        RouteRepository.updateRouteOrderUpload(context, false);
    }

    public static Stop stop(Cursor cursor) {
        Stop stop = new Stop();
        int c = -1;
        stop.key = cursor.getInt(++c);
        stop.site_key = cursor.getLong(++c);
        stop.name = cursor.getString(++c);
        stop.address = cursor.getString(++c);
        stop.delivery_count = cursor.getInt(++c);
        stop.baseDeliveryKey = (cursor.isNull(++c) ? null : cursor.getLong(c));
        stop.signature_key = (cursor.isNull(++c) ? null : cursor.getLong(c));
        stop.setPlanned(cursor.isNull(++c) ? false : Boolean.valueOf(cursor.getString(c)));
        stop.setSort(cursor.isNull(++c) ? null : cursor.getInt(c));
        stop.setWindow_id(cursor.isNull(++c) ? null : cursor.getInt(c));
        return stop;
    }

    public static void removeChildlessStop(Context context) {
        TrackDB.getInstance(context).getStopDao().removeChildless();
    }

}
