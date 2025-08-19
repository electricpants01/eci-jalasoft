package eci.signalr.messenger;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class Conversation extends RealmObject {
    @PrimaryKey
    @SerializedName("ConversationId")
    private String conversationId;
    @SerializedName("ConversationName")
    private String conversationName;
    @SerializedName("ConversationType")
    private String conversationType;
    @SerializedName("Creator")
    private String creator;
    @SerializedName("Archive")
    private boolean archive;
    @SerializedName("UpdateTime")
    private Date updateTime;
    @SerializedName("UnreadMessageCount")
    private int unreadMessageCount;
    @SerializedName("LastMessage")
    private String lastMessage;
    @SerializedName("Users")
    @Ignore
    private List<ConversationUser> users;

    @SerializedName("SenderId")
    private String userIdent;

    private String userName;

    public static String CONVERSATION_ID = "conversationId";

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public boolean getArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public long getTimestamp() {
        return updateTime.getTime() * -1;
    }

    public void setTimestamp(long timestamp) {
        updateTime = new Date(timestamp * -1);
    }

    public int getUnreadMessageCount() {
        return unreadMessageCount;
    }

    public void setUnreadMessageCount(int unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public List<ConversationUser> getUsers() {
        return users;
    }

    public void setUsers(List<ConversationUser> users) {
        this.users = users;
    }

    public String getUserIdent() {
        return userIdent;
    }

    public void setUserIdent(String userIdent) {
        this.userIdent = userIdent;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFirstLetter() {
        if (userName == null || userName.isEmpty()) {
            return "U";
        }
        return userName.substring(0, 1);
    }
}