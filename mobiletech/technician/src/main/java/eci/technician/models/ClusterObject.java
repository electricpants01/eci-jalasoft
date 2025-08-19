package eci.technician.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterObject implements ClusterItem {

    private LatLng position;
    private String title;
    private String snippet;
    private String callNumber_Code;
    private String status_StatusCode;
    private String statusCode;
    private double color;


    public String getStatus_StatusCode() {
        return status_StatusCode;
    }

    public void setStatus_StatusCode(String status_StatusCode) {
        this.status_StatusCode = status_StatusCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
    public String getCallNumber_Code() {
        return callNumber_Code;
    }

    public void setCallNumber_Code(String callNumber_Code) {
        this.callNumber_Code = callNumber_Code;
    }

    public int getCallNumber_ID() {
        return callNumber_ID;
    }

    public void setCallNumber_ID(int callNumber_ID) {
        this.callNumber_ID = callNumber_ID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private int callNumber_ID;
    private String id;

    public ClusterObject(LatLng position) {
        this.position = position;
    }

    public ClusterObject(LatLng position, String callNumber_Code, String status_StatusCode, String statusCode, int callNumber_ID, String id, double color) {
        this.position = position;
        this.callNumber_Code = callNumber_Code;
        this.callNumber_ID = callNumber_ID;
        this.id = id;
        this.status_StatusCode = status_StatusCode;
        this.statusCode = statusCode;
        this.color = color;
    }

    public ClusterObject(LatLng position, String title, String snippet) {
        this.position = position;
        this.title = title;
        this.snippet = snippet;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public double getColor() {
        return color;
    }

    public void setColor(double color) {
        this.color = color;
    }
}
