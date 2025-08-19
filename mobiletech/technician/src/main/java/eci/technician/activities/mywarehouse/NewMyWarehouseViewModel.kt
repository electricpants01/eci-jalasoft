package eci.technician.activities.mywarehouse

import androidx.lifecycle.*
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.order.TechnicianWarehousePart
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.repository.RealmLiveData
import eci.technician.viewmodels.ViewModelUtils.Event
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class NewMyWarehouseViewModel : ViewModel() {

    val realm: Realm = Realm.getDefaultInstance()

    var searchQuery: String? = null

    private val _warehouseItemList = MutableLiveData<List<TechnicianWarehousePart>>()
    val warehouseItemsList: LiveData<List<TechnicianWarehousePart>> = _warehouseItemList

    private val _swipeLoading: MutableLiveData<Boolean> = MutableLiveData()
    val swipeLoading: LiveData<Boolean> = _swipeLoading

    val toastMessage = MutableLiveData<Event<Int>>()
    val networkError = MutableLiveData<Event<Pair<ErrorType, String?>>>()

    init {
        fetchParts(forceUpdate = false)
    }

    private val technicianWarehousePartsWithoutCustomer: RealmLiveData<TechnicianWarehousePart> =
        DatabaseRepository.getInstance().technicianWarehousePartsWithoutCustomer
    private val observerParts = Observer<RealmResults<TechnicianWarehousePart>> {
        var list = realm.copyFromRealm(it).toList()
        list = filterList(list)
        _warehouseItemList.value = list
    }

    fun getTechnicianWarehousePartsFromDB() {
        technicianWarehousePartsWithoutCustomer.observeForever(observerParts)
    }

    private fun filterList(list: List<TechnicianWarehousePart>): List<TechnicianWarehousePart> {
        var filteredList = list
        val currentTechWarehouse = AppAuth.getInstance().technicianUser.warehouseId
        filteredList =
            filteredList.filter { it.isFromTechWarehouse(currentTechWarehouse) }.toMutableList()
        filteredList =
            filteredList.filter { part -> part.updateAvailableQuantityUIForMyWarehouse() > 0 }
        filteredList = filteredList.filter { it.availableQty > 0 }

        return filteredList
    }

    @Synchronized
    fun fetchParts(forceUpdate: Boolean) = viewModelScope.launch {
        PartsRepository.getAvailablePartsByWarehouseForTech(forceUpdate).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _swipeLoading.value = false
                    toastMessage.value = Event(R.string.updated)
                }
                is Resource.Error -> {
                    _swipeLoading.value = false
                    val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    when (pairError.first) {
                        ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                            toastMessage.value = Event(R.string.timeout_message)
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
                            networkError.value = Event(pairError)
                            toastMessage.value = Event(R.string.somethingWentWrong)
                        }
                    }
                }
                is Resource.Loading -> {
                    _swipeLoading.value = true
                    toastMessage.value =
                        Event(R.string.updating)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        technicianWarehousePartsWithoutCustomer.removeObserver(observerParts)
        realm.close()
    }


    private var _loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

}