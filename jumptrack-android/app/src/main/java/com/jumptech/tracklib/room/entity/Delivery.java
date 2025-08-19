package com.jumptech.tracklib.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_DELIVERY)
public class Delivery {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_key")
    long key;

    @ColumnInfo(name = "_stop_key")
    Long stopKey;

    @ColumnInfo(name = "_site_key")
    Long siteKey;

    @ColumnInfo(name = "_type")
    String type;

    @ColumnInfo(name = "_delivery_cd")
    String deliveryCd;

    @ColumnInfo(name = "_delivery_note")
    String deliveryNote;

    @ColumnInfo(name = "_signing")
    Integer signing;

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public Long getStopKey() {
        return stopKey;
    }

    public void setStopKey(Long stopKey) {
        this.stopKey = stopKey;
    }

    public Long getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(Long siteKey) {
        this.siteKey = siteKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeliveryCd() {
        return deliveryCd;
    }

    public void setDeliveryCd(String deliveryCd) {
        this.deliveryCd = deliveryCd;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public Integer getSigning() {
        return signing;
    }

    public void setSigning(Integer signing) {
        this.signing = signing;
    }
}
