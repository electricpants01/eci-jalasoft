package eci.technician.activities.problemCodes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.models.order.ProblemCode
import eci.technician.repository.CodesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProblemCodesSearchViewModel : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    val searchQuery = MutableLiveData<String>()
    var orderId: MutableLiveData<Int> = MutableLiveData()
    private var _problemCodesList: MutableLiveData<List<ProblemCode>> = MutableLiveData()
    val problemCodesList: LiveData<List<ProblemCode>> = _problemCodesList

    init {
        getAllProblemCodes()
    }

    fun getAllProblemCodes() = viewModelScope.launch {
        CodesRepository.getAllProblemCodes().collect {
            _problemCodesList.value = it
        }
    }

    fun saveProblemCodeToDB(
        problemCodeId: Int,
        problemCodeName: String,
        problemCodeDescription: String,
        onCompleteSave: () -> Unit
    ) = viewModelScope.launch(dispatcher) {
        orderId.value?.let { orderIdValue ->
            val isRepeated = CodesRepository.isProblemCodeRepeated(problemCodeName, orderIdValue)
            if (!isRepeated) {
                CodesRepository.saveProblemCodeToDB(
                    problemCodeId,
                    problemCodeName,
                    problemCodeDescription,
                    orderIdValue,
                    onCompleteSave
                )
            }
        }
    }

}