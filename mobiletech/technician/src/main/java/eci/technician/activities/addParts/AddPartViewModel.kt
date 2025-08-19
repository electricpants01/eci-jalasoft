package eci.technician.activities.addParts

import androidx.lifecycle.*
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.data.UsedPart
import eci.technician.models.order.Bin
import eci.technician.models.order.TechnicianWarehousePart
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.repository.RealmLiveData
import eci.technician.repository.ServiceOrderRepository
import eci.technician.repository.ServiceOrderRepository.ServiceOrderStatus
import eci.technician.viewmodels.ViewModelUtils
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPartViewModel : ViewModel() {

    val realm: Realm = Realm.getDefaultInstance()
    var searchQuery: String? = ""
    var searchQueryBin: String? = ""
    var customerWarehouseId = 0
    var partSelected: TechnicianWarehousePart? = null
    var binSelected: Bin? = null
    var callId: Int = 0
        set(value) {
            field = value
            updateServiceCallStatus()
        }
    var addPartAsPending = false
    var serviceCallStatus: ServiceOrderStatus =
        ServiceOrderStatus.PENDING
    var currentTechWarehouse = 0

    private val _warehouseItemList = MutableLiveData<List<TechnicianWarehousePart>>()
    val warehouseItemsList: LiveData<List<TechnicianWarehousePart>> = _warehouseItemList

    private var _loadingParts: MutableLiveData<Boolean> = MutableLiveData()
    val loadingParts: LiveData<Boolean> = _loadingParts

    val toastMessage = MutableLiveData<ViewModelUtils.Event<Int>>()
    val networkError = MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>>()

    init {
        currentTechWarehouse = AppAuth.getInstance().technicianUser.warehouseId
        fetchTechnicianWarehouseParts(forceUpdate = false)
    }

    fun fetchTechnicianWarehouseParts(forceUpdate: Boolean) {
        viewModelScope.launch {
            val getAvailablePartsByWarehouseFlow = when (serviceCallStatus) {
                ServiceOrderStatus.DISPATCHED, ServiceOrderStatus.ARRIVED -> {
                    PartsRepository.getAvailablePartsByWarehouseForOffline(forceUpdate)
                }
                else -> {
                    PartsRepository.getAvailablePartsByWarehouseForTech(forceUpdate)
                }
            }

            getAvailablePartsByWarehouseFlow.collect { value ->
                when (value) {
                    is Resource.Success -> {
                        _loadingParts.value = false
                    }
                    is Resource.Error -> {
                        _loadingParts.value = false
                        val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                        when (pair.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                                toastMessage.value = ViewModelUtils.Event(R.string.timeout_message)
                            }
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            -> {
                                // do nothing because we support offline
                            }
                            ErrorType.NOT_SUCCESSFUL,
                            ErrorType.BACKEND_ERROR,
                            ErrorType.HTTP_EXCEPTION,
                            ErrorType.SOMETHING_WENT_WRONG -> {
                                networkError.value = ViewModelUtils.Event(pair)
                                toastMessage.value =
                                    ViewModelUtils.Event(R.string.somethingWentWrong)
                            }
                        }
                    }
                    is Resource.Loading -> {
                        _loadingParts.value = true
                    }
                }
            }
        }
    }

    private fun updateServiceCallStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serviceCallStatus =
                    ServiceOrderRepository.getServiceOrderStatusByCallNumberId(callId)
            }
        }
    }


    private val technicianWarehouseParts: RealmLiveData<TechnicianWarehousePart> =
        DatabaseRepository.getInstance().technicianWarehouseParts
    private val observerParts = Observer<RealmResults<TechnicianWarehousePart>> {
        var list = realm.copyFromRealm(it).toList()
        list = filterList(list)
        _warehouseItemList.value = list
    }

    fun getTechnicianWarehousePartsFromDB() {
        technicianWarehouseParts.observeForever(observerParts)
    }


    private fun filterList(list: List<TechnicianWarehousePart>): MutableList<TechnicianWarehousePart> {
        val partListCopy = list.toMutableList()

        if (!(serviceCallStatus == ServiceOrderStatus.DISPATCHED ||
                    serviceCallStatus == ServiceOrderStatus.ARRIVED) ||
            customerWarehouseId == 0
        ) {
            partListCopy.removeIf { !it.isFromTechWarehouse(currentTechWarehouse) }
        }

        partListCopy.removeIf { !it.isFromTechWarehouse(currentTechWarehouse) && it.warehouseID != customerWarehouseId }

        val list1 = deleteNegativeValues(partListCopy.toMutableList())

        return list1.sortedWith(
            compareBy(
                { it.item },
                { it.isFromTechWarehouse(currentTechWarehouse) })
        ).toMutableList()
    }


    private fun deleteNegativeValues(warehouses: MutableList<TechnicianWarehousePart>): MutableList<TechnicianWarehousePart> {
        warehouses.removeIf {
            it.getCalculatedQuantityInDatabase() < 1.0
        }
        return warehouses
    }

    fun createUsedPart(quantity: Double, created: () -> Unit) {
        val usedPart = UsedPart.createInstance(
            callId = callId,
            itemId = partSelected?.itemId ?: 0,
            partName = partSelected?.item ?: "",
            partDescription = partSelected?.description ?: "",
            quantity = quantity,
            localUsageStatus = if (addPartAsPending) UsedPart.PENDING_STATUS_CODE else UsedPart.USED_STATUS_CODE,
            warehouseId = partSelected?.warehouseID ?: 0,
            warehouseName = partSelected?.warehouse ?: "",
            bindId = binSelected?.binId ?: 0,
            binName = binSelected?.bin ?: "",
            serialNumber = binSelected?.serialNumber ?: ""
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PartsRepository.saveNewPart(usedPart)
                withContext(Dispatchers.Main) {
                    created.invoke()
                }
            }
        }

    }


    override fun onCleared() {
        super.onCleared()
        technicianWarehouseParts.removeObserver(observerParts)
        realm.close()
    }

}