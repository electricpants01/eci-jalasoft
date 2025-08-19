package eci.technician.activities.requestNeededParts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.R
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.data.UsedPart
import eci.technician.models.order.Part
import eci.technician.repository.PartsRepository
import eci.technician.viewmodels.ViewModelUtils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PartUseViewModel : ViewModel() {
    var searchQuery: String? = null
    var holdCodeId: Int = 0
    var callId: Int = 0

    /**
     * Is requesting a part for REQUEST PARTS option
     */
    var isRequestingPart = false

    private val _swipeLoading: MutableLiveData<Boolean> = MutableLiveData()
    val swipeLoading: LiveData<Boolean> = _swipeLoading

    private val _neededPartsList: MutableLiveData<List<Part>> = MutableLiveData()
    val neededPartsList: LiveData<List<Part>> = _neededPartsList

    val toastMessage = MutableLiveData<Event<Int>>()
    val networkError = MutableLiveData<Event<Pair<ErrorType, String?>>>()

    private val _loading: MutableLiveData<Boolean> = MutableLiveData()
    val loading: LiveData<Boolean> = _loading

    init {
        fetchParts(forceUpdate = false)
        getNeededPartsFromDB(showLoading = true)
    }

    @Synchronized
    fun fetchParts(
        partCode: String = "",
        available: Boolean = false,
        forceUpdate: Boolean = false
    ) = viewModelScope.launch {
        PartsRepository.getAllPartsFromAllWarehouses(partCode, available, forceUpdate)
            .collect { value ->
                when (value) {
                    is Resource.Success -> {
                        delay(500)
                        getNeededPartsFromDB(showLoading = false)
                        _swipeLoading.value = false
                        toastMessage.value = Event(R.string.updated)
                    }
                    is Resource.Error -> {
                        _swipeLoading.value = false
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                                toastMessage.value = Event(R.string.timeout_message)
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
                                networkError.value = Event(pairError)
                                toastMessage.value = Event(R.string.somethingWentWrong)
                            }
                        }
                    }
                    is Resource.Loading -> {
                        _swipeLoading.value = true
                        toastMessage.value =
                            Event(R.string.updating)
                    }
                }
            }


    }

    private fun getNeededPartsFromDB(showLoading: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            if (showLoading) {
                _loading.value = true
            }
            withContext(Dispatchers.IO) {
                val parts = PartsRepository.getAllPartsForNeededParts()
                withContext(Dispatchers.Main) {
                    _neededPartsList.value = parts
                    if (showLoading) {
                        _loading.value = false
                    }
                }
            }
        }
    }


    fun saveNeededPart(
        part: Part,
        quantity: Double,
        description: String,
        shouldEditDescription: Boolean,
        created: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val usedPart = UsedPart.createNeededPartInstance(
                    callId = callId,
                    itemId = part.itemId,
                    partName = part.item ?: "",
                    partDescription = description,
                    quantity = quantity,
                    localUsageStatus = UsedPart.NEEDED_STATUS_CODE,
                    warehouseId = part.warehouseID,
                    warehouseName = part.warehouse ?: "",
                    bindId = 0,
                    holdCodeId = holdCodeId
                )
                PartsRepository.saveNewPart(usedPart)
                withContext(Dispatchers.Main) {
                    created.invoke()
                }
            }
        }
    }
}