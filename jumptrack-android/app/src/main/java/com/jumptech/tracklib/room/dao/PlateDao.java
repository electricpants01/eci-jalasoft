package com.jumptech.tracklib.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Plate;
import java.util.List;

@Dao
public interface PlateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Plate item);

    @Update
    void update(Plate item);

    @Query("select t.rowid as plate_key, t._line_key as line_key, t._plate as plate, t._scanned as scanned from plate t join line l on t._line_key = l._key " +
            "where (:deliveryKey <> '' and l._delivery_key = :deliveryKey) or (:lineKey <> '' and l._key = :lineKey)" +
            " or (:scan <> '' and t._plate = :scan) or (:scanned <> '' and _scanned = :scanned) order by t._plate asc")
    List<com.jumptech.tracklib.data.Plate> getPlates(Long deliveryKey, Long lineKey, String scan, Boolean scanned);

    @Delete
    void delete(Plate item);

    @Query("UPDATE plate SET _scanned = :scanned WHERE rowid = :rowId")
    int updateScannedPlate(long rowId, boolean scanned);

    @Query("INSERT INTO plate (_line_key,_plate) VALUES (:keyLine,:plate)")
    void insertPlate(long keyLine, String plate);

    @Query("UPDATE " + TrackDB.TBL_PLATE + " " +
            "SET _scanned = 0 " +
            "WHERE _line_key in ( " +
            "SELECT l._key " +
            "FROM " + TrackDB.TBL_STOP + " p " +
            "JOIN " + TrackDB.TBL_DELIVERY + " d on p._key = d._stop_key " +
            "JOIN " + TrackDB.TBL_LINE + " l on d._key = l._delivery_key " +
            "WHERE p._key = :stopKey " +
            "AND p._signature_key ISNULL " +
            ") ")
    void resetPlate(long stopKey);

    @Query("DELETE FROM plate")
    void nukeTable();

    @Query("DELETE FROM plate WHERE _line_key NOT IN (SELECT _key FROM line)")
    void clearPending();

}
