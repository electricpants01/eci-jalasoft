package eci.technician.viewmodels

import androidx.lifecycle.*
import eci.technician.R
import eci.technician.activities.OrderDetailActivity
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class OrderDetailViewModel : ViewModel() {

    private val localServiceOrder = MutableLiveData<ServiceOrder>()
    var hasConnection = false

    var serviceOrder: LiveData<ServiceOrder> = localServiceOrder
    val currentTechWarehouseId: Int = AppAuth.getInstance().technicianUser.warehouseId
    var customerWarehouseId: Int = 0

    private var _isLoadingFetchOneServiceCall: MutableLiveData<Boolean> = MutableLiveData()
    val isLoadingFetchOneServiceCall: LiveData<Boolean> = _isLoadingFetchOneServiceCall

    val toastMessage = MutableLiveData<ViewModelUtils.Event<Int>>()
    val networkError = MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>>()
    val verificationError = MutableLiveData<ViewModelUtils.Event<String>>()

    /**
     * first: showForAction
     * second:ShowFotIncomplete
     * third:ShowForFetchOneServiceCall
     */
    private var _progressBarForButtons: MutableLiveData<Triple<Boolean, Boolean, Boolean>> =
        MutableLiveData(
            Triple(first = false, second = false, third = false)
        )
    val progressBarForButtons: LiveData<Triple<Boolean, Boolean, Boolean>> = _progressBarForButtons


    fun getOrderViewLiveData(callNumberId: Int): RealmLiveData<ServiceOrder> {
        return DatabaseRepository.getInstance().getServiceOrderLiveDataByNumberId(callNumberId)
    }

    fun updatePartDeletable(callId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = ServiceOrderRepository.getServiceOrderStatusByCallNumberId(callId)
            if (status == ServiceOrderRepository.ServiceOrderStatus.ON_HOLD) {
                PartsRepository.updatePartsDeletableByCallId(callId, isDeletable = false)
            }
        }
    }

    fun fetchOneServiceCall(callId: Int) {
        viewModelScope.launch {
            ServiceOrderRepository.getOneServiceCallByIdAndSave(callId).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        _isLoadingFetchOneServiceCall.value = false
                        updateProgressForFetchServiceCall(false)
                        value.data?.let {
                            verifyOneServiceCallResponse(callId, it)
                        }
                    }
                    is Resource.Error -> {
                        updateProgressForFetchServiceCall(false)
                        _isLoadingFetchOneServiceCall.value = false
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                                toastMessage.value = ViewModelUtils.Event(R.string.timeout_message)
                            }
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            -> {
                                // do nothing because we support offline

                            }
                            ErrorType.NOT_SUCCESSFUL,
                            ErrorType.BACKEND_ERROR,
                            ErrorType.HTTP_EXCEPTION,
                            ErrorType.SOMETHING_WENT_WRONG -> {
                                networkError.value = ViewModelUtils.Event(pairError)
                                toastMessage.value =
                                    ViewModelUtils.Event(R.string.somethingWentWrong)
                            }
                        }

                    }
                    is Resource.Loading -> {
                        updateProgressForFetchServiceCall(true)
                        _isLoadingFetchOneServiceCall.value = true
                    }
                }
            }
        }

    }

    private suspend fun verifyOneServiceCallResponse(callId: Int, list: List<ServiceOrder>) {
        ServiceOrderRepository.verifyOneServiceCallResponse(
            callId,
            list,
            onSuccess = {
                //do nothing, already saved in db
            },
            onReassigned = {
                verificationError.value = ViewModelUtils.Event(OrderDetailActivity.SC_REASSIGNED)
            },
            onUnavailable = {
                verificationError.value = ViewModelUtils.Event(OrderDetailActivity.SC_UNAVAILABLE)
            })
    }


    fun updateProgressForActionPerformed(show: Boolean) {
        val triple = _progressBarForButtons.value
        if (!show && hasConnection){
            viewModelScope.launch {
                delay(500)
                _progressBarForButtons.value = triple?.copy(first = show)
            }
        }else{
            _progressBarForButtons.value = triple?.copy(first = show)
        }
    }

    fun updateProgressForIncompleteInProgress(show: Boolean) {
        val triple = _progressBarForButtons.value
        _progressBarForButtons.value = triple?.copy(second = show)
    }

    private fun updateProgressForFetchServiceCall(show: Boolean) {
        val triple = _progressBarForButtons.value
        _progressBarForButtons.value = triple?.copy(third = show)
    }
}