package eci.technician.helpers.attachmentHelper

import android.os.Handler
import android.os.Looper
import android.util.Log
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.ErrorHelper.MTErrorListener

class AttachmentHandler {
    private var attachmentListeners: MutableList<AttachmentListener> = mutableListOf()

    companion object {
        @Volatile
        private var mInstance: AttachmentHandler? = null
        fun get(): AttachmentHandler =
                mInstance ?: synchronized(this) {
                    val newInstance = mInstance ?: AttachmentHandler().also { mInstance = it }
                    newInstance
                }
    }

    fun addListener(attachmentListener: AttachmentListener) {
        attachmentListeners.add(attachmentListener)
    }

    fun removeListener(attachmentListener: AttachmentListener) {
        attachmentListeners.remove(attachmentListener)
    }

    fun notifyListeners(data: String, requestType:String) {
        Handler(Looper.getMainLooper()).post(Runnable {
            attachmentListeners.forEach {
                it.updateAttachmentList()
            }
        })
    }
}