package eci.technician.helpers.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;

import eci.technician.R;

public class NotificationHelper {

    final static int TRACK_USER_NOTIFICATION_ID = 1;

    private final static String MOBILE_TECH_CHANNEL_ID = "MobileTechChannel";

    private static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static int getTrackUserNotificationId(){
        return TRACK_USER_NOTIFICATION_ID;
    }

    /**
     * If we use an android version bigger than Oreo we need to register a channel to push notifications
     */
    public static void registerChannel(Context context) {
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            mChannel = new NotificationChannel(MOBILE_TECH_CHANNEL_ID, context.getString(R.string.notification_gps_channel), importance);
            mChannel.setDescription(context.getString(R.string.notification_gps_description));
            getNotificationManager(context).createNotificationChannel(mChannel);
        }
    }

    /**
     * Generates a notification object
     *
     * @param context       app context
     * @param pendingIntent intent to shot when the user tap the notification
     * @param title         title
     * @param ticker        ticker
     * @param content       content
     * @param smallIcon     android resource icon
     * @param dismissible   determines if the notification can be swiped by the user
     */
    public static Notification generateNotification(Context context, PendingIntent pendingIntent,
                                                    String title, String ticker, String content,
                                                    @DrawableRes int smallIcon, boolean dismissible) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MOBILE_TECH_CHANNEL_ID)
                .setContentTitle(title)
                .setTicker(ticker)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent);
        if (dismissible) {
            builder.setAutoCancel(true);
        } else {
            builder.setOngoing(true);
        }

        return builder.build();
    }
}
