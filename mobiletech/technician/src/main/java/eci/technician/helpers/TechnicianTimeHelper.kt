package eci.technician.helpers

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import eci.technician.R
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.repository.TechnicianTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TechnicianTimeHelper(private val context: Context) {

    @Synchronized
    fun clockIn(
        changeStatusModel: ChangeStatusModel,
        lifecycleScope: Lifecycle,
        onSuccess: () -> Unit,
        onError: (title: String, message: String, pair:Pair<ErrorType, String?>?) -> Unit
    ) =
        lifecycleScope.coroutineScope.launch {
            TechnicianTimeRepository.clockInUser(changeStatusModel).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        withContext(Dispatchers.IO) {
                            TechnicianTimeRepository.fetchClockInUpdateActions {
                                onSuccess.invoke()
                            }
                        }
                    }
                    is Resource.Error -> {
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION,
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            ErrorType.NOT_SUCCESSFUL,
                            ErrorType.BACKEND_ERROR,
                            ErrorType.HTTP_EXCEPTION,
                            ErrorType.SOMETHING_WENT_WRONG -> {
                                onError(
                                    context.getString(R.string.somethingWentWrong),
                                    pairError.second ?: "",
                                    pairError
                                )
                            }
                        }
                    }
                    is Resource.Loading -> { /* Not needed */
                    }
                }
            }
        }

    @Synchronized
    fun clockOut(
        changeStatusModel: ChangeStatusModel,
        lifecycleScope: Lifecycle,
        onSuccess: () -> Unit,
        onError: (title: String, message: String, pair:Pair<ErrorType,String?>?) -> Unit
    ) =
        lifecycleScope.coroutineScope.launch {
            TechnicianTimeRepository.clockOutUser(changeStatusModel).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        TechnicianTimeRepository.fetchClockOutActions {}
                        onSuccess.invoke()
                    }
                    is Resource.Error -> {
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION,
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            ErrorType.NOT_SUCCESSFUL,
                            ErrorType.BACKEND_ERROR,
                            ErrorType.HTTP_EXCEPTION,
                            ErrorType.SOMETHING_WENT_WRONG -> {
                                onError(
                                    context.getString(R.string.somethingWentWrong),
                                    pairError.second ?: "",
                                    pairError
                                )
                            }
                        }
                    }
                    is Resource.Loading -> { /* Not needed */
                    }
                }
            }
        }
}