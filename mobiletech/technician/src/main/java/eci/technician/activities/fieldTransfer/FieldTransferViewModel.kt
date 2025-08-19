package eci.technician.activities.fieldTransfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.viewmodels.ViewModelUtils.Event
import eci.technician.models.field_transfer.PartRequestTransfer
import eci.technician.models.field_transfer.PostCancelOrderModel
import eci.technician.repository.PartsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FieldTransferViewModel : ViewModel() {
    private val _fieldTransferList = MutableLiveData<List<PartRequestTransfer>>()
    val fieldTransferList: LiveData<List<PartRequestTransfer>> = _fieldTransferList
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    private val _swipeLoading: MutableLiveData<Boolean> = MutableLiveData()
    val swipeLoading: LiveData<Boolean> = _swipeLoading
    val networkError = MutableLiveData<Event<Pair<ErrorType, String?>>>()
    var hasBeenOpenedFromNotification = false

    init {
        fetchMyPartRequest()
    }

    fun fetchMyPartRequest() = viewModelScope.launch {
        PartsRepository.getMyPartRequests().collect { value ->
            when (value) {
                is Resource.Success -> {
                    _fieldTransferList.value = value.data ?: listOf()
                    _isEmpty.value = value.data?.isEmpty() ?: true
                    fetchPartRequestFromMe()
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _swipeLoading.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    networkError.value = Event(pairError)
                }
                is Resource.Loading -> {
                    _isLoading.value = true
                    _swipeLoading.value = true
                }
            }

        }
    }

    private fun fetchPartRequestFromMe() = viewModelScope.launch {
        PartsRepository.getPartRequestFromMe().collect { value ->
            when (value) {
                is Resource.Success -> {
                    val list = _fieldTransferList.value?.toMutableList() ?: mutableListOf()
                    value.data?.forEach { list.add(it) }
                    _fieldTransferList.value = list
                    _swipeLoading.value = false
                    _isEmpty.value = list.isEmpty()
                    _isLoading.value = false
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    networkError.value = Event(pairError)
                }
                is Resource.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }

    fun cancelTransferOrder(item: PartRequestTransfer) = viewModelScope.launch {
        val cancelModel =
            PostCancelOrderModel(PostCancelOrderModel.ACTION_TYPE_DELETE, item.toID ?: 0)
        cancelRejectTransferOrder(cancelModel)
    }

    fun rejectTransferOrder(item: PartRequestTransfer) = viewModelScope.launch {
        val rejectModel =
            PostCancelOrderModel(PostCancelOrderModel.ACTION_TYPE_REJECT, item.toID ?: 0)
        cancelRejectTransferOrder(rejectModel)
    }

    private fun cancelRejectTransferOrder(postModel: PostCancelOrderModel) = viewModelScope.launch {
        PartsRepository.cancelTransferOrder(postModel).collect { value ->
            when (value) {
                is Resource.Success -> {
                    fetchMyPartRequest()
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    networkError.value = Event(pairError)
                }
                is Resource.Loading -> {
                    _isLoading.value = true
                }
            }
        }

    }

    fun acceptTransferOrder(item: PartRequestTransfer) = viewModelScope.launch {
        PartsRepository.postTransferRequest(item.toID ?: 0).collect { value ->
            when (value) {
                is Resource.Success -> {
                    fetchMyPartRequest()
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    networkError.value = Event(pairError)
                }
                is Resource.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }


}