package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.api.retroapi.SearchDataRepository
import eci.technician.models.ProcessingResult
import eci.technician.models.create_call.CallType
import eci.technician.models.create_call.CreateSC
import eci.technician.models.create_call.CustomerItem
import eci.technician.models.create_call.EquipmentItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CreateCallViewModel : ViewModel() {

    var equipmentItemSelected: EquipmentItem? = null
    var callTypeSelected: CallType? = null
    var customerLocationSelected: CustomerItem? = null
    var equipmentListFromCustomerSelected: MutableList<EquipmentItem>? = null
    var descriptionField: String = ""
    var callerField: String = ""
    var remarksField: String = ""
    var assignToMeField: Boolean = true


    var createSC: CreateSC = CreateSC()

    var callTypesList = MutableLiveData<MutableList<CallType>>()
    var callTypesList_: LiveData<MutableList<CallType>> = callTypesList

    var equipmentDataList = MutableLiveData<MutableList<EquipmentItem>>()
    var equipmentDataList_: LiveData<MutableList<EquipmentItem>> = equipmentDataList

    fun getAllCallTypes() {
        callTypesList_ = RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllCallTypes()
    }

    fun createServiceCall(createSC: CreateSC): LiveData<ProcessingResult> {
        return RetrofitRepository.RetrofitRepositoryObject.getInstance().createServiceCall(createSC)
    }

    fun getCustomerDataByText(txtToSearch: String): MutableLiveData<MutableList<CustomerItem>> {
        return RetrofitRepository.RetrofitRepositoryObject.getInstance().getCustomerDataByText(txtToSearch)
    }

    fun validData(): Boolean {
        val validCustomer = customerLocationSelected != null
        val validEquipment = equipmentItemSelected != null
        val validCallType = callTypeSelected != null
        val validCaller = callerField.isNotEmpty()
        return validCustomer && validEquipment && validCallType && validCaller
    }

    fun createServiceCallModel(): CreateSC? {
        if (!validData()) {
            return null
        }
        val technicianId = if (assignToMeField) AppAuth.getInstance().technicianUser.technicianNumber else null
        return CreateSC(callerField, remarksField, descriptionField, equipmentItemSelected?.equipmentNumberID, callTypeSelected?.callTypeId, Date(), technicianId)
    }
}