package com.jumptech.tracklib.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_LINE)
public class Line {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_key")
    public Long key;

    @ColumnInfo(name = "_delivery_key")
    public Integer deliveryKey;

    @ColumnInfo(name = "_qty_target")
    public Integer qtyTarget;

    @ColumnInfo(name = "_name")
    public String name;

    @ColumnInfo(name = "_product_no")
    public String productNo;

    @ColumnInfo(name = "_uom")
    public String uom;

    @ColumnInfo(name = "_desc")
    public String desc;

    @ColumnInfo(name = "_scan")
    public String scan;

    @ColumnInfo(name = "_qty_accept")
    public Integer qtyAccept;

    @ColumnInfo(name = "_partial_reason")
    public String partialReason;

    @ColumnInfo(name = "_scanning")
    public Boolean scanning = false;

    @ColumnInfo(name = "_qty_loaded")
    public Integer qtyLoaded;

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public Integer getDeliveryKey() {
        return deliveryKey;
    }

    public void setDeliveryKey(Integer deliveryKey) {
        this.deliveryKey = deliveryKey;
    }

    public Integer getQtyTarget() {
        return qtyTarget;
    }

    public void setQtyTarget(Integer qtyTarget) {
        this.qtyTarget = qtyTarget;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductNo() {
        return productNo;
    }

    public void setProductNo(String productNo) {
        this.productNo = productNo;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getScan() {
        return scan;
    }

    public void setScan(String scan) {
        this.scan = scan;
    }

    public Integer getQtyAccept() {
        return qtyAccept;
    }

    public void setQtyAccept(Integer qtyAccept) {
        this.qtyAccept = qtyAccept;
    }

    public String getPartialReason() {
        return partialReason;
    }

    public void setPartialReason(String partialReason) {
        this.partialReason = partialReason;
    }

    public Boolean getScanning() {
        return scanning;
    }

    public void setScanning(Boolean scanning) {
        this.scanning = scanning;
    }

    public Integer getQtyLoaded() {
        return qtyLoaded;
    }

    public void setQtyLoaded(Integer qtyLoaded) {
        this.qtyLoaded = qtyLoaded;
    }

}
