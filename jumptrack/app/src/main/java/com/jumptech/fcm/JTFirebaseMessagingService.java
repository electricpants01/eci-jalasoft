package com.jumptech.fcm;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.jumptech.android.util.NotificationHelper;
import com.jumptech.jumppod.Broadcast;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.comms.CommandPrompt;
import com.jumptech.tracklib.data.Command;
import com.jumptech.tracklib.data.Prompt;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.PromptRepository;
import com.jumptech.ui.LauncherActivity;

public class JTFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = JTFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Token: " + token);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.fcm_token_key), token);
        editor.apply();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            if (new TrackPreferences(getApplicationContext()).getLastServerTime() > remoteMessage.getSentTime()) {
                Log.i(TAG, "Notification older than last update");
            } else {
                Log.i(TAG, "Notification valid");
                String operation = remoteMessage.getData().get("operation");
                Log.i(TAG, "Operation = " + operation);
                // Update routes
                if ("1".equals(operation)) {

                    CommandPrompt cmdPrompt = new CommandPrompt();
                    cmdPrompt.command = Command.ROUTE_UPDATE;
                    cmdPrompt.prompt = new Prompt(getString(R.string.routeUpdateOnServer));

                    PromptRepository.prompt(getApplicationContext(), cmdPrompt);

                    showChangeRouteNotification();

                    sendBroadcast(new Intent(Broadcast.ROUTE_SYNC.getAction()));

                } else {
                    Log.i(TAG, "Unknown operation " + operation);
                }
            }
        }
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    public void showChangeRouteNotification(){
        // Create pending intent
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_IMMUTABLE);

        // Generate and show the notification
        Notification notification = NotificationHelper.generateNotification(this, pendingIntent, getString(R.string.routeUpdatedNotificationTitle),
                getString(R.string.routeUpdatedNotificationTitle),getString(R.string.routeUpdateOnServer), R.drawable.icon_white, true);

        NotificationHelper.showNotification(this, notification, NotificationHelper.CHANGE_ROUTE_NOTIFICATION_ID);
    }
}
