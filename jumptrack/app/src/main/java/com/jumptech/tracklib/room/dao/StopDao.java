package com.jumptech.tracklib.room.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Stop;

@Dao
public interface StopDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Stop item);

    @Update
    void update(Stop item);

    @Delete
    void delete(Stop item);

    @Query("SELECT p._key _id, p._site_key, s._name, s._address, count(*) delivery_count, p._base_delivery_key, p._signature_key, p._planned, p._sort, p._window_id " +
            "from " + TrackDB.TBL_STOP + " p " +
            "JOIN " + TrackDB.TBL_SITE + " s ON p._site_key = s._key " +
            "JOIN " + TrackDB.TBL_DELIVERY + " d ON p._key = d._stop_key " +
            "JOIN " + TrackDB.TBL_SIGNATURE + " g ON p._signature_key = g._id AND g._uploaded = 0 " +
            "WHERE p._signature_key NOTNULL " +
            "GROUP BY p._key " +
            "ORDER BY p._signature_key, p._sort ")
    Cursor getNonSyncedStops();

    @Query("SELECT p._key _id, p._site_key, s._name, s._address, count(*) delivery_count, p._base_delivery_key, p._signature_key, p._planned, p._sort, p._window_id " +
            "FROM " + TrackDB.TBL_STOP + " p " +
            "JOIN " + TrackDB.TBL_SITE + " s ON p._site_key = s._key " +
            "JOIN " + TrackDB.TBL_DELIVERY + " d ON p._key = d._stop_key " +
            "WHERE p._signature_key ISNULL " +
            "GROUP BY p._key " +
            "ORDER BY p._signature_key, p._sort ")
    Cursor inTransitionStop();

    @Query("DELETE FROM stop WHERE _signature_key IS NOT NULL ")
    int deleteSignaturesAll();

    @Query("DELETE FROM stop WHERE _signature_key ISNULL")
    int deleteSignaturesNull();

    @Query("DELETE FROM stop WHERE _signature_key ISNULL and _key = :stopKey")
    int deleteSignaturesNullFromStopKey(long stopKey);

    @Query("INSERT INTO stop (_window_id) VALUES (NULL)")
    long insertEmptyRow();

    @Query("UPDATE stop SET _site_key = :siteKey, _planned = 0, _sort = :stopKey, _base_delivery_key = :baseDeliveryKey, _signature_key = :signatureKey WHERE _key = :stopKey")
    void updateStop(long stopKey, Integer siteKey, Long baseDeliveryKey, Long signatureKey);

    @Query("INSERT INTO stop  (_key , _site_key , _planned , _sort , _base_delivery_key , _signature_key) VALUES (:stopKey, :siteKey,0, :stopKey, :baseDeliveryKey,:signatureKey )")
    void insertStop(long stopKey, Integer siteKey, Long baseDeliveryKey, Long signatureKey);

    @Query("UPDATE stop SET _sort = :position WHERE _key = :key")
    void updateSort(long key, int position);

    @Query("DELETE FROM stop WHERE rowid = :stopKey")
    void deleteByRowID(long stopKey);

    @Query("SELECT * FROM stop WHERE _key = :id")
    Stop getFromId(long id);

    @Query("DELETE " +
            "FROM " + TrackDB.TBL_STOP + " " +
            "WHERE _key NOT IN (" +
            "SELECT d._stop_key " +
            "FROM " + TrackDB.TBL_DELIVERY + " d " +
            "GROUP BY d._stop_key " +
            ") ")
    void removeChildless();

    @Query("DELETE FROM stop")
    void nukeTable();

}
