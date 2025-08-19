package eci.technician.models;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.net.HttpURLConnection;
import java.util.List;

public class ProcessingResult {
    @SerializedName("HasError")
    private boolean hasError;
    @SerializedName("Errors")
    private List<ErrorResult> errors;
    @SerializedName("Result")
    private String result;
    private int statusCode;

    public ProcessingResult() {
        statusCode = HttpURLConnection.HTTP_OK;
    }

    public static String formatErrors(ErrorResult[] errors) {
        String[] errorTexts = new String[errors.length];
        for (int i = 0; i < errors.length; i++) {
            errorTexts[i] = errors[i].getFriendlyError();
        }
        return TextUtils.join("\n", errorTexts);
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public List<ErrorResult> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorResult> errors) {
        this.errors = errors;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getFormattedErrors() {
        return formatErrors(errors.toArray(new ErrorResult[errors.size()]));
    }

    public int getFirstErrorCode() {
        if (errors.isEmpty()) {
            return 0;
        }
        return errors.get(0).getErrorCode();
    }
}