package com.jumptech.tracklib.room.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Photo;

@Dao
public interface PhotoDao {

    @Insert
    void insert(Photo item);

    @Update
    void update(Photo item);

    @Delete
    void delete(Photo item);

    @RawQuery
    Cursor getPhotosUnsynced(SupportSQLiteQuery query);

    @Query("UPDATE photo SET _uploaded = :uploaded WHERE _path=:path ")
    void updatePhoto(String path, boolean uploaded);

    @Query("UPDATE photo SET _signature_id = :signatureId WHERE _signature_id = -111")
    void updateActivePhoto(long signatureId);

    @Query("SELECT _path FROM photo WHERE _signature_id = :signatureKey ORDER BY rowid")
    Cursor getPhotosPath(long signatureKey);

    @Query("SELECT _path FROM photo WHERE _signature_id = :signatureKey")
    Cursor getPhotosPathWithoutOrder(long signatureKey);

    @Query("DELETE FROM photo WHERE _signature_id = :signatureId")
    void deleteFromSignature(long signatureId);

    @Query("SELECT count(*) FROM " + TrackDB.TBL_PHOTO + " WHERE _signature_id = :signatureId ")
    Cursor getNewPhotosCount(long signatureId);

    @Query("DELETE FROM photo")
    void nukeTable();

}
