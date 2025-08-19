package eci.technician.helpers.api.retroapi

import retrofit2.HttpException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

object ApiUtils {
    suspend fun <T> safeCall(apiCall: suspend () -> Response<T>): Resource<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                body?.let { it ->
                    return Resource.Success(it)
                }
            }
            return Resource.Error(
                "${response.code()}",
                pair = Pair(ErrorType.NOT_SUCCESSFUL, "Unexpected status code: ${response.code()}")
            )
        } catch (throwable: Exception) {
            when (throwable) {
                is ConnectException -> {
                    return Resource.Error(
                        throwable.localizedMessage ?: "",
                        pair = Pair(ErrorType.CONNECTION_EXCEPTION, throwable.localizedMessage)
                    )
                }

                /**
                 * Inheritance from InterruptedIOException -> IOException
                 */
                is SocketTimeoutException -> {
                    return Resource.Error(
                        throwable.localizedMessage ?: "",
                        pair = Pair(
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION,
                            "The connection to the server timed out"
                        )
                    )
                }
                /**
                 * Handles the EXCEPTION before the generic IOException
                 * Inheritance - UnknownServiceException -> IOException
                 * Inheritance - UnknownHostException -> IOException
                 * Inheritance - SSLException -> IOException
                 */
                is UnknownServiceException,
                is UnknownHostException,
                is SSLException -> {
                    return Resource.Error(
                        throwable.localizedMessage ?: "",
                        pair = Pair(ErrorType.SOMETHING_WENT_WRONG, throwable.localizedMessage ?: "")
                    )
                }
                /**
                 * Inheritance - EOFException -> IOException
                 */
                is EOFException -> {
                    return Resource.Error (
                        throwable.localizedMessage ?: "",
                        pair = Pair(ErrorType.SOMETHING_WENT_WRONG, "Unexpected body")
                    )
                }
                is IOException -> {
                    return Resource.Error(
                        throwable.localizedMessage ?: "",
                        pair = Pair(ErrorType.IO_EXCEPTION, "Could not connect to the server")
                    )
                }
                is HttpException -> {
                    return Resource.Error(
                        throwable.localizedMessage ?: "",
                        pair = Pair(ErrorType.HTTP_EXCEPTION, throwable.localizedMessage)
                    )
                }
                else -> {
                    return Resource.Error(
                        throwable.localizedMessage ?: "",
                        pair = Pair(ErrorType.SOMETHING_WENT_WRONG, throwable.localizedMessage)
                    )
                }
            }
        }
    }
}