package com.jumptech.networking.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConfigResponse {

    @SerializedName("driver-image")
    private DriverImage driverImage;
    private Gps gps;
    @SerializedName("stop-sort-enable")
    private boolean stopSortEnable;
    @SerializedName("sync-rate")
    private Integer syncRate;
    @SerializedName("unscheduled-dropoff")
    private boolean unscheduledDropoff;
    @SerializedName("unscheduled-pickup")
    private boolean unscheduledPickup;
    @SerializedName("partial-reason")
    private List<String> partialReasons;

    public static class DriverImage {
        public Integer max;
        @SerializedName("short-side-pixel")
        public Integer shortSidePixel;
    }

    public static class Gps {
        public Boolean enabled;
        @SerializedName("min-duration")
        public Integer minDuration;
        @SerializedName("min-distance")
        public Integer minDistance;
    }

    public DriverImage getDriverImage() {
        return driverImage;
    }

    public Gps getGps() {
        return gps;
    }

    public boolean isStopSortEnable() {
        return stopSortEnable;
    }

    public Integer getSyncRate() {
        return syncRate;
    }

    public boolean isUnscheduledDropoff() {
        return unscheduledDropoff;
    }

    public boolean isUnscheduledPickup() {
        return unscheduledPickup;
    }

    public List<String> getPartialReasons() {
        return partialReasons;
    }
}
