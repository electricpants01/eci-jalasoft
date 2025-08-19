package eci.signalr.messenger;

import com.google.gson.annotations.SerializedName;

public class TypingModel {
    @SerializedName("ConversationId")
    private String conversationId;
    @SerializedName("WriterIdent")
    private String writerIdent;
    @SerializedName("WriterName")
    private String writerName;
    @SerializedName("Message")
    private String message;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getWriterIdent() {
        return writerIdent;
    }

    public void setWriterIdent(String writerIdent) {
        this.writerIdent = writerIdent;
    }

    public String getWriterName() {
        return writerName;
    }

    public void setWriterName(String writerName) {
        this.writerName = writerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTypingMessage() {
        return String.format("%s is typing...", writerName);
    }
}
