package eci.technician.helpers.api

import eci.technician.R
import eci.technician.helpers.AppAuth

object ApiErrorHelper {
    fun getFormattedError(errorCode: Int, serverError: String): String {
        return when (errorCode) {
            2 -> {
                fixServerError(serverError)
            }
            else -> {
                serverError
            }

        }
    }

    private fun fixServerError(error: String): String {
        var res = error
        if (error.first() == ' ') {
            res = res.removePrefix(" ")
        }
        return res
    }

    fun getUserFriendlyError(errorText: String): String {
        val errorFirsSplit = errorText.split(":")
        return try {
            if (errorFirsSplit.isNotEmpty()) {
                var errorReady = errorFirsSplit.first().replace(" ", "")
                errorReady = errorReady.removePrefix("StatusCode")
                errorReady = errorReady.removePrefix("(")
                errorReady = errorReady.removeSuffix(")")
                getStringForErrorNumber(errorReady.toInt(), errorText)
            } else {
                getStringForErrorNumber(0, errorText)
            }
        } catch (e: Exception) {
            getStringForErrorNumber(0, errorText)
        }
    }

    private fun getStringForErrorNumber(errorNumber: Number, errorText: String): String {
        return when (errorNumber) {
            5002 -> AppAuth.getInstance().context.getString(R.string.message_error_5002)
            else -> errorText
        }
    }
}