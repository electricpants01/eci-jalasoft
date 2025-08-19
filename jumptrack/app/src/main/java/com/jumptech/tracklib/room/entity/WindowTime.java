package com.jumptech.tracklib.room.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_WINDOW_TIME)
public class WindowTime {

    @PrimaryKey
    @ColumnInfo(name = "_id")
    Integer id;

    @ColumnInfo(name = "_window_id")
    Integer windowId;

    @ColumnInfo(name = "_startSec")
    long startSec;

    @ColumnInfo(name = "_endSec")
    long endSec;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWindowId() {
        return windowId;
    }

    public void setWindowId(Integer windowId) {
        this.windowId = windowId;
    }

    public long getStartSec() {
        return startSec;
    }

    public void setStartSec(long startSec) {
        this.startSec = startSec;
    }

    public long getEndSec() {
        return endSec;
    }

    public void setEndSec(long endSec) {
        this.endSec = endSec;
    }
}
