package eci.signalr;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import eci.signalr.event.SignalREventListener;
import eci.signalr.messenger.Conversation;
import eci.signalr.messenger.Message;
import eci.signalr.messenger.MessageStatusChangeModel;
import eci.signalr.messenger.MessengerEventListener;
import eci.signalr.messenger.TypingModel;
import eci.signalr.messenger.UserStatusChangeModel;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.ErrorCallback;
import microsoft.aspnet.signalr.client.MessageReceivedHandler;
import microsoft.aspnet.signalr.client.NullLogger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.StateChangedCallback;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class TechnicianConnection implements IConnection {
    private static final String SIGNAL_R_SERVICE_CALL_UPDATED = "ServiceCallUpdated";
    private static final String SIGNAL_R_NEW_SERVICE_CALL = "NewServiceCall";
    private static final String SIGNAL_R_RECEIVE_MESSAGE = "ReceiveMessage";
    private static final String SIGNAL_R_MESSAGE_STATUS_CHANGED = "MessageStatusChanged";
    private static final String SIGNAL_R_TYPING_MESSAGE = "TypingMessage";
    private static final String SIGNAL_R_USER_STATUS_CHANGED = "UserStatusChanged";
    private static final String SIGNAL_R_CONVERSATION_USERS_ADDED = "ConversationUsersAdded";
    private static final String SIGNAL_R_CONVERSATION_USERS_REMOVED = "ConversationUsersRemoved";
    private static final String SIGNAL_R_CONVERSATION_NAME_UPDATED = "ConversationNameUpdated";
    private static final String SIGNAL_R_HIDE_CONVERSATION = "HideConversation";
    private static final String SIGNAL_R_UN_HIDE_CONVERSATION = "UnHideConversation";
    private static final String SIGNAL_R_STATUS_CHANGED = "StatusChanged";
    private String TAG = "Signal R";
    private String token;
    private HubConnection connection;
    private HubProxy technicianProxy;
    private HubProxy messagingProxy;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ConnectionState connectionState;
    private Runnable connectSignalRRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, String.format("Time %s", new Date().toString()));
            connect();
        }
    };

    private List<ConnectionListener> connectionListeners = new ArrayList<>();
    private List<SignalREventListener> eventListeners = new ArrayList<>();
    private List<MessengerEventListener> messengerEventListeners = new ArrayList<>();

    public TechnicianConnection() {
    }

    @Override
    public ConnectionState getConnectionState(){
        synchronized (this){
            if (connectionState != ConnectionState.Connected){
                return ConnectionState.Disconnected;
            }else return connectionState;
        }
    }

    @Override
    public void connect() {
        if (token.isEmpty()) {
            return;
        }
        if (connection == null) {
            throw new RuntimeException("Connection not initialized");
        }
        try {
            String bearer = "Bearer " + token;
            bearer = URLEncoder.encode(bearer, "utf-8");

            String queryString = "Authorization=" + bearer;
            connection.setQueryString(queryString);
            SignalRFuture<Void> future = connection.start(new ServerSentEventsTransport(new NullLogger()));
            future.done(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            setToken("");
            connection.disconnect();
        }
    }

    @Override
    public void initialize(String server) {
        if (connection != null) {
            throw new RuntimeException("Connection already initialized");
        }

        try {
            String bearer = "Bearer " + token;
            bearer = URLEncoder.encode(bearer, "utf-8");

            String queryString = "Authorization=" + bearer;

            connection = new HubConnection(server, queryString, true, new NullLogger());
            connection.setGson(new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z").create());

            technicianProxy = connection.createHubProxy("TechnicianHub");
            messagingProxy = connection.createHubProxy("MessagingHub");

            connection.connected(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Connected");
                    connectionState = ConnectionState.Connected;
                }
            });

            connection.error(new ErrorCallback() {
                @Override
                public void onError(Throwable error) {
                }
            });

            connection.stateChanged(new StateChangedCallback() {
                @Override
                public void stateChanged(final ConnectionState oldState, final ConnectionState newState) {
                    Log.d(TAG, oldState + "=>" + newState);
                    connectionState = newState;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (ConnectionListener listener : connectionListeners) {
                                listener.stateChanged(oldState, newState);
                                if (newState == ConnectionState.Connected) {
                                    listener.connected();
                                }
                                if (newState == ConnectionState.Disconnected) {
                                    listener.disconnected();
                                }
                                if (newState == ConnectionState.Reconnecting) {
                                    listener.reconnecting();
                                }
                            }
                        }
                    });
                }
            });

            connection.closed(new Runnable() {
                @Override
                public void run() {
                    connectionState = ConnectionState.Disconnected;
                    retryConnect();
                }
            });

            connection.received(new MessageReceivedHandler() {
                @Override
                public void onMessageReceived(JsonElement json) {
                    Log.d("NEW_EVENT", new Gson().toJson(json));
                }
            });

            initializeEventListeners();
            initializeMessengerEventListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeMessengerEventListeners() {
        messagingProxy.subscribe(SIGNAL_R_RECEIVE_MESSAGE).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.messageReceived(createGson().fromJson(obj[0], Message.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_MESSAGE_STATUS_CHANGED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.messageStatusChanged(createGson().fromJson(obj[0], MessageStatusChangeModel.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_TYPING_MESSAGE).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.typingMessage(createGson().fromJson(obj[0], TypingModel.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_USER_STATUS_CHANGED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.userStatusChanged(createGson().fromJson(obj[0], UserStatusChangeModel.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_CONVERSATION_USERS_ADDED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.conversationUsersAdded(createGson().fromJson(obj[0], Conversation.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_CONVERSATION_USERS_REMOVED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.conversationUsersRemoved(createGson().fromJson(obj[0], Conversation.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_CONVERSATION_NAME_UPDATED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.conversationNameUpdated(createGson().fromJson(obj[0], Conversation.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_HIDE_CONVERSATION).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.hideConversation(createGson().fromJson(obj[0], Conversation.class));
                        }
                    }
                });
            }
        });

        messagingProxy.subscribe(SIGNAL_R_UN_HIDE_CONVERSATION).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (MessengerEventListener listener : messengerEventListeners) {
                            listener.unHideConversation(createGson().fromJson(obj[0], Conversation.class));
                        }
                    }
                });
            }
        });
    }

    private Gson createGson() {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z").create();
    }

    private void initializeEventListeners() {
        technicianProxy.subscribe(SIGNAL_R_SERVICE_CALL_UPDATED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (SignalREventListener listener : eventListeners) {
                            listener.serviceCallUpdated(obj[0]);
                        }
                    }
                });
            }
        });

        technicianProxy.subscribe(SIGNAL_R_NEW_SERVICE_CALL).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    public void run() {
                        for (SignalREventListener listener : eventListeners) {
                            listener.newServiceCall(obj[0]);
                        }
                    }
                });
            }
        });

        technicianProxy.subscribe(SIGNAL_R_STATUS_CHANGED).addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(final JsonElement[] obj) throws Exception {
                handler.post(new Runnable() {
                    public void run() {
                        for (SignalREventListener listener : eventListeners) {
                            listener.userStatusChanged(obj[0].getAsString());
                        }
                    }
                });
            }
        });
    }

    private void retryConnect() {
        int seconds = 3;
        Log.d(TAG, String.format("Waiting %s seconds", seconds));


        handler.removeCallbacks(connectSignalRRunnable);
        Log.d(TAG, String.format("Time %s", new Date().toString()));
        handler.postDelayed(connectSignalRRunnable, seconds * 1000);

    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    @Override
    public void addEventListener(SignalREventListener eventListener) {
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(SignalREventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    @Override
    public void addMessengerEventListener(MessengerEventListener messengerEventListener) {
        messengerEventListeners.add(messengerEventListener);
    }

    @Override
    public void removeMessengerEventListener(MessengerEventListener messengerEventListener) {
        messengerEventListeners.remove(messengerEventListener);
    }

    @Override
    public void updateServiceCalls() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (SignalREventListener listener : eventListeners) {
                    listener.serviceCallsUpdated();
                }
            }
        });
    }

    @Override
    public void updateConversation(final Conversation conversation) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (MessengerEventListener listener : messengerEventListeners) {
                    listener.updateConversation(conversation);
                }
            }
        });
    }

    @Override
    public HubProxy getMessagingProxy() {
        return messagingProxy;
    }
}
