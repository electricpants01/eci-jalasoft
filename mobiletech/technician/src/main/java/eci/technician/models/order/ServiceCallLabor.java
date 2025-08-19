package eci.technician.models.order;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import eci.technician.helpers.AppAuth;
import eci.technician.tools.ConstantsKotlin.TechnicianServiceCallLaborStatus;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ServiceCallLabor extends RealmObject {

    @PrimaryKey
    private String laborId;
    @SerializedName("Id")
    private String id;
    @SerializedName("CallId")
    private int callId;
    @SerializedName("TechnicianId")
    private int technicianId;
    @SerializedName("DispatchTimeStr")
    private Date dispatchTime;
    @SerializedName("ArriveTimeStr")
    private Date arriveTime;
    @SerializedName("DepartureTimeStr")
    private String departureTime;
    private String techName = "";

    public static final String CALL_ID = "callId";
    public static final String TECHNICIAN_ID = "technicianId";
    public static final String DISPATCH_TIME = "dispatchTime";
    public static final String ARRIVE_TIME = "arriveTime";
    public static final String LABOR_ID = "laborId";

    public ServiceCallLabor() {
        laborId = this.callId + "_" + this.technicianId;
    }

    public String getLaborId() {
        return laborId;
    }

    public void setLaborId(String laborId) {
        this.laborId = laborId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCallId() {
        return callId;
    }

    public void setCallId(int callId) {
        this.callId = callId;
    }

    public int getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(int technicianId) {
        this.technicianId = technicianId;
    }

    public Date getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Date dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public Date getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(Date arriveTime) {
        this.arriveTime = arriveTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getTechName() {
        return techName;
    }

    public void setTechName(String techName) {
        this.techName = techName;
    }
    @NonNull
    public TechnicianServiceCallLaborStatus getTechnicianAssistStatus() {
        try {
            return getDepartureTime() != null ? TechnicianServiceCallLaborStatus.COMPLETED : (getArriveTime() != null ? TechnicianServiceCallLaborStatus.ARRIVED : (getDispatchTime() != null ? TechnicianServiceCallLaborStatus.DISPATCHED : TechnicianServiceCallLaborStatus.PENDING));
        } catch (Exception e) {
            return TechnicianServiceCallLaborStatus.PENDING;
        }
    }
}
