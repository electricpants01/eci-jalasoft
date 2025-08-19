package eci.technician.helpers.ErrorHelper

import android.os.Handler
import android.os.Looper
import eci.technician.helpers.api.retroapi.ErrorType

class ErrorHandler {
    public var errorListeners: MutableList<MTErrorListener> = mutableListOf()

    companion object {
        @Volatile
        private var mInstance: ErrorHandler? = null
        public fun get(): ErrorHandler =
            mInstance ?: synchronized(this) {
                val newInstance = mInstance ?: ErrorHandler().also { mInstance = it }
                newInstance
            }
    }

    public fun addErrorListener(errorListener: MTErrorListener) {
        errorListeners.add(errorListener)
    }

    public fun removeListener(errorListener: MTErrorListener) {
        errorListeners.remove(errorListener)
    }

    public fun notifyListeners(error:Pair<ErrorType, String?>?, requestType: String, callId: Int, data:String) {
        Handler(Looper.getMainLooper()).post(Runnable {
            errorListeners.forEach {
                it.onListenError(error, requestType, callId, data)
            }
        })
    }
}


