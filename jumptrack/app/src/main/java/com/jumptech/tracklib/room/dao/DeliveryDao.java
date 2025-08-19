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
import com.jumptech.tracklib.room.entity.Delivery;

import java.util.List;

@Dao
public interface DeliveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Delivery item);

    @Update
    void update(Delivery item);

    @Delete
    void delete(Delivery item);

    @Query("SELECT * FROM delivery WHERE _key=:key ")
    Delivery getByID(long key);

    @RawQuery
    Cursor fetch(SupportSQLiteQuery query);

    @Query("UPDATE delivery SET _signing = :signing WHERE _stop_key = :stopKey")
    void signatureStop(long stopKey, int signing);

    @Query("UPDATE delivery SET _signing = :signing WHERE _key = :deliveryKey")
    void signatureDelivery(long deliveryKey, int signing);

    @Query("UPDATE delivery SET _signing = 0 WHERE _signing = 1")
    void signatureDeliveryClear();

    @Query("UPDATE delivery SET _stop_key = :stopKey, _signing = 0 WHERE _signing = 1")
    void updateStopAndSigning(long stopKey);

    @Query("INSERT INTO delivery (_site_key) VALUES (NULL)")
    long insertEmptyRow();

    @Query("INSERT INTO delivery (_key , _stop_key, _site_key, _type , _delivery_cd) VALUES (:deliveryId ,:stopKey, :siteKey, :type, :deliveryCd )")
    void insertDelivery(long deliveryId, long stopKey, int siteKey, String deliveryCd, String type);

    @Query("UPDATE delivery SET _site_key = :siteKey, _stop_key = :stopKey, _type = :type, _delivery_cd = :deliveryCd WHERE _key = :deliveryId")
    void updateDelivery(long deliveryId, long stopKey, int siteKey, String deliveryCd, String type);

    @Query("SELECT d._key FROM stop p JOIN delivery d ON p._key = d._stop_key WHERE p._signature_key NOTNULL AND d._key > 0 ORDER BY p._signature_key DESC, d._key DESC ")
    Long getLastScheduledDelivery();

    @Query("DELETE FROM delivery WHERE _key = :key")
    void deleteByKey(long key);

    @Query("SELECT * FROM delivery WHERE _key = :id")
    Delivery getFromId(long id);

    @Query("DELETE FROM delivery")
    void nukeTable();

    @Query("DELETE FROM delivery WHERE _stop_key NOT IN (SELECT _key FROM stop)")
    void clearPending();

    @Query("select distinct d._key as id, d._stop_key as stop_key," +
            " s._name as name, s._address as address, d._delivery_cd as display, d._delivery_note as note," +
            " d._type as db_type, sum(case when l._key is not null then 1 else 0 end) as line_count "
            + "from " + TrackDB.TBL_DELIVERY + " d "
            + "join " + TrackDB.TBL_SITE + " s on d._site_key = s._key "
            + "left outer join " + TrackDB.TBL_LINE + " l on d._key = l._delivery_key "
            + "where (:stopKey <> '' and d._stop_key = :stopKey) "
            + "or (:deliveryKey <> '' and d._key = :deliveryKey) "
            + "or (:deliveryCode <> '' and d._delivery_cd = :deliveryCode) "
            + "group by d._key ")
    List<com.jumptech.tracklib.data.Delivery> fetchDelivery(Long stopKey, Long deliveryKey, String deliveryCode);

    @Query("select s._name " +
            "from " + TrackDB.TBL_DELIVERY + " d " +
            "join " + TrackDB.TBL_SITE + " s on d._site_key = s._key " +
            "where d._signing = 1 ")
    String getSigningName();

}
