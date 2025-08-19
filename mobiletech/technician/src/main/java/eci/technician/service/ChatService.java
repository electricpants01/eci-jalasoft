package eci.technician.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import eci.signalr.ConnectionListener;
import eci.signalr.messenger.Conversation;
import eci.signalr.messenger.ConversationUser;
import eci.signalr.messenger.Message;
import eci.signalr.messenger.MessageStatusChangeModel;
import eci.signalr.messenger.MessengerEventListener;
import eci.signalr.messenger.TypingModel;
import eci.signalr.messenger.UserStatusChangeModel;
import eci.technician.MainApplication;
import eci.technician.helpers.AppAuth;
import eci.technician.helpers.chat.MessageHelper;
import eci.technician.tools.Constants;
import io.realm.Realm;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.SignalRFuture;

public class ChatService extends Service implements MessengerEventListener, ConnectionListener {

    private static final String TAG = "TechnicianChatService";
    private static final String EXCEPTION = "Exception logger";
    public static final String REFRESH_SERVICE_TAG = "refresh_messages";

    private final Object conversationsUpdater = new Object();

    private Runnable updateChatRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (conversationsUpdater) {
                Log.d(TAG, "run: Conversation update");
                try {
                    updateConversations();
                } catch (Exception e) {
                    Log.e(TAG, EXCEPTION, e);
                }
                Log.d(TAG, "run: End update");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if (AppAuth.getInstance().getChatEnabled()) {
                if (MainApplication.getConnection() != null) {
                    MainApplication.getConnection().addMessengerEventListener(this);
                }
            }
            if (MainApplication.getConnection() != null) {
                MainApplication.getConnection().addConnectionListener(this);
            }
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION, e);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Stopped");
        if (AppAuth.getInstance().getChatEnabled()) {
            if (MainApplication.getConnection() != null) {
                MainApplication.getConnection().removeMessengerEventListener(this);
            }
        }
        if (MainApplication.getConnection() != null) {
            MainApplication.getConnection().removeConnectionListener(this);
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(Constants.MESSAGES_NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        load();
        return START_STICKY;
    }

    @Override
    public void updateAll() {
    }

    @Override
    public void messageReceived(Message message) {
        MessageHelper.INSTANCE.saveMessageReceived(null, message, new Continuation<Unit>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }

    @Override
    public void messageStatusChanged(final MessageStatusChangeModel messageStatusChangeModel) {
        MessageHelper.INSTANCE.updateStatusChangedForMessage(null, messageStatusChangeModel, new Continuation<Unit>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }

    @Override
    public void typingMessage(TypingModel typingModel) {
    }

    @Override
    public void userStatusChanged(UserStatusChangeModel userStatusChangeModel) {
    }

    @Override
    public void conversationUsersAdded(Conversation conversation) {
    }

    @Override
    public void conversationUsersRemoved(Conversation conversation) {
    }

    @Override
    public void conversationNameUpdated(Conversation conversation) {
        Realm realm = Realm.getDefaultInstance();
        try {
            Conversation dbConversation = realm.where(Conversation.class).equalTo("conversationId", conversation.getConversationId()).findFirst();
            if (dbConversation != null) {
                dbConversation.setConversationName(conversation.getConversationName());
            }
        } catch (Exception e) {

        } finally {
            realm.close();
        }
    }

    @Override
    public void hideConversation(Conversation conversation) {
    }

    @Override
    public void unHideConversation(Conversation conversation) {
    }

    @Override
    public void updateConversation(Conversation conversation) {
        //sendSeenStatus();
    }

    @Override
    public void onConversationPressed(Conversation item) {

    }

    @Override
    public void updateStatusMessage() {

    }

    private void saveOrUpdateMessage(final Message message, final boolean event) {
        Realm realm = Realm.getDefaultInstance();
        try {
            if (realm.where(Conversation.class).equalTo("conversationId", message.getConversationId()).findFirst() == null) {
                getConversationFromServer(message.getConversationId());
            }
            message.setMyMessage(message.getSenderId().equals(AppAuth.getInstance().getTechnicianUser().getId()));
            realm.executeTransaction(realm1 ->
                    realm1.copyToRealmOrUpdate(message));

            final Conversation conversation = realm.where(Conversation.class).equalTo("conversationId", message.getConversationId()).findFirst();
            if (conversation != null) {
                realm.executeTransaction(realm12 -> {
                    conversation.setLastMessage(message.getMessage());
                    conversation.setUpdateTime(message.getMessageTime());
                    if (!message.isMyMessage() && event) {
                        conversation.setUnreadMessageCount(conversation.getUnreadMessageCount() + 1);
                    }
                });
            }
        } catch (Exception e) {

        } finally {
            realm.close();
        }

    }

    private void getConversationFromServer(String conversationId) {
        SignalRFuture<Conversation> future = null;
        if (MainApplication.getConnection() != null) {
            future = MainApplication.getConnection().getMessagingProxy().invoke(Conversation.class, "GetConversationInfo", conversationId);
        }

        if (future != null) {
            future.done(new Action<Conversation>() {
                @Override
                public void run(Conversation conversation) throws Exception {
                    saveOrUpdateConversation(conversation);
                    MainApplication.getConnection().updateConversation(conversation);
                }
            });
        }
    }

    private void saveOrUpdateConversation(final Conversation conversation) {
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.executeTransaction(realm1 -> {
                if (conversation.getUsers() != null && !conversation.getUsers().isEmpty()) {
                    for (ConversationUser conversationUser : conversation.getUsers()) {
                        if (!conversationUser.getChatIdent().equals(AppAuth.getInstance().getTechnicianUser().getId())) {
                            conversation.setUserName(conversationUser.getChatName());
                            conversation.setUserIdent(conversationUser.getChatIdent());
                            break;
                        }
                    }
                }
                realm1.copyToRealmOrUpdate(conversation);
            });
        } catch (Exception e) {

        } finally {
            realm.close();
        }
    }

    public void updateConversationsFromServer(final Date updateTime) {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    SignalRFuture<Conversation[]> future = null;
                    if (MainApplication.getConnection() != null) {
                        future = MainApplication.getConnection().getMessagingProxy()
                                .invoke(Conversation[].class, "GetConversationUpdate", updateTime);
                    }

                    Conversation[] conversations = new Conversation[0];
                    if (future != null) {
                        conversations = future.get();
                    }
                    for (Conversation conversation : conversations) {
                        saveOrUpdateConversation(conversation);
                    }
                    updateMessages();
                } catch (Exception e) {
                    Log.e(TAG, EXCEPTION, e);
                }
            }
        };
        new Thread(runnable).start();
    }

    private synchronized void updateMessages() throws ExecutionException, InterruptedException {
        MessageHelper.INSTANCE.updateMessages(new Continuation<Unit>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }

    private void updateConversations() {
        Realm realm = Realm.getDefaultInstance();
        try {
            Date updateTime = realm.where(Conversation.class).maximumDate("updateTime");
            if (updateTime != null) {
                updateConversationsFromServer(updateTime);
            } else {
                Date ancientDate = new Date();
                ancientDate.setTime(1);
                updateConversationsFromServer(ancientDate);
            }
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION, e);
        } finally {
            realm.close();
        }
    }

    private void load() {
        if (AppAuth.getInstance().getChatEnabled()) {
            new Thread(updateChatRunnable).start();
        }
    }

    @Override
    public void connected() {
        load();
    }

    @Override
    public void disconnected() {

    }

    @Override
    public void stateChanged(ConnectionState oldState, ConnectionState newState) {

    }

    @Override
    public void reconnecting() {

    }

}
