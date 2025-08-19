package eci.technician.models.order;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ServiceCallProperty extends RealmObject {
    public static final String CALL_ID = "serviceCallId";

    @PrimaryKey
    private int serviceCallId;
    private String comments;

    public ServiceCallProperty() {
    }

    public ServiceCallProperty(int serviceCallId, String comments) {
        this.serviceCallId = serviceCallId;
        this.comments = comments;
    }

    public int getServiceCallId() {
        return serviceCallId;
    }

    public void setServiceCallId(int serviceCallId) {
        this.serviceCallId = serviceCallId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
