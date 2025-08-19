package eci.technician.helpers.chat

import android.util.Log
import eci.signalr.messenger.Conversation
import eci.signalr.messenger.Message
import eci.signalr.messenger.MessageStatusChangeModel
import eci.technician.MainApplication
import eci.technician.helpers.AppAuth
import io.realm.Realm
import io.realm.Sort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

object MessageHelper {
    const val TAG = "MessageHelper"
    const val EXCEPTION = "Exception"

    @Synchronized
    suspend fun updateMessages() {
        val realm = Realm.getDefaultInstance()
        try {
            val updateTime = realm.where(Message::class.java).sort("messageTime", Sort.DESCENDING)
                .findFirst()?.messageTime
            updateTime?.let { date ->
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.MONTH, -4)
                val dateMinusTwoDays = calendar.time
                updateMessagesFromServer(dateMinusTwoDays)
            } ?: kotlin.run {
                val ancientDate = Date()
                ancientDate.time = 1
                updateMessagesFromServer(ancientDate)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }finally {
            realm.close()
        }
    }


    private suspend fun updateMessagesFromServer(updateTime: Date) {
        val futureMessages = MainApplication.connection?.messagingProxy?.invoke(
            Array<Message>::class.java,
            "GetMessageHistory",
            updateTime
        )
        futureMessages?.let { futureMessagesNotNull ->
            withContext(Dispatchers.Default) {
                kotlin.runCatching {
                    val messages = futureMessagesNotNull.get()?.toMutableList() ?: mutableListOf()
                    val myId = AppAuth.getInstance().technicianUser.id
                    myId?.let {
                        messages.forEach { saveOrUpdateMessage(it, myId) }
                    }
                }
            }
        }
    }

    suspend fun updateConversationFromServer(date: Date){
        val futureConversations = MainApplication.connection?.messagingProxy?.invoke(
            Array<Conversation>::class.java,
            "GetConversationUpdate",
            date)
        GlobalScope.launch {
            futureConversations?.let { futureConversationsNotNull ->
                withContext(Dispatchers.IO){
                    kotlin.runCatching {
                        val conversations = futureConversationsNotNull.get()
                        for (conversation in conversations) {
                            saveOrUpdateConversation(conversation)
                        }
                    }
                }
            }
        }


    }


    private suspend fun saveOrUpdateConversation(conversation: Conversation){
        withContext(Dispatchers.IO){
            val realm = Realm.getDefaultInstance()
            val myUser = AppAuth.getInstance().technicianUser.id
            try {
                if (conversation.users != null && conversation.users.isNotEmpty()){
                    for (user in conversation.users) {
                        if (user.chatIdent != null && user.chatIdent != myUser){
                            conversation.userName = user.chatName
                            conversation.userIdent = user.chatIdent
                        }
                    }

                }
                realm.executeTransaction {
                    realm.copyToRealmOrUpdate(conversation)
                }
            }catch (e:Exception){
                Log.e(TAG, EXCEPTION, e)
            }finally {
                realm.close()
            }
        }
    }


    private suspend fun saveOrUpdateMessage(message: Message, myId: String) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val messageAlreadyInDB = realm.where(Message::class.java)
                    .equalTo(Message.MESSAGE_ID, message.messageId)
                    .findFirst()

                messageAlreadyInDB?.let { messageInDB ->
                    if (shouldSaveTheNewState(message.status, messageInDB.status)) {
                        realm.executeTransaction { messageInDB.status = message.status }
                    }
                } ?: kotlin.run {

                    message.isMyMessage = message.senderId == myId
                    realm.executeTransaction { realm1 -> realm1.copyToRealmOrUpdate(message) }
                    val conversation = realm.where(Conversation::class.java).equalTo(Conversation.CONVERSATION_ID, message.conversationId).findFirst()
                    conversation?.let {
                        if (shouldSaveMessageInConversation(message.messageTime, it.updateTime)){
                            realm.executeTransaction {
                                conversation.lastMessage = message.message
                                conversation.updateTime = message.messageTime
                            }
                        }
                    }
                    sendDeliveredStatus(message.messageId)
                }

            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
    }

    private fun shouldSaveMessageInConversation(messageTime: Date?, updateTime: Date?): Boolean {
        if (messageTime == null) return false
        if (updateTime == null) return true
        return messageTime.after(updateTime)
    }


    suspend fun setMessagesReadSendUpdateConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val conversation = realm.where(Conversation::class.java)
                    .equalTo("conversationId", conversationId)
                    .findFirst()
                conversation?.let {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.MONTH, -4)
                    val currentTimeMinusTwoDays = calendar.time
                    val messages = realm.where(Message::class.java)
                        .equalTo("conversationId", conversationId)
                        .between(Message.MESSAGE_TIME, currentTimeMinusTwoDays, Date())
                        .equalTo(Message.MY_MESSAGE, false)
                        .findAll()

                    realm.executeTransaction {
                        conversation.unreadMessageCount = 0
                    }
                    MainApplication.connection?.updateConversation(conversation)
                    for (message in messages) {
                        sendSeenStatus(message.messageId)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
    }

    @Synchronized
    suspend fun saveMessageReceived(conversationId: String?, message: Message) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val isMyMessage = message.senderId == AppAuth.getInstance().technicianUser.id
                val isOnCurrentConversation = message.conversationId == conversationId
                message.isMyMessage = isMyMessage
                val conversation = realm.where(Conversation::class.java)
                    .equalTo(Conversation.CONVERSATION_ID, message.conversationId).findFirst()

                val messageFromDB =
                    realm.where(Message::class.java).equalTo(Message.MESSAGE_ID, message.messageId)
                        .findFirst()
                if (messageFromDB == null) {

                    realm.executeTransaction { realm1 ->
                        realm1.copyToRealmOrUpdate(message)
                    }

                    conversation?.let {
                        realm.executeTransaction {
                            conversation.updateTime = message.messageTime
                            conversation.lastMessage = message.message
                        }
                    } ?: kotlin.run {
                        val date = Date()
                        date.time = 1
                        updateConversationFromServer(date)
                    }

                    if (!isMyMessage && isOnCurrentConversation) {
                        sendSeenStatus(message.messageId)
                    }
                    if (!isMyMessage && !isOnCurrentConversation) {
                        sendDeliveredStatus(message.messageId)
                        conversation?.let {
                            realm.executeTransaction {
                                conversation.unreadMessageCount += 1
                            }
                        }
                    } else {
                    }
                } else {

                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
    }


    suspend fun sendSeenStatus(messageId: String) {
        MainApplication.connection?.messagingProxy?.invoke("MessageSeen", messageId)
    }

    suspend fun sendDeliveredStatus(messageId: String) {
        MainApplication.connection?.messagingProxy?.invoke("MessageDelivered", messageId)

    }

    suspend fun sendConversationUpdate(conversationId: String) {
        MainApplication.connection?.messagingProxy?.invoke("conversationId", conversationId)
    }


    suspend fun updateStatusChangedForMessage(
        conversationId: String?,
        messageStatusChangeModel: MessageStatusChangeModel
    ) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val message = realm.where(Message::class.java)
                    .equalTo(Message.MESSAGE_ID, messageStatusChangeModel.messageId).findFirst()
                message?.let {
                    if (shouldSaveTheNewState(
                            newState = messageStatusChangeModel.status,
                            oldState = it.status
                        )
                    ) {
                        realm.executeTransaction {
                            message.status = messageStatusChangeModel.status
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
    }

    private fun shouldSaveTheNewState(newState: String?, oldState: String?): Boolean {
        val newMessageStatus = getMessageStatusFromString(newState)
        val oldMessageStatus = getMessageStatusFromString(oldState)
        when (newMessageStatus) {
            MessageStatus.SENT -> {
                return false
            }
            MessageStatus.DELIVERED -> {
                return when (oldMessageStatus) {
                    MessageStatus.SENT -> true
                    MessageStatus.DELIVERED -> false
                    MessageStatus.SEEN -> false
                }
            }
            MessageStatus.SEEN -> {
                return when (oldMessageStatus) {
                    MessageStatus.SENT -> true
                    MessageStatus.DELIVERED -> true
                    MessageStatus.SEEN -> false
                }
            }
        }
    }

    enum class MessageStatus {
        SENT, DELIVERED, SEEN
    }

    private fun getMessageStatusFromString(status: String?): MessageStatus {
        if (status == null) return MessageStatus.SENT
        return when (status.toLowerCase(Locale.getDefault())) {
            "delivered" -> MessageStatus.DELIVERED
            "seen" -> MessageStatus.SEEN
            else -> MessageStatus.SENT
        }
    }
}