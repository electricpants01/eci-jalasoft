package com.jumptech.tracklib.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Site;

@Dao
public interface SiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Site item);

    @Update
    void update(Site item);

    @Delete
    void delete(Site item);

    @Query("UPDATE " + TrackDB.TBL_SITE + " SET _address = _address1 || " +
            "CASE WHEN _address2 IS NULL THEN '' ELSE '\n' || _address2 END || " +
            "CASE WHEN _address3 IS NULL THEN '' ELSE '\n' || _address3 END || " +
            "'\n' || _city || ', ' || _state || ' ' || _zip ")
    void updateAddress();

    @Query("SELECT * FROM site WHERE _key = :id")
    Site getFromId(long id);

    @Query("DELETE FROM site")
    void nukeTable();
}
