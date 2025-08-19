package com.jumptech.tracklib.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jumptech.tracklib.room.TrackDB;

@Entity(tableName = TrackDB.TBL_ROUTE)
public class Route {


    @PrimaryKey
    long id;

    @ColumnInfo(name = "name")
    String name;

    @ColumnInfo(name = "command")
    String command;

    @ColumnInfo(name = "finished")
    int finished;

    @ColumnInfo(name = "order_uploaded")
    int orderUploaded;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getFinished() {
        return finished;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public int getOrderUploaded() {
        return orderUploaded;
    }

    public void setOrderUploaded(int orderUploaded) {
        this.orderUploaded = orderUploaded;
    }
}
