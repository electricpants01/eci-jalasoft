package eci.technician.helpers.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import eci.technician.MainActivity
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.activities.fieldTransfer.FieldTransferActivity
import eci.technician.repository.NotificationsRepository
import eci.technician.tools.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

object NotificationManager {

    private const val MOBILE_TECH_ATTACHMENT_NOTIFICATION_CHANNEL = "MobileTechAttachmentChannel"
    private const val MOBILE_TECH_NEW_SERVICE_CALL_NOTIFICATION_CHANNEL =
        "MobileTechNewServiceCallChannel"
    private const val MOBILE_TECH_FIELD_TRANSFER_NOTIFICATION_CHANNEL =
        "MobileTechNewFieldTransferChannel"
    private const val MOBILE_TECH_CHAT_NOTIFICATION_CHANNEL = "MobileTechChatChannel"

    /**
     * Notification type that comes within the notification object from firebase
     * The value is set by the backend team
     */
    const val NOTIFICATION_TYPE_KEY = "alertType"
    const val NOTIFICATION_TYPE_NEW_SERVICE_CALL = "new_service"
    const val NOTIFICATION_TYPE_FIELD_REQUEST_TRANSFER = "request_transfer"
    const val NOTIFICATION_TYPE_FIELD_DELETE_TRANSFER = "delete_transfer"
    const val NOTIFICATION_TYPE_FIELD_REJECT_TRANSFER = "reject_transfer"
    const val NOTIFICATION_TYPE_FIELD_ACCEPT_TRANSFER = "accept_transfer"
    const val NOTIFICATION_TYPE_CHAT_MESSAGE = "new_message"
    private const val NOTIFICATION_CONVERSATION_ID_KEY = "conversationid"
    private const val NOTIFICATION_TITLE_KEY = "title"
    private const val NOTIFICATION_BODY_KEY = "body"

    private fun registerNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                MOBILE_TECH_ATTACHMENT_NOTIFICATION_CHANNEL,
                context.getString(R.string.attachment_notification_channel_name),
                importance
            ).apply {
                description =
                    context.getString(R.string.attachment_notification_channel_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerNewServiceCallNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                MOBILE_TECH_NEW_SERVICE_CALL_NOTIFICATION_CHANNEL,
                "New Service call",
                importance
            ).apply {
                description = "New Service Call"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerFieldTransferNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                MOBILE_TECH_FIELD_TRANSFER_NOTIFICATION_CHANNEL,
                "Field Transfer",
                importance
            ).apply {
                description = "Field Transfer"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerChatNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                MOBILE_TECH_CHAT_NOTIFICATION_CHANNEL,
                "Chat",
                importance
            ).apply {
                description = "Chat"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        context: Context,
        title: String,
        description: String,
        channel: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_new_notification)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setAutoCancel(true)
    }

    fun showAttachmentNotification(context: Context, title: String, description: String) {
        val random = Random
        val notificationId = random.nextInt(9999 - 1000) + 1000
        registerNotificationChannel(context)

        val intent =
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.TAPPED, true)
        val pendingIntent =
            PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = createNotification(
            context,
            title,
            description,
            MOBILE_TECH_ATTACHMENT_NOTIFICATION_CHANNEL
        )
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.setContentIntent(pendingIntent).build())
        }
    }

    fun showNewServiceCallNotification(context: Context, remoteMessage: RemoteMessage) {
        val title = remoteMessage.data[NOTIFICATION_TITLE_KEY]
            ?: remoteMessage.notification?.title
            ?: ""
        val description = remoteMessage.data[NOTIFICATION_BODY_KEY]
            ?: remoteMessage.notification?.body
            ?: ""
        val random = Random
        val notificationId = random.nextInt(9999 - 1000) + 1000
        registerNewServiceCallNotificationChannel(context)

        val intent =
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.TAPPED, true)
        val pendingIntent =
            PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = createNotification(
            context,
            title,
            description,
            MOBILE_TECH_NEW_SERVICE_CALL_NOTIFICATION_CHANNEL
        )
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.setContentIntent(pendingIntent).build())
        }
    }

    fun deleteNotificationByConversationId(context: Context, conversationId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val notification =
                NotificationsRepository.getNotificationByConversationId(conversationId)
            notification?.let {
                withContext(Dispatchers.Main) {
                    with(NotificationManagerCompat.from(context)) {
                        cancel(notification.generatedId)
                    }
                }
            }
        }

    }

    fun showFieldTransferNotification(context: Context, remoteMessage: RemoteMessage) {
        val title = remoteMessage.data[NOTIFICATION_TITLE_KEY]
            ?: remoteMessage.notification?.title
            ?: ""
        val description = remoteMessage.data[NOTIFICATION_BODY_KEY]
            ?: remoteMessage.notification?.body
            ?: ""
        val random = Random
        val notificationId = random.nextInt(9999 - 1000) + 1000
        registerFieldTransferNotificationChannel(context)
        val intent = Intent(
            context,
            FieldTransferActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (remoteMessage.notification == null) {
            intent.putExtra(FieldTransferActivity.OPEN_FROM_NOTIFICATION, true)
        }

        val pendingIntent =
            PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = createNotification(
            context,
            title,
            description,
            MOBILE_TECH_FIELD_TRANSFER_NOTIFICATION_CHANNEL
        )
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.setContentIntent(pendingIntent).build())
        }
    }

    fun showChatNotification(context: Context, remoteMessage: RemoteMessage) {

        val title = remoteMessage.data[NOTIFICATION_TITLE_KEY]
            ?: remoteMessage.notification?.title
            ?: ""
        val description = remoteMessage.data[NOTIFICATION_BODY_KEY]
            ?: remoteMessage.notification?.body
            ?: ""
        val conversationId = remoteMessage.data[NOTIFICATION_CONVERSATION_ID_KEY]
            ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            NotificationsRepository.saveMessage(description, conversationId)
            val notification =
                NotificationsRepository.getNotificationByConversationId(conversationId)
            notification?.let {
                withContext(Dispatchers.Main) {
                    registerChatNotificationChannel(context)
                    val intent =
                        Intent(
                            context,
                            MainActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                    intent.putExtra(MainActivity.OPEN_FROM_CHAT_NOTIFICATION, true)
                    val pendingIntent =
                        PendingIntent.getActivity(
                            context,
                            notification.generatedId,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                        )

                    val notificationStyle = NotificationCompat.InboxStyle()
                    notification.getDisplayMessages().forEach {
                        notificationStyle.addLine(it)
                    }

                    if (!MainApplication.isAppOpened) {
                        val newMessageNotification =
                            NotificationCompat.Builder(
                                context,
                                MOBILE_TECH_CHAT_NOTIFICATION_CHANNEL
                            )
                                .setSmallIcon(R.drawable.ic_new_notification)
                                .setContentTitle(title)
                                .setContentText(description)
                                .setStyle(notificationStyle)
                                .setGroup(conversationId)
                                .setGroupSummary(true)
                                .setAutoCancel(true)
                        with(NotificationManagerCompat.from(context)) {
                            notify(
                                notification.generatedId,
                                newMessageNotification.setContentIntent(pendingIntent).build()
                            )
                        }
                    }
                }
            }
        }
    }
}