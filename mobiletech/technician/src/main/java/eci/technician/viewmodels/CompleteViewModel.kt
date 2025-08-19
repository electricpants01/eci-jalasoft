package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.order.IncompleteCode
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.repository.RealmLiveData
import kotlinx.coroutines.GlobalScope

class CompleteViewModel : ViewModel() {

    private val localIncompleteCodes = MutableLiveData<MutableList<IncompleteCode>>()

    var incompleteCodeList: LiveData<MutableList<IncompleteCode>> = localIncompleteCodes

    fun getIncompleteCodes(): RealmLiveData<IncompleteCode> {
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllIncompleteCodes()
        return DatabaseRepository.getInstance().incompleteCodesLiveData
    }

    fun deleteParts(orderId:Int) {
        PartsRepository.deleteAllPartsByOrderId(orderId, GlobalScope)
    }

}