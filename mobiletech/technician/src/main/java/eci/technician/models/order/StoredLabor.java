package eci.technician.models.order;

import java.util.Date;

import io.realm.RealmObject;

public class StoredLabor extends RealmObject {

    private int callID;
    private Date dispatchTime;
    private Date arriveTime;

    public int getCallID() {
        return callID;
    }

    public void setCallID(int callID) {
        this.callID = callID;
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

}
