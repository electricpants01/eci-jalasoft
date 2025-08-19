package eci.technician.models.gps;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class CarInfo {
    @SerializedName("Id")
    private UUID id;

    @SerializedName("Company")
    private String company;

    @SerializedName("CarNumber")
    private String carNumber;

    @SerializedName("UpdateInterval")
    private int updateInterval;

    @SerializedName("Ident")
    private String ident;

    @SerializedName("Imei")
    private String imei;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCarNumber() {
        return carNumber;
    }

    public void setCarNumber(String carNumber) {
        this.carNumber = carNumber;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }
}