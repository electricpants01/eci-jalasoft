package eci.signalr.event;

import com.google.gson.JsonElement;

public interface SignalREventListener {
    void serviceCallUpdated(JsonElement jsonElement);

    void newServiceCall(JsonElement jsonElement);

    void serviceCallsUpdated();

    void userStatusChanged(String status);
}
