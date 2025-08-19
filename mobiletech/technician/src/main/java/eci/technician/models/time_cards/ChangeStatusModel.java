package eci.technician.models.time_cards;

import com.google.gson.annotations.SerializedName;

import eci.technician.models.gps.GPSLocation;

import java.util.Date;

public class ChangeStatusModel {
    @SerializedName("Odometer")
    private double odometer;

    @SerializedName("Reason")
    private String reason;

    @SerializedName("ActionType")
    private String actionType;

    @SerializedName("Location")
    private GPSLocation gpsLocation;

    @SerializedName("Timestamp")
    private Date actionTime;

    public ChangeStatusModel() {
        this.reason = "";
        this.actionTime = new Date();
    }

    public ChangeStatusModel(double odometer) {
        this.odometer = odometer;
        this.actionTime = new Date();
    }

    public ChangeStatusModel(int odometer) {
        this.odometer = odometer;
        this.actionTime = new Date();
    }

    public double getOdometer() {
        return odometer;
    }

    public void setOdometer(double odometer) {
        this.odometer = odometer;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public GPSLocation getGpsLocation() {
        return gpsLocation;
    }

    public void setGpsLocation(GPSLocation gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public Date getActionTime() {
        return actionTime;
    }

    public void setActionTime(Date actionTime) {
        this.actionTime = actionTime;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}