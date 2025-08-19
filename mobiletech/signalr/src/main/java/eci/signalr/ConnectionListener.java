package eci.signalr;

import microsoft.aspnet.signalr.client.ConnectionState;

public interface ConnectionListener {
    void connected();

    void disconnected();

    void stateChanged(ConnectionState oldState, ConnectionState newState);

    void reconnecting();
}
