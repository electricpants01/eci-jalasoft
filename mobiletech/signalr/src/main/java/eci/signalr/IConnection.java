package eci.signalr;

import eci.signalr.event.SignalREventListener;
import eci.signalr.messenger.Conversation;
import eci.signalr.messenger.MessengerEventListener;

import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.hubs.HubProxy;

public interface IConnection {
    void connect();

    void disconnect();

    void initialize(String server);

    void setToken(String token);

    void addConnectionListener(ConnectionListener connectionListener);

    void removeConnectionListener(ConnectionListener connectionListener);

    void addEventListener(SignalREventListener eventListener);

    void removeEventListener(SignalREventListener eventListener);

    void addMessengerEventListener(MessengerEventListener messengerEventListener);

    void removeMessengerEventListener(MessengerEventListener messengerEventListener);

    void updateServiceCalls();

    void updateConversation(Conversation conversation);

    HubProxy getMessagingProxy();

    ConnectionState getConnectionState();
}
