package eci.technician.models.order;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ServiceCallMeter extends RealmObject {
    public static final String CALL_ID = "serviceOrderId";

    @PrimaryKey
    private String id = UUID.randomUUID().toString();
    private int serviceOrderId;
    private int meterId;
    private double meterValue;
    private double userLastValue;


    public double getUserLastValue() {
        return userLastValue;
    }

    public void setUserLastValue(double userLastValue) {
        this.userLastValue = userLastValue;
    }

    public ServiceCallMeter() {
    }

    public ServiceCallMeter(int serviceOrderId, int meterId, double meterValue, double userLastValue) {
        this.serviceOrderId = serviceOrderId;
        this.meterId = meterId;
        this.meterValue = meterValue;
        this.userLastValue = userLastValue;
    }

    public int getServiceOrderId() {
        return serviceOrderId;
    }

    public void setServiceOrderId(int serviceOrderId) {
        this.serviceOrderId = serviceOrderId;
    }

    public int getMeterId() {
        return meterId;
    }

    public void setMeterId(int meterId) {
        this.meterId = meterId;
    }

    public double getMeterValue() {
        return meterValue;
    }

    public void setMeterValue(double meterValue) {
        this.meterValue = meterValue;
    }

    public String getId() {
        return id;
    }
}
