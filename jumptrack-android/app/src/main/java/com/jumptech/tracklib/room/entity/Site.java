package com.jumptech.tracklib.room.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_SITE)
public class Site {

    @PrimaryKey
    @ColumnInfo(name = "_key")
    int key;

    @ColumnInfo(name = "_account")
    String account;

    @ColumnInfo(name = "_name")
    String name;

    @ColumnInfo(name = "_address1")
    String address1;

    @ColumnInfo(name = "_address2")
    String address2;

    @ColumnInfo(name = "_address3")
    String address3;

    @ColumnInfo(name = "_city")
    String city;

    @ColumnInfo(name = "_state")
    String state;

    @ColumnInfo(name = "_zip")
    String zip;

    @ColumnInfo(name = "_address")
    String address;

    @ColumnInfo(name = "_phone")
    String phone;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone(){
        return phone;
    }

    public void setPhone(String phone){
        this.phone = phone;
    }
}
