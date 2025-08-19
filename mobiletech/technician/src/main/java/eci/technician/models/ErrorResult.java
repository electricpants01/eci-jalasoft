package eci.technician.models;

import com.google.gson.annotations.SerializedName;

import eci.technician.helpers.api.ApiErrorHelper;

public class ErrorResult {
    @SerializedName("ErrorText")
    private String errorText;
    @SerializedName("ErrorCode")
    private int errorCode;

    public ErrorResult(String errorText) {
        this.errorText = errorText;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getFriendlyError(){
        return ApiErrorHelper.INSTANCE.getUserFriendlyError(errorText);
    }
}
