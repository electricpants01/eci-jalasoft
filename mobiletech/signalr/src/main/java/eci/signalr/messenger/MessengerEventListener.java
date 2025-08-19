package eci.signalr.messenger;

public interface MessengerEventListener {
    void updateAll();

    void messageReceived(Message message);

    void messageStatusChanged(MessageStatusChangeModel messageStatusChangeModel);

    void typingMessage(TypingModel typingModel);

    void userStatusChanged(UserStatusChangeModel userStatusChangeModel);

    void conversationUsersAdded(Conversation conversation);

    void conversationUsersRemoved(Conversation conversation);

    void conversationNameUpdated(Conversation conversation);

    void hideConversation(Conversation conversation);

    void unHideConversation(Conversation conversation);

    void updateConversation(Conversation conversation);

    void onConversationPressed(Conversation item);

    void updateStatusMessage();
}
