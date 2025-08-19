package eci.technician.helpers.sortList

import eci.signalr.messenger.ConversationUser

class TechnicianConversationSortByName : Comparator<ConversationUser> {
    override fun compare(o1: ConversationUser?, o2: ConversationUser?): Int {
        return o1!!.chatName.compareTo(o2!!.chatName)
    }
}