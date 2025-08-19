package eci.signalr.messenger;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

public class ConversationUser extends RealmObject {
    private String conversationId;
    @SerializedName("ChatIdent")
    private String chatIdent;
    @SerializedName("ChatName")
    private String chatName;
    @SerializedName("UserType")
    private String userType;

    public String getChatIdent() {
        return chatIdent;
    }

    public void setChatIdent(String chatIdent) {
        this.chatIdent = chatIdent;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserFirstLetter() {
        if (chatName == null || chatName.isEmpty()) {
            return "U";
        }
        return chatName.substring(0, 1);
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
