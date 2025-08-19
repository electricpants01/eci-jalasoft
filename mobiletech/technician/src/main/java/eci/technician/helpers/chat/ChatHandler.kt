package eci.technician.helpers.chat

import eci.technician.interfaces.UpdateChatIconListener

object ChatHandler {
    var listenerI: UpdateChatIconListener? = null

    fun setListener(listener: UpdateChatIconListener)
    {
        this.listenerI = listener
    }

    fun getListener(): UpdateChatIconListener?
    {
        return listenerI
    }
}