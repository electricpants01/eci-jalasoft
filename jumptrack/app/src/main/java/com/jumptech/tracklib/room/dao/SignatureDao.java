package com.jumptech.tracklib.room.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jumptech.tracklib.room.entity.Signature;

@Dao
public interface SignatureDao {

    @Insert
    void insert(Signature item);

    @Update
    void update(Signature item);

    @Delete
    void delete(Signature item);

    @Query("SELECT rowid FROM signature WHERE _id = -111")
    Cursor getActiveSignature();

    @Query("INSERT INTO signature ( _signee  , _signed  , _crumb  , _reference, _uploaded) VALUES (:signee,:signed,:crumb,:reference,0)  ")
    long insertActiveSignatureUpdated(String signee, String signed, String crumb, String reference);

    @Query("UPDATE signature SET  _signee = :signee, _signed = :signed, _crumb = :crumb, _reference = :reference  WHERE _id = :id")
    void updateActiveSignature(long id, String signee, String signed, String crumb, String reference);

    @Query("SELECT _id, _note, _signee, _path, _signed, _crumb, _reference FROM signature WHERE _id = :signatureKey")
    Cursor signature(long signatureKey);

    @Query("INSERT OR REPLACE INTO signature (_id,_uploaded) VALUES (:id,:uploaded)")
    void insertNewAssert(long id, boolean uploaded);

    @Query("UPDATE signature SET _note = :note where _id = :id")
    int updateNote(long id, String note);

    @Query("UPDATE signature SET _path = :path where _id = :id")
    void updatePath(long id, String path);

    @Query("SELECT _path from signature WHERE _id = :id")
    Cursor getPathsFromSignature(long id);

    @Query("DELETE FROM signature WHERE _id = :id")
    void deleteFromId(long id);

    @Query("UPDATE signature SET _uploaded = 1 where _id = :id")
    void updateUploaded(long id);

    @Query("SELECT * FROM signature WHERE _id = :id")
    Signature getFromId(long id);

    @Query("DELETE FROM signature")
    void nukeTable();

}
