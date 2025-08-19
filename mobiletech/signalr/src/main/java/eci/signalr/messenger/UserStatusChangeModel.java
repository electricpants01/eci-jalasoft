package eci.signalr.messenger;

import com.google.gson.annotations.SerializedName;

public class UserStatusChangeModel {
    @SerializedName("ChatIdent")
    private String chatIdent;
    @SerializedName("Status")
    private String status;

    public String getChatIdent() {
        return chatIdent;
    }

    public void setChatIdent(String chatIdent) {
        this.chatIdent = chatIdent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
