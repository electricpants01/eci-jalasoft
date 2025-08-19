package com.jumptech.tracklib.room.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

@Dao
public interface GenericDao {

    @RawQuery
    Cursor genericQuery(SupportSQLiteQuery query);

}
