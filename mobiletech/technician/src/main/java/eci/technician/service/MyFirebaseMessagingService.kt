package eci.technician.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import eci.technician.helpers.notification.NotificationManager


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.d("firebaseToken", "the TOKEN :  $p0")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        var notificationType = ""
        if (!data.isNullOrEmpty()) {
            notificationType = data[NotificationManager.NOTIFICATION_TYPE_KEY] ?: ""
        }

        when (notificationType) {
            NotificationManager.NOTIFICATION_TYPE_FIELD_REQUEST_TRANSFER,
            NotificationManager.NOTIFICATION_TYPE_FIELD_DELETE_TRANSFER,
            NotificationManager.NOTIFICATION_TYPE_FIELD_REJECT_TRANSFER,
            NotificationManager.NOTIFICATION_TYPE_FIELD_ACCEPT_TRANSFER-> {
                NotificationManager.showFieldTransferNotification(applicationContext, remoteMessage)
            }
            NotificationManager.NOTIFICATION_TYPE_CHAT_MESSAGE -> {
                NotificationManager.showChatNotification(applicationContext, remoteMessage)
            }
            else -> {
                /**
                 * will open the app with the main activity
                 */
                NotificationManager.showNewServiceCallNotification(
                    applicationContext,
                    remoteMessage
                )
            }
        }
    }
}