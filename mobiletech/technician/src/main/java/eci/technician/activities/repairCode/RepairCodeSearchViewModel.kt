package eci.technician.activities.repairCode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.models.order.RepairCode
import eci.technician.repository.CodesRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RepairCodeSearchViewModel : ViewModel() {

    private var _searchQuery: MutableLiveData<String> = MutableLiveData()
    val searchQuery: LiveData<String> = _searchQuery
    private var _repairCodeList: MutableLiveData<List<RepairCode>> = MutableLiveData()
    val repairCodeList: LiveData<List<RepairCode>> = _repairCodeList

    fun getAllRepairCodes() = viewModelScope.launch {
        CodesRepository.getAllRepairCodes().collect { myRepairCodeList ->
            _repairCodeList.postValue(myRepairCodeList)
        }
    }

    fun setSearchQuery(query: String){
        _searchQuery.postValue(query)
    }

    fun saveProblemCodeToDB(
        orderId: Int,
        repairCodeId: Int,
        repairCodeName: String,
        description: String,
        onCompleteSave: () -> Unit
    ) {
        val isRepeated = CodesRepository.isRepairCodeRepeated(orderId, repairCodeName.toString())
        if (!isRepeated) {
            CodesRepository.saveRepairCodeToDB(
                orderId,
                repairCodeId,
                repairCodeName,
                description,
                onCompleteSave
            )
        }
    }
}