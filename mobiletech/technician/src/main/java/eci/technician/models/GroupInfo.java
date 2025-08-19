package eci.technician.models;

import com.google.gson.annotations.SerializedName;

public class GroupInfo {
    @SerializedName("GroupId")
    private String groupId;
    @SerializedName("GroupName")
    private String GroupName;
    @SerializedName("IsManager")
    private boolean isManager;
    @SerializedName("IsTechnician")
    private boolean isTechnician;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return GroupName;
    }

    public void setGroupName(String groupName) {
        GroupName = groupName;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        isManager = manager;
    }

    public boolean isTechnician() {
        return isTechnician;
    }

    public void setTechnician(boolean technician) {
        isTechnician = technician;
    }
}
