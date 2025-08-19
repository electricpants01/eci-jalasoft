package eci.technician.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.signalr.messenger.Message
import eci.signalr.messenger.MessageStatusChangeModel
import eci.technician.helpers.chat.MessageHelper
import eci.technician.helpers.notification.NotificationManager
import eci.technician.repository.NotificationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MessengerViewModel : ViewModel() {

    var isInBackground = false

    var isinback: LiveData<Boolean> = MutableLiveData<Boolean>(false)

    companion object {
        const val TEN_SECONDS = 10 * 1000L
    }

    init {


    }

    fun handleNotificationIfExists(conversationId: String, context: Context) {
        viewModelScope.launch {
            NotificationsRepository.emptyMessagesByConversationId(conversationId)
            withContext(Dispatchers.Main) {
                NotificationManager.deleteNotificationByConversationId(
                    conversationId = conversationId,
                    context = context
                )
            }
        }
    }


    fun startCheckingMessages() {
        viewModelScope.launch {
            while (true) {
                MessageHelper.updateMessages()
                delay(TEN_SECONDS)
            }
        }
    }

    fun sendSeenStatusForMessage(message: Message){
        viewModelScope.launch {
            MessageHelper.sendSeenStatus(message.messageId)
        }
    }

    fun sendOneConversationUpdate(conversationId: String) {
        viewModelScope.launch {
            MessageHelper.sendConversationUpdate(conversationId)
        }
    }

    fun sendSeenMessagesReadUpdatePeriodically(conversationId: String) {
        viewModelScope.launch {
            while (!isInBackground) {
                MessageHelper.setMessagesReadSendUpdateConversation(conversationId)
                delay(TEN_SECONDS)
            }
        }
    }


    fun sendMessagesReadUpdate(conversationId: String) {
        viewModelScope.launch {
            MessageHelper.setMessagesReadSendUpdateConversation(conversationId)
        }
    }

    fun saveMessageReceived(conversationId: String?, message: Message) {
        viewModelScope.launch {
            MessageHelper.saveMessageReceived(conversationId, message)
        }
    }

    fun updateStatusChangedForMessage(
        it: String,
        messageStatusChangeModel: MessageStatusChangeModel
    ) {
        viewModelScope.launch {
            MessageHelper.updateStatusChangedForMessage(it, messageStatusChangeModel)
        }
    }

    fun updateConversations(date: Date) {
        viewModelScope.launch {
            MessageHelper.updateConversationFromServer(date)
        }
    }

}