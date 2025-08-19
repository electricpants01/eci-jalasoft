package eci.technician.models.order;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class StatusChangeModel {
    @SerializedName("CallId")
    private Integer callId = null;
    @SerializedName("TechnicianId")
    private Integer technicianId = null;
    @SerializedName("Odometer")
    private Double odometer = null;
    @SerializedName("ActionTime")
    private Date actionTime = null;
    @SerializedName("CodeId")
    private Integer codeId = null;
    @SerializedName("Comments")
    private String comments = null;
    @SerializedName("ProblemCodes")
    private List<Integer> problemCodes;
    @SerializedName("RepairCodes")
    private List<Integer> repairCodes;
    @SerializedName("UsedParts")
    private List<Map<String, Object>> usedParts;
    @SerializedName("Meters")
    private List<EquipmentMeter> meters;
    @SerializedName("ActivityCodeId")
    private int activityCodeId;
    @SerializedName("FileContentBase64")
    private String fileContentBase64;
    @SerializedName("ContentType")
    private String contentType;
    @SerializedName("FileName")
    private String fileName;
    @SerializedName("FileSize")
    private int fileSize;
    @SerializedName("SigneeName")
    private String signeeName;
    @SerializedName("Labor")
    private Labor labor;
    @SerializedName("IncompleteCodeId")
    private Integer incompleteCodeId;
    @SerializedName("PreventativeMaintenance")
    private boolean preventativeMaintenance;
    @SerializedName("EmailDetail")
    private Map<String, Object> emailDetail;

    private int holdCodeTypeId;

    @SerializedName("StatusCode_Code")
    private String statusCodeCode = "";

    @SerializedName("OnHoldProblemDescription")
    private String description = "";

    public int getHoldCodeTypeId() {
        return holdCodeTypeId;
    }

    public void setHoldCodeTypeId(int holdCodeTypeId) {
        this.holdCodeTypeId = holdCodeTypeId;
    }

    public boolean isPreventativeMaintenance() {
        return preventativeMaintenance;
    }

    public void setPreventativeMaintenance(boolean preventativeMaintenance) {
        this.preventativeMaintenance = preventativeMaintenance;
    }

    public Integer getIncompleteCodeId() {
        return incompleteCodeId;
    }

    public void setIncompleteCodeId(Integer incompleteCodeId) {
        this.incompleteCodeId = incompleteCodeId;
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

    public double getOdometer() {
        return odometer;
    }

    public void setOdometer(double odometer) {
        this.odometer = odometer;
    }

    public Date getActionTime() {
        return actionTime;
    }

    public void setActionTime(Date actionTime) {
        this.actionTime = actionTime;
    }

    public Integer getCodeId() {
        return codeId;
    }

    public void setCodeId(int codeId) {
        this.codeId = codeId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<Integer> getProblemCodes() {
        return problemCodes;
    }

    public void setProblemCodes(List<Integer> problemCodes) {
        this.problemCodes = problemCodes;
    }

    public List<Integer> getRepairCodes() {
        return repairCodes;
    }

    public void setRepairCodes(List<Integer> repairCodes) {
        this.repairCodes = repairCodes;
    }

    public List<Map<String, Object>> getUsedParts() {
        return usedParts;
    }

    public void setUsedParts(List<Map<String, Object>> usedParts) {
        this.usedParts = usedParts;
    }

    public List<EquipmentMeter> getMeters() {
        return meters;
    }

    public void setMeters(List<EquipmentMeter> meters) {
        this.meters = meters;
    }

    public int getActivityCodeId() {
        return activityCodeId;
    }

    public void setActivityCodeId(int activityCodeId) {
        this.activityCodeId = activityCodeId;
    }

    public String getFileContentBase64() {
        return fileContentBase64;
    }

    public void setFileContentBase64(String fileContentBase64) {
        this.fileContentBase64 = fileContentBase64;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getSigneeName() {
        return signeeName;
    }

    public void setSigneeName(String signeeName) {
        this.signeeName = signeeName;
    }

    public Labor getLabor() {
        return labor;
    }

    public void setLabor(Labor labor) {
        this.labor = labor;
    }

    public Map<String, Object> getEmailDetail() {
        return emailDetail;
    }

    public void setEmailDetail(Map<String, Object> emailDetail) {
        this.emailDetail = emailDetail;
    }

    public String getStatusCodeCode() {
        return statusCodeCode;
    }

    public void setStatusCodeCode(String statusCodeCode) {
        this.statusCodeCode = statusCodeCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}