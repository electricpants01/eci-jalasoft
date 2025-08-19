package eci.technician.helpers.ErrorHelper

import android.content.Context
import eci.technician.R
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.models.ProcessingResult
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

object RequestCodeHandler {

    /**
     * This method handles the response and the processing result
     * retrieves the code errors from the response or the error from the proccesingResult object
     */
    fun <T> getMessageErrorFromResponse(response: Response<T>, processingResult: ProcessingResult?): RequestError {
        return if (processingResult == null) {
            RequestError("Something went wrong", "Unexpected status code: ${response.code()}")
        } else {
            RequestError("Something went wrong", "${processingResult.formattedErrors} (Server Error)")
        }

    }

    fun getMessageErrorOnFailure(throwable: Throwable): RequestError {
        return RequestError(null, throwable.localizedMessage ?: "Error")
    }

    fun getMessageErrorOnFailure(throwable: Throwable, context: Context): RequestError {
        return when (throwable) {
            is ConnectException -> {
                RequestError(null, context.getString(R.string.something_went_wrong_connection))
            }
            is SocketTimeoutException -> {
                RequestError(null, context.getString(R.string.timeout_message))
            }
            is IOException -> {
                RequestError(null, context.getString(R.string.something_went_wrong_io_exception))
            }
            else -> {
                RequestError(null, "Error")
            }
        }
    }
}