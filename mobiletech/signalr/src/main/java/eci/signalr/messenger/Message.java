package eci.signalr.messenger;

import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Message extends RealmObject {
    @SerializedName("ConversationId")
    private String conversationId;
    @PrimaryKey
    @SerializedName("MessageId")
    private String messageId;
    @SerializedName("SenderId")
    private String senderId;
    @SerializedName("SenderName")
    private String senderName;
    @SerializedName("SenderType")
    private String senderType;
    @SerializedName("MessageTime")
    private Date messageTime;
    @SerializedName("Message")
    private String message;
    private boolean myMessage;
    private String status;
    private boolean seenStatusSent;
    private boolean read;

    public static final String CONVERSATION_ID_QUERY_NAME = "conversationId";
    public static final String MESSAGE_ID = "messageId";
    public static final String MY_MESSAGE = "myMessage";
    public static final String MESSAGE_TIME = "messageTime";

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

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Date getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(Date messageTime) {
        this.messageTime = messageTime;
    }

    public long getTimestamp() {
        return messageTime.getTime() * -1;
    }

    public void setTimestamp(long timestamp) {
        messageTime = new Date(timestamp * -1);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isMyMessage() {
        return myMessage;
    }

    public void setMyMessage(boolean myMessage) {
        this.myMessage = myMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateTime() {
        DateFormat format = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
        return messageTime == null ? "" : format.format(messageTime);
    }

    public String getUserFirstLetter() {
        if (senderName == null || senderName.isEmpty()) {
            return "";
        }
        return senderName.substring(0, 1);
    }

    public int getStatusState() {
        if (status == null) {
            return 0;
        }
        switch (status.toLowerCase()) {
            case "delivered":
                return 1;
            case "seen":
                return 2;
            default:
                return 0;
        }
    }

    public boolean getShowStatus() {
        return myMessage && getStatusState() > 0;
    }

    public boolean isSeenStatusSent() {
        return seenStatusSent;
    }

    public void setSeenStatusSent(boolean seenStatusSent) {
        this.seenStatusSent = seenStatusSent;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
