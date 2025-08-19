package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.RequestPart
import eci.technician.repository.PartsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RequestPartsViewModel : ViewModel() {


    private var _loading: MutableLiveData<Boolean> = MutableLiveData()
    val loading: LiveData<Boolean> = _loading

    private var _networkError: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val networkError: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _networkError

    private var _successRequestParts: MutableLiveData<ViewModelUtils.Event<Boolean>> =
        MutableLiveData()
    val successRequestParts: LiveData<ViewModelUtils.Event<Boolean>> = _successRequestParts

    fun requestParts(list: List<RequestPart>?) = viewModelScope.launch {
        val listOfParts = list?.toMutableList() ?: mutableListOf<RequestPart>()
        PartsRepository.requestPartsMaterial(listOfParts).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loading.value = false
                    _successRequestParts.value = ViewModelUtils.Event(true)

                }
                is Resource.Error -> {
                    _loading.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _networkError.value = ViewModelUtils.Event(pairError)

                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }


}