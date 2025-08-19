package eci.technician.activities.allparts

import androidx.lifecycle.*
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.ProcessingResult
import eci.technician.repository.PartsRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.viewmodels.ViewModelUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllPartsViewModel : ViewModel() {

    var currentCallId = 0
    var onHoldCodeIdForNeededParts = 0
    var allowChangePartStatus = false
    var isAssist = false
    var customerWarehouseId: Int = 0
    var currentTechWarehouseId: Int = AppAuth.getInstance().technicianUser.warehouseId

    private val _serviceCallStatus: MutableLiveData<ServiceOrderRepository.ServiceOrderStatus> =
        MutableLiveData(ServiceOrderRepository.ServiceOrderStatus.PENDING)
    val serviceCallStatus: LiveData<ServiceOrderRepository.ServiceOrderStatus> = _serviceCallStatus

    private val _isAddingNeededPartsForOnHold: MutableLiveData<Boolean> = MutableLiveData(false)
    val isAddingNeededPartsForOnHold: LiveData<Boolean> = _isAddingNeededPartsForOnHold

    private val _currentStep: MutableLiveData<Int> = MutableLiveData()
    val currentStep: LiveData<Int> = _currentStep

    private var _swipeUsedParts: MutableLiveData<Boolean> = MutableLiveData()
    val swipeUsedParts: LiveData<Boolean> = _swipeUsedParts

    private var _networkError: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val networkError: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _networkError

    fun updateServiceOrderStatus(callNumberId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = ServiceOrderRepository.getServiceOrderStatusByCallNumberId(callNumberId)
            withContext(Dispatchers.Main) {
                _serviceCallStatus.value = status
            }
        }
    }

    fun updateCurrentStep(currentStep: Int) {
        _currentStep.value = currentStep
    }

    fun updateIsAddingPartsFroOnHold(isAdding: Boolean) {
        _isAddingNeededPartsForOnHold.value = isAdding
    }

    fun updatePartsByCallId() {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentCallId != 0) {
                PartsRepository.getUsedPartsByCallIdFromServer(currentCallId).collect { }
            }
        }
    }


    fun fetchUsedPartsByCallIdFlow() = viewModelScope.launch {
        if (currentCallId == 0) return@launch
        PartsRepository.getUsedPartsByCallIdFromServer(currentCallId).collect { value ->
            when(value){
                is Resource.Success -> {
                    _swipeUsedParts.value = false
                }
                is Resource.Error -> {
                    _swipeUsedParts.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _networkError.value = ViewModelUtils.Event(pairError)
                }
                is Resource.Loading -> {
                    _swipeUsedParts.value = true

                }
            }
        }
    }

}