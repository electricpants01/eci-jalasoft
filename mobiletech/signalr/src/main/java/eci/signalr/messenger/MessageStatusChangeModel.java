package eci.signalr.messenger;

import com.google.gson.annotations.SerializedName;

public class MessageStatusChangeModel {
    @SerializedName("ConversationId")
    private String conversationId;
    @SerializedName("MessageId")
    private String messageId;
    @SerializedName("Status")
    private String status;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
