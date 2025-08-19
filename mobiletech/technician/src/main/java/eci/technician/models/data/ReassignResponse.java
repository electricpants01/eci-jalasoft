package eci.technician.models.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import io.realm.RealmObject;

public class ReassignResponse implements Serializable {
    public UnavailableParts getUnavailableParts() {
        return unavailableParts;
    }

    public void setUnavailableParts(UnavailableParts unavailableParts) {
        this.unavailableParts = unavailableParts;
    }

    @SerializedName("UnavailableParts")
    private UnavailableParts unavailableParts;

    public UnavailableParts getNeededParts() {
        return neededParts;
    }

    public void setNeededParts(UnavailableParts neededParts) {
        this.neededParts = neededParts;
    }

    @SerializedName("NeededParts")
    private UnavailableParts neededParts;
}
