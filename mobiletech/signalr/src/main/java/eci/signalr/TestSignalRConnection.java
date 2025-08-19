package eci.signalr;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Date;

import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.NullLogger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.StateChangedCallback;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class TestSignalRConnection {
    private static final String TAG = "SIGNAL_R";
    private HubConnection connection;
    private HubProxy someProxy;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable connectSignalRRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, String.format("Time %s", new Date().toString()));
            connect();
        }
    };

    public void connect() {
        if (connection == null) {
            throw new RuntimeException("Connection not initialized");
        }
        try {
            SignalRFuture<Void> future = connection.start(new ServerSentEventsTransport(new NullLogger()));
            future.done(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize(String server) {
        if (connection != null) {
            throw new RuntimeException("Connection already initialized");
        }

        try {
            connection = new HubConnection(server);

            someProxy = connection.createHubProxy("SomeHub");
            connection.connected(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Connected");
                }
            });

            connection.stateChanged(new StateChangedCallback() {
                @Override
                public void stateChanged(final ConnectionState oldState, final ConnectionState newState) {
                    Log.d(TAG, oldState + "=>" + newState);
                }
            });

            connection.closed(new Runnable() {
                @Override
                public void run() {
                    retryConnect();
                }
            });

            //initializeEventListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retryConnect() {
        int seconds = 3;
        Log.d(TAG, String.format("Waiting %s seconds", seconds));
        handler.removeCallbacks(connectSignalRRunnable);
        Log.d(TAG, String.format("Time %s", new Date().toString()));
        handler.postDelayed(connectSignalRRunnable, seconds * 1000);
    }
}
