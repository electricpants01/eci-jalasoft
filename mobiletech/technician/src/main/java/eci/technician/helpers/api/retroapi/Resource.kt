package eci.technician.helpers.api.retroapi

import android.content.Context
import android.util.Log
import eci.technician.R
import eci.technician.models.ProcessingResult

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val error: Pair<ErrorType, String?>? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null, pair: Pair<ErrorType, String?>) :
        Resource<T>(data, message, error = pair)

    companion object {
        fun <T> getProcessingResultError(processingResult: ProcessingResult): Error<ProcessingResult> {
            return Error(
                "",
                pair = Pair(
                    ErrorType.BACKEND_ERROR,
                    "${processingResult.formattedErrors} (Server Error)"
                ),
                data = processingResult
            )
        }
        fun getGenericError():Error<ProcessingResult>{
            return Error(
                "",
                pair = Pair(
                    ErrorType.SOMETHING_WENT_WRONG,
                    "Unexpected error"
                ),
                data = null
            )
        }

        fun <T> getGenericErrorType():Error<T>{
            return Error(
                "",
                pair = Pair(
                    ErrorType.SOMETHING_WENT_WRONG,
                    "Unexpected error"
                ),
                data = null
            )
        }

        fun logError(
            error: Pair<ErrorType, String?>?,
            TAG: String,
            EXCEPTION: String,
        ) {
            val pairError = error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
            Log.e(
                TAG,
                EXCEPTION,
                Exception(pairError.first.toString() + " " + pairError.second.toString())
            )
        }


    }

}


