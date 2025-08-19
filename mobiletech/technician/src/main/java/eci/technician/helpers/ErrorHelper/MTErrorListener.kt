package eci.technician.helpers.ErrorHelper

import eci.technician.helpers.api.retroapi.ErrorType

interface MTErrorListener {
    fun onListenError(
        error: Pair<ErrorType, String?>?,
        requestType: String,
        callId: Int,
        data: String
    )
}