package eci.technician.models.gps;

import android.location.Location;

import com.google.gson.annotations.SerializedName;

public class GPSLocation {
    @SerializedName("Latitude")
    private double latitude;

    @SerializedName("Longitude")
    private double longitude;

    @SerializedName("Speed")
    private double speed;

    @SerializedName("Altitude")
    private double altitude;

    public GPSLocation(double latitude, double longitude, double speed, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.altitude = altitude;
    }

    public static GPSLocation fromAndroidLocation(Location location) {
        return location == null ? null : new GPSLocation(location.getLatitude(), location.getLongitude(), location.getSpeed(), location.getAltitude());
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}