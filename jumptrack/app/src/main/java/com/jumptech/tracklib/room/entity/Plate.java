package com.jumptech.tracklib.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_PLATE)
public class Plate {

    @PrimaryKey
    public int plate_key;

    public long getLineKey() {
        return lineKey;
    }

    public void setLineKey(long lineKey) {
        this.lineKey = lineKey;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public Boolean getScanned() {
        return scanned;
    }

    public void setScanned(Boolean scanned) {
        this.scanned = scanned;
    }

    public int getPlate_key() {
        return plate_key;
    }

    public void setPlate_key(int plate_key) {
        this.plate_key = plate_key;
    }

    @ColumnInfo(name = "_line_key")
    long lineKey;

    @ColumnInfo(name = "_plate")
    String plate;

    @ColumnInfo(name = "_scanned")
    Boolean scanned;

}
