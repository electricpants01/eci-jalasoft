package eci.technician.helpers.api.retroapi

import androidx.lifecycle.MutableLiveData
import eci.technician.models.create_call.CustomerItem
import eci.technician.models.create_call.EquipmentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.net.SocketTimeoutException

object SearchDataRepository {
    var apiService: MobileTechApi? = null
    var searchCustomerItemsLiveData: MutableLiveData<GenericDataModel<MutableList<CustomerItem>>?> = MutableLiveData()
    var searchEquipmentItemLiveData: MutableLiveData<GenericDataModel<MutableList<EquipmentItem>>?> = MutableLiveData()

    init {
        apiService = RetrofitApiHelper.getApi()
    }

    suspend fun getSearchForCustomerItem(searchQuery: String?) {
        searchCustomerItemsLiveData.postValue(null)
        try {
            val result = apiService?.getCustomerDataByTextSuspend(searchQuery)
            val genericDataModel = GenericDataModel(result?.isSuccessful, result?.body(), result?.message(), RequestStatus.SUCCESS)
            searchCustomerItemsLiveData.postValue(genericDataModel)
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                val genericDataModel = GenericDataModel(false, mutableListOf<CustomerItem>(), "", RequestStatus.TIMEOUT)
                searchCustomerItemsLiveData.postValue(genericDataModel)
            } else {
                val genericDataModel = GenericDataModel(false, mutableListOf<CustomerItem>(), e.message, RequestStatus.ERROR)
                searchCustomerItemsLiveData.postValue(genericDataModel)
            }
        }
    }

    /**
     *  Used when there is a customer selected previously, so we filter on the existing equipments from that customer
     */
    fun filterEquipmentItemBySearchQuery(searchQuery: String, customerItem: CustomerItem?){
        searchEquipmentItemLiveData.postValue(null)
        val result = customerItem?.equipments?.filter {
            it.equipmentNumberCode.lowercase().contains(searchQuery) ||
                    it.modelNumberCode.lowercase().contains(searchQuery) ||
                    it.modelDescription.lowercase().contains(searchQuery) ||
                    it.serialNumber.lowercase().contains(searchQuery)

        }?.toMutableList()
        val genericDataModel = GenericDataModel(true, result, null  , RequestStatus.SUCCESS)
        searchEquipmentItemLiveData.postValue(genericDataModel)
    }

    /**
     *  Used when there is NO customerItem selected, so we fetch all the equipment items
     */
    suspend fun fetchEquipmentItemBySearchQuery(searchQuery: String?) {
        searchEquipmentItemLiveData.postValue(null)
        try {
            val result = apiService?.getEquipmentsDataByTextSuspend(searchQuery)
            val genericDataModel = GenericDataModel(result?.isSuccessful, result?.body(), result?.message(), RequestStatus.SUCCESS)
            searchEquipmentItemLiveData.postValue(genericDataModel)
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                val genericDataModel = GenericDataModel(false, mutableListOf<EquipmentItem>(), "", RequestStatus.TIMEOUT)
                searchEquipmentItemLiveData.postValue(genericDataModel)
            } else {
                val genericDataModel = GenericDataModel(false, mutableListOf<EquipmentItem>(), e.message, RequestStatus.TIMEOUT)
                searchEquipmentItemLiveData.postValue(genericDataModel)
            }
        }
    }

}

