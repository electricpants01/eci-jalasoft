package com.jumptech.tracklib.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Line;

import java.util.List;

@Dao
public interface LineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Line item);

    @Update
    void update(Line item);

    @Delete
    void delete(Line item);

    @Query("SELECT _key, _delivery_key, _qty_target, _name, _product_no, " +
            " _uom, _desc, _scan, _qty_accept, _partial_reason as partialReason, _scanning, _qty_loaded " +
            "FROM line WHERE (:deliveryKey <> '' and :deliveryKey = _delivery_key)" +
            " or (:lineKey <> '' and :lineKey = _key) or (:scan <> '' and :scan = _scan) order by _name, _key")
    List<com.jumptech.tracklib.data.Line> getLines(Long deliveryKey, Long lineKey, String scan);

    @Query("DELETE FROM line WHERE _key = :key")
    void deleteByKey(long key);

    @Query("DELETE FROM line WHERE _delivery_key = :deliveryKey")
    void deleteByDeliveryKey(long deliveryKey);

    @Query("SELECT _key, _delivery_key, _qty_target, _name, _product_no, " +
            " _uom, _desc, _scan, _qty_accept, _partial_reason as partialReason, _scanning, _qty_loaded " +
            " from line where :key = _key limit 1")
    com.jumptech.tracklib.data.Line getByID(long key);

    @Query("UPDATE line SET _qty_accept = CASE WHEN _scanning = 1 THEN _qty_accept + 1 ELSE 1 end , _scanning = 1 WHERE _key = :lineKey ")
    void updateScannedLine(long lineKey);

    @Query("UPDATE line SET _delivery_key = :deliveryKey,_qty_target =:qtyTarget ,_name = :name ,_product_no =:productNo," +
            "_uom=:uom,_desc=:desc,_qty_accept=:qtyAccept WHERE rowid = :key")
    void update(long key, Long deliveryKey, Long qtyTarget, String name, String productNo, String uom, String desc, Integer qtyAccept);

    @Query("INSERT INTO line (_key,_delivery_key,_qty_target,_name,_product_no,_uom,_desc,_qty_accept) VALUES (:key, :deliveryKey, :qtyTarget , :name ,:productNo," +
            ":uom,:desc,:qtyAccept)")
    void insert(long key, Long deliveryKey, Long qtyTarget, String name, String productNo, String uom, String desc, Integer qtyAccept);

    @Query("INSERT INTO line (_delivery_key) VALUES (NULL)")
    long insertEmptyRow();

    @Query("UPDATE line set _qty_accept=:acceptQty, _partial_reason=:partialReason WHERE _key =:lineKey")
    int updateQtyAndPartial(long lineKey, int acceptQty, String partialReason);

    @Query("SELECT _key FROM line WHERE _key = :id")
    Long getFromId(long id);

    @Query("UPDATE " + TrackDB.TBL_LINE + " " +
            "SET _qty_accept = CASE WHEN (SELECT COUNT(*) FROM plate WHERE _line_key = _key) > 0 THEN 0 ELSE _qty_target END" +
            ", _partial_reason = null " +
            ", _scanning = 0 " +
            "WHERE _delivery_key IN ( " +
            "SELECT d._key " +
            "FROM " + TrackDB.TBL_STOP + " p " +
            "JOIN " + TrackDB.TBL_DELIVERY + " d on p._key = d._stop_key " +
            "WHERE p._key = :stopKey " +
            "AND p._signature_key ISNULL " +
            ") ")
    void resetLine(long stopKey);

    @Query("DELETE FROM line")
    void nukeTable();

    @Query("DELETE FROM line WHERE _delivery_key NOT IN (SELECT _key FROM delivery)")
    void clearPending();
}
