package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.order.CancelCode
import eci.technician.models.order.StatusChangeModel
import eci.technician.repository.ServiceOrderRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CancelCodeViewModel : ViewModel() {


    private val cancelCodeSelected: MutableLiveData<CancelCode> = MutableLiveData()
    private val comments: MutableLiveData<String> = MutableLiveData()
    private var _loading:MutableLiveData<Boolean> = MutableLiveData()
    val loading:LiveData<Boolean> = _loading

    val networkError = MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>>()
    val successEvent = MutableLiveData<ViewModelUtils.Event<Boolean>>()

    init {
        val emptyCancelCode = CancelCode()
        emptyCancelCode.code = AppAuth.getInstance().context.getString(R.string.select_cancel_code)
        cancelCodeSelected.value = emptyCancelCode
        comments.value = ""
    }

    fun setCancelCodeSelected(cancelCode: CancelCode) {
        cancelCodeSelected.value = cancelCode
    }

    fun getCancelCodeSelected(): LiveData<CancelCode> {
        return cancelCodeSelected
    }

    fun setComments(newComments: String) {
        comments.value = newComments
    }

    fun getComments(): LiveData<String> {
        return comments
    }

    fun cancelServiceCall(statusChangeModel: StatusChangeModel) = viewModelScope.launch {
        ServiceOrderRepository.cancelServiceCallByIdFromServer(statusChangeModel).collect { value ->
            when(value){
                is Resource.Success ->{
                    _loading.value = false
                    successEvent.value = ViewModelUtils.Event(true)
                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG,"")
                    networkError.value = ViewModelUtils.Event(pair)
                }
                is Resource.Loading -> {

                    _loading.value = true
                }
            }
        }
    }
}