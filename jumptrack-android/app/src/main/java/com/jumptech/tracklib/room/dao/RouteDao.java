package com.jumptech.tracklib.room.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.jumptech.tracklib.room.entity.Route;

@Dao
public interface RouteDao {

    @Insert
    void insert(Route item);

    @Update
    void update(Route item);

    @Delete
    void delete(Route item);

    @Query("UPDATE route SET command = :command")
    void updateCommand(String command);

    @Query("UPDATE route SET finished = :finished")
    int updateFinishedRoutes(boolean finished);

    @Query("UPDATE route SET order_uploaded = :uploaded")
    void updateOrderUpload(boolean uploaded);

    @Query("SELECT id, name, command, finished, order_uploaded FROM route")
    Cursor fetchRoute();

    @Query("INSERT OR REPLACE INTO route (id, name, finished, order_uploaded) VALUES (:routeKey, :name, 0, 1)")
    void insertRoute(long routeKey, String name);

    @Query("DELETE FROM route")
    void nukeTable();

}
