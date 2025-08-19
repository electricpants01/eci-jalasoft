package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.order.Part
import eci.technician.models.order.RequestPartTransferItem
import eci.technician.repository.PartsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class NewPartRequestViewModel : ViewModel() {

    var itemName: String? = ""
    var itemDescription: String? = ""
    private var _loading: MutableLiveData<Boolean> = MutableLiveData()
    val loading: LiveData<Boolean> = _loading

    private var _networkError: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val networkError: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _networkError


    private var _onTechWarehousePartsSuccess: MutableLiveData<ViewModelUtils.Event<List<Part>>> =
        MutableLiveData()
    val onTechWarehousePartsSuccess: LiveData<ViewModelUtils.Event<List<Part>>> =
        _onTechWarehousePartsSuccess

    private var _onPartsInTechWarehousesSuccess: MutableLiveData<ViewModelUtils.Event<List<RequestPartTransferItem>>> =
        MutableLiveData()
    val onPartsInTechWarehousesSuccess: LiveData<ViewModelUtils.Event<List<RequestPartTransferItem>>> =
        _onPartsInTechWarehousesSuccess

    private var _onPutTransferOrderSuccess:MutableLiveData<ViewModelUtils.Event<Boolean>> = MutableLiveData()
    val onPutTransferOrderSuccess:LiveData<ViewModelUtils.Event<Boolean>> = _onPutTransferOrderSuccess

    fun getOnlyTechWarehousePartsForFieldTransfer(partCode: String) = viewModelScope.launch {
        PartsRepository.getOnlyTechWarehousePartsForFieldTransfer(partCode).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loading.value = false
                    val list = value.data ?: listOf()
                    _onTechWarehousePartsSuccess.value = ViewModelUtils.Event(list)

                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                    _networkError.value = ViewModelUtils.Event(pair)
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }

    fun getPartsInTechnicianWarehouses(itemId: String) = viewModelScope.launch {
        PartsRepository.getPartsInTechnicianWarehouses(itemId).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loading.value = false
                    val list = value.data ?: listOf()
                    _onPartsInTechWarehousesSuccess.value = ViewModelUtils.Event(list)

                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                    _networkError.value = ViewModelUtils.Event(pair)
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }

    fun putTransferOrder(map: HashMap<String,Any>) = viewModelScope.launch {
        PartsRepository.putTransferOrder(map).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loading.value = false
                    _onPutTransferOrderSuccess.value = ViewModelUtils.Event(true)

                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                    _networkError.value = ViewModelUtils.Event(pair)
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }
}