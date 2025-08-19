package com.jumptech.tracklib.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_STOP)
public class Stop {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_key")
    Integer key;

    @ColumnInfo(name = "_site_key")
    Long siteKey;

    @ColumnInfo(name = "_planned")
    Boolean planned = false;

    @ColumnInfo(name = "_sort")
    Integer sort;

    @ColumnInfo(name = "_base_delivery_key")
    Long baseDeliveryKey;

    @ColumnInfo(name = "_signature_key")
    Long signatureKey = null;

    @ColumnInfo(name = "_window_id")
    Integer windowId;

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Long getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(Long siteKey) {
        this.siteKey = siteKey;
    }

    public Boolean getPlanned() {
        return planned;
    }

    public void setPlanned(Boolean planned) {
        this.planned = planned;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Long getBaseDeliveryKey() {
        return baseDeliveryKey;
    }

    public void setBaseDeliveryKey(Long baseDeliveryKey) {
        this.baseDeliveryKey = baseDeliveryKey;
    }

    public Long getSignatureKey() {
        return signatureKey;
    }

    public void setSignatureKey(Long signatureKey) {
        this.signatureKey = signatureKey;
    }

    public Integer getWindowId() {
        return windowId;
    }

    public void setWindowId(Integer windowId) {
        this.windowId = windowId;
    }
}
