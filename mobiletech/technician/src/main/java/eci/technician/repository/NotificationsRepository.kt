package eci.technician.repository

import android.util.Log
import eci.technician.models.notification.NotificationMessage
import io.realm.Realm
import io.realm.RealmList
import kotlin.random.Random

object NotificationsRepository {
    const val TAG = "NotificationsRepository"
    const val EXCEPTION = "Exception"

    suspend fun saveMessage(message: String, conversationId: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val notification = realm
                .where(NotificationMessage::class.java)
                .equalTo(NotificationMessage.CONVERSATION_ID, conversationId)
                .findFirst()
            notification?.let {
                realm.executeTransaction {
                    notification.conversationMessages.add(message)
                }
            } ?: kotlin.run {
                realm.executeTransaction {
                    val notificationId = Random.nextInt(9999 - 1000) + 1000
                    val newNotification =
                        NotificationMessage(conversationId, RealmList(message), notificationId)
                    realm.insertOrUpdate(newNotification)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun getNotificationByConversationId(conversationId: String): NotificationMessage? {
        var notificationMessage: NotificationMessage? = null
        val realm = Realm.getDefaultInstance()
        try {
            val notification = realm
                .where(NotificationMessage::class.java)
                .equalTo(NotificationMessage.CONVERSATION_ID, conversationId)
                .findFirst()

            notification?.let {
                notificationMessage = realm.copyFromRealm(it)
            } ?: kotlin.run {
                notificationMessage = null
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return notificationMessage
    }

    suspend fun emptyMessagesByConversationId(conversationId: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val notification = realm
                .where(NotificationMessage::class.java)
                .equalTo(NotificationMessage.CONVERSATION_ID, conversationId)
                .findFirst()

            notification?.let {
                realm.executeTransaction {
                    notification.conversationMessages = RealmList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }
}