package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.time_cards.Shift
import eci.technician.models.time_cards.ShiftDetails
import eci.technician.models.time_cards.TimeCardsItemResponse
import eci.technician.repository.TechnicianTimeRepository
import io.realm.Realm
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TimeCardsFragmentViewModel : ViewModel() {
    companion object {
        const val TAG = "TimeCardsFragmentViewModel"
        const val EXCEPTION = "Exception"
    }

    val realm: Realm = Realm.getDefaultInstance()

    private var _shiftList: MutableLiveData<MutableList<Shift>> = MutableLiveData()
    val shiftList: LiveData<MutableList<Shift>> = _shiftList

    private var _networkError: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val networkError: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _networkError

    private var _successPayPeriod: MutableLiveData<ViewModelUtils.Event<MutableList<Shift>>> =
        MutableLiveData()
    val successPayPeriod: LiveData<ViewModelUtils.Event<MutableList<Shift>>> = _successPayPeriod

    private var _successShiftDetail: MutableLiveData<ViewModelUtils.Event<MutableList<ShiftDetails>>> =
        MutableLiveData()
    val successShiftDetails: LiveData<ViewModelUtils.Event<MutableList<ShiftDetails>>> =
        _successShiftDetail

    private var _timeCardList: MutableLiveData<MutableList<TimeCardsItemResponse>> = MutableLiveData()
    val timeCardList: LiveData<MutableList<TimeCardsItemResponse>> = _timeCardList

    init {
        loadShiftFlow()
    }

    private fun loadShiftFlow() = viewModelScope.launch {
        TechnicianTimeRepository.getShiftsOffline().collect {
            _shiftList.value = it.toMutableList()
        }
    }

    fun fetchShiftsList() = viewModelScope.launch {
        TechnicianTimeRepository.fetchShifts()
            .collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        // do nothing, observing from db
                    }

                    is Resource.Loading -> {
                        // do nothing
                    }
                    is Resource.Error -> {
                        //do nothing
                    }
                }
            }
    }


    fun fetchShiftDetailByShiftId(shiftId: String, shouldDelete: Boolean) = viewModelScope.launch {
        TechnicianTimeRepository.getShiftDetailByShiftId(shiftId, shouldDelete).collect { value ->
            when (value) {
                is Resource.Success -> {
                    val response = value.data?.result ?: ""
                    val list = TechnicianTimeRepository.createShiftDetailListFromResponse(response)
                    _successShiftDetail.value = ViewModelUtils.Event(list)

                }
                is Resource.Error -> {
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _networkError.value = ViewModelUtils.Event(pairError)
                }
                is Resource.Loading -> {
                    // do nothing
                }
            }
        }
    }

    fun fetchTimeCards(dateString:String) = viewModelScope.launch {
        TechnicianTimeRepository.getTimeCards(dateString).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    val response = resource.data?.result ?: ""
                    val list = TechnicianTimeRepository.createTimeCardListFromResponse(response).toMutableList()
                    _timeCardList.postValue(list)
                   // TechnicianTimeRepository.createWeekEventListFromTimeCards(list.toMutableList())
                }
                is Resource.Error -> {
                    // TODO
                }
                is Resource.Loading -> {
                    // TODO
                }
            }

        }
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }


}