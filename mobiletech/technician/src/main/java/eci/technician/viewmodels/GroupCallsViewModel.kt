package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.ProcessingResult
import eci.technician.models.TechnicianGroup
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.RealmLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GroupCallsViewModel : ViewModel() {

    var listOfGroupCallLoaded: MutableList<GroupCallServiceOrder> = mutableListOf()
    var technicianGroupList: MutableList<TechnicianGroup> = mutableListOf()
    var textToSearch = ""
    val assignedIdCalls = mutableListOf<Int>()

    fun getDetailedGroupLiveData(callNumberId: Int): RealmLiveData<GroupCallServiceOrder> {
        return DatabaseRepository.getInstance()
            .getGroupCallsServiceOrderLiveDataByNumberId(callNumberId)
    }

    fun reassignCall(map: HashMap<String, Any>): LiveData<ProcessingResult?> {
        return RetrofitRepository.RetrofitRepositoryObject.getInstance().reassignServiceCall(map)
    }

    fun updateGroupCall(serviceOrder: GroupCallServiceOrder) {
        val position = getItemPositionById(serviceOrder.callNumber_Code)
        this.listOfGroupCallLoaded[position] = serviceOrder
    }

    fun deleteGroupCall(serviceOrder: GroupCallServiceOrder) {
        this.listOfGroupCallLoaded.remove(serviceOrder)
    }

    private fun getItemPositionById(callNumber_Code: String): Int {
        val selectedServiceCall =
            listOfGroupCallLoaded.find { it.callNumber_Code == callNumber_Code }
        return listOfGroupCallLoaded.indexOf(selectedServiceCall)
    }

    fun fetchTechnicianActiveServiceCalls(forceUpdate:Boolean) = viewModelScope.launch(Dispatchers.IO) {
        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .getTechnicianActiveServiceCallsFlow(forceUpdate).collect { }
    }
}