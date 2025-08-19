package eci.technician.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TechnicianItem extends RealmObject {
    @PrimaryKey
    @SerializedName("Technician_ID")
    private int id;
    @SerializedName("FirstName")
    private String firstName;
    @SerializedName("LastName")
    private String lastName;
    @SerializedName("Technician_Code")
    private String code;
    @SerializedName("Email")
    private String email;
    @SerializedName("CompanyId")
    private String guidCompanyId;
    @SerializedName("Id")
    private String guid;
    @SerializedName("LastUpdateString")
    private Date lastUpdateString;
    @SerializedName("GPSUpdateTimeString")
    private Date gpsUpdateTimeString;


    public Date getGpsUpdateTimeString() {
        return gpsUpdateTimeString;
    }

    public void setGpsUpdateTimeString(Date gpsUpdateTimeString) {
        this.gpsUpdateTimeString = gpsUpdateTimeString;
    }


    public Date getLastUpdateString() {
        return lastUpdateString;
    }

    public void setLastUpdateString(Date lastUpdateString) {
        this.lastUpdateString = lastUpdateString;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getGuidCompanyId() {
        return guidCompanyId;
    }

    public void setGuidCompanyId(String guidCompanyId) {
        this.guidCompanyId = guidCompanyId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFullName() {
        return String.format("%s %s", firstName, lastName);
    }
}
