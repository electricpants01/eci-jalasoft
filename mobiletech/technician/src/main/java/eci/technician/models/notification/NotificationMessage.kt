package eci.technician.models.notification

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class NotificationMessage(

    @PrimaryKey
    var conversationId: String = "",
    var conversationMessages: RealmList<String> = RealmList(),
    var generatedId: Int = 0
) : RealmObject() {

    fun getDisplayMessages(): List<String> {
        return if (conversationMessages.size > 4) {
            val displayMessages = mutableListOf<String>()
            val messageList = conversationMessages.toMutableList().takeLast(4)
            displayMessages.add("...")
            displayMessages.addAll(messageList)
            displayMessages
        } else {
            conversationMessages
        }
    }

    companion object {
        const val CONVERSATION_ID = "conversationId"
    }
}