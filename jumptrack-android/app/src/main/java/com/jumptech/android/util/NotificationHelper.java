package com.jumptech.android.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import com.jumptech.jumppod.R;

/**
 * This class helps to handle all the notification process and centralize the notification workflow
*/
public class NotificationHelper {

    public static int DATA_SERVICES_NOTIFICATION_ID = 1;

    public static int CHANGE_ROUTE_NOTIFICATION_ID = 2;

    private final static String JUMPTRACK_CHANNEL_ID = "JumpTrackChannel";

    private static NotificationManager getNotificationManager(Context context){
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Generates a notification object
     * @param context app context
     * @param pendingIntent intent to shot when the user tap the notification
     * @param title title
     * @param ticker ticker
     * @param content content
     * @param smallIcon android resource icon
     * @param dismissible determines if the notification can be swiped by the user
    */
    public static Notification generateNotification(Context context, PendingIntent pendingIntent,
                                                    String title, String ticker, String content,
                                                    @DrawableRes int smallIcon, boolean dismissible){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, JUMPTRACK_CHANNEL_ID)
                .setContentTitle(title)
                .setTicker(ticker)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent);
        if(dismissible){
            builder.setAutoCancel(true);
        } else {
            builder.setOngoing(true);
        }

        return builder.build();
    }

    /**
     * If we use an android version bigger than Oreo we need to register a channel to push notifications
    */
    public static void registerChannel(Context context) {
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            mChannel = new NotificationChannel(JUMPTRACK_CHANNEL_ID, context.getString(R.string.notification_gps_channel), importance);
            mChannel.setDescription(context.getString(R.string.notification_gps_description));
            getNotificationManager(context).createNotificationChannel(mChannel);
        }
    }

    /**
     * Show a notification
     * @param context app context
     * @param notificationID notification uniqueID, this helps to dismiss programmatically later
    */
    public static void showNotification(Context context, Notification notification, int notificationID){
        getNotificationManager(context).notify(notificationID, notification);
    }

    /**
     * Dismiss a notification
     * @param context app context
     * @param notificationId if a notificaiton with this id is showing up, it will be dismissed
    */
    public static void dismissNotificationByID(Context context, int notificationId){
        getNotificationManager(context).cancel(notificationId);
    }
}
