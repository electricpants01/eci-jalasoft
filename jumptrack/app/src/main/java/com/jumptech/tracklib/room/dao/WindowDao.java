package com.jumptech.tracklib.room.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jumptech.tracklib.room.entity.Window;

@Dao
public interface WindowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Window item);

    @Update
    void update(Window item);

    @Delete
    void delete(Window item);

    @Query("SELECT _display FROM window WHERE _id = :windowID")
    Cursor getDisplay(Integer windowID);

    @Query("DELETE FROM window")
    void nukeTable();
}
