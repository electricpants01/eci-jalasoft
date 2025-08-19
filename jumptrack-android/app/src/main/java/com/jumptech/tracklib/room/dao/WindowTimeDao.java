package com.jumptech.tracklib.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jumptech.tracklib.room.entity.WindowTime;

@Dao
public interface WindowTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WindowTime item);

    @Update
    void update(WindowTime item);

    @Delete
    void delete(WindowTime item);

    @Query("DELETE FROM windowTime")
    void nukeTable();
}
