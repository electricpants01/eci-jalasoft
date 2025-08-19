package com.jumptech.tracklib.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_PHOTO)
public class Photo {

    @PrimaryKey(autoGenerate = true)
    public int key;

    @ColumnInfo(name = "_signature_id")
    long signatureId;

    @ColumnInfo(name = "_path")
    String path;

    @ColumnInfo(name = "_uploaded")
    boolean uploaded;

    public long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }
}
