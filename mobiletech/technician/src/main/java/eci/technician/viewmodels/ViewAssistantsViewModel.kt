package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.ProcessingResult
import eci.technician.models.TechnicianItem
import eci.technician.models.order.ServiceCallLabor
import eci.technician.repository.AssistantRepository
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.RealmLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewAssistantsViewModel : ViewModel() {

    private val _techniciansList: MutableLiveData<List<TechnicianItem>> = MutableLiveData()

    val technicianList: LiveData<List<TechnicianItem>> = _techniciansList

    private var _loadingAssistants: MutableLiveData<Boolean> = MutableLiveData()
    val loadingAssistants: LiveData<Boolean> = _loadingAssistants

    private var _networkError: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val networkError: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _networkError


    fun fetchAllTechnicians() = viewModelScope.launch {
        AssistantRepository.getAllTechnicians().collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loadingAssistants.value = false
                    val responseBody = value.data?.result ?: ""
                    fillTechniciansList(responseBody)
                }
                is Resource.Error -> {
                    _loadingAssistants.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _networkError.value = ViewModelUtils.Event(pairError)
                }
                is Resource.Loading -> {
                    _loadingAssistants.value = true
                }
            }
        }
    }


    fun getAllLaborsForServiceCall(serviceCallId: Int): RealmLiveData<ServiceCallLabor> {
        return DatabaseRepository.getInstance().getAllLaborsForServiceCall(serviceCallId)
    }

    private suspend fun fillTechniciansList(responseBody: String) =
        viewModelScope.launch(Dispatchers.Default) {
            val list = AssistantRepository.createTechniciansListFromResponse(responseBody)
            withContext(Dispatchers.Main) {
                _techniciansList.value = list
            }
        }
}