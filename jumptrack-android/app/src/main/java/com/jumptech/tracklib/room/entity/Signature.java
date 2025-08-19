package com.jumptech.tracklib.room.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_SIGNATURE)
public class Signature {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    long id;

    @ColumnInfo(name = "_note")
    String note;

    @ColumnInfo(name = "_signee")
    String signee;

    @ColumnInfo(name = "_signed")
    String signed;

    @ColumnInfo(name = "_path")
    String path;

    @ColumnInfo(name = "_crumb")
    String crumb;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSignee() {
        return signee;
    }

    public void setSignee(String signee) {
        this.signee = signee;
    }

    public String getSigned() {
        return signed;
    }

    public void setSigned(String signed) {
        this.signed = signed;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCrumb() {
        return crumb;
    }

    public void setCrumb(String crumb) {
        this.crumb = crumb;
    }

    public Integer getUploaded() {
        return uploaded;
    }

    public void setUploaded(Integer uploaded) {
        this.uploaded = uploaded;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @ColumnInfo(name = "_uploaded")
    Integer uploaded;

    @ColumnInfo(name = "_reference")
    String reference;

}
