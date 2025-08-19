package eci.technician.models.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;

public class UnavailableParts extends RealmObject implements Serializable {
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @SerializedName("ErrorMessage")
    private String errorMessage;
    @Ignore
    @SerializedName("Parts")
    private List<Parts> parts = new ArrayList<>();

    public List<Parts> getParts() {
        return parts;
    }

    public void setParts(List<Parts> parts) {
        this.parts = parts;
    }

}
