package eci.technician.models.order;


import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class PartUsageStatus extends RealmObject {
    @PrimaryKey
    private int usageStatusId;
    private String usageStatus;

    public PartUsageStatus(int usageStatusId, String usageStatus) {
        this.usageStatusId = usageStatusId;
        this.usageStatus = usageStatus;
    }

    public PartUsageStatus() {
    }

    public int getUsageStatusId() {
        return usageStatusId;
    }

    public void setUsageStatusId(int usageStatusId) {
        this.usageStatusId = usageStatusId;
    }

    public String getUsageStatus() {
        return usageStatus;
    }

    public void setUsageStatus(String usageStatus) {
        this.usageStatus = usageStatus;
    }
}
