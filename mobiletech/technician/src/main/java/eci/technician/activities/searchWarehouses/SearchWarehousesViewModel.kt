package eci.technician.activities.searchWarehouses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.R
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.order.Part
import eci.technician.repository.PartsRepository
import eci.technician.viewmodels.ViewModelUtils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchWarehousesViewModel : ViewModel() {

    companion object {
        private const val TECHNICIAN_PREFIX = "tch"
    }

    var searchQuery: String = ""
    var techSearchQuery: String = ""
    var selectedOption: String = ""

    private var listOfPartsBackUp = listOf<Part>()

    private val _warehouseItemList = MutableLiveData<List<Part>>()
    val warehouseItemsList: LiveData<List<Part>> = _warehouseItemList

    private var _loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var _warehouseList: MutableLiveData<Pair<String, List<String>>> = MutableLiveData()
    val warehouseList: LiveData<Pair<String, List<String>>> = _warehouseList

    private var _showCustomFilter: MutableLiveData<Boolean> = MutableLiveData()
    val showCustomFilter: LiveData<Boolean> = _showCustomFilter

    private val _swipeLoading: MutableLiveData<Boolean> = MutableLiveData()
    val swipeLoading: LiveData<Boolean> = _swipeLoading

    val toastMessage = MutableLiveData<Event<Int>>()
    val networkError = MutableLiveData<Event<Pair<ErrorType, String?>>>()

    private val _showTechFilter: MutableLiveData<Boolean> = MutableLiveData()
    val showTechFilter: LiveData<Boolean> = _showTechFilter


    fun fetchParts(partCode: String = "") = viewModelScope.launch {
        PartsRepository.getAllAvailablePartsFromAllWarehouses(partCode = partCode)
            .collect { value ->
                when (value) {
                    is Resource.Success -> {
                        _swipeLoading.value = false
                        _loading.value = false
                        toastMessage.value = Event(R.string.updated)
                        val itemComparator = compareBy<Part> { it.item }
                        val quantityComparator = itemComparator.thenByDescending { it.availableQty }
                        listOfPartsBackUp = value.data?.sortedWith(quantityComparator) ?: listOf()
                        _showCustomFilter.value = value.data?.isNotEmpty() ?: false
                        updateWarehouseList(value.data?.toMutableList() ?: mutableListOf())
                        setFilterSelected(selectedOption)
                    }
                    is Resource.Error -> {
                        _swipeLoading.value = false
                        _loading.value = false
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                                toastMessage.value = Event(R.string.timeout_message)
                            }
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            -> {
                                // do nothing because we support offline
                                toastMessage.value = Event(R.string.somethingWentWrong)
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
                        _loading.value = true
                        _swipeLoading.value = true
                        toastMessage.value =
                            Event(R.string.updating)
                    }
                }
            }
    }

    private fun updateWarehouseList(listOfParts: MutableList<Part>) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {

                var warehouseInitialList: MutableList<String> = mutableListOf("All", "Technicians")
                val wareHouseListSaved =
                    _warehouseList.value?.second?.toMutableList() ?: mutableListOf()
                wareHouseListSaved.remove("All")
                wareHouseListSaved.remove("Technicians")

                var warehouseListValues =
                    listOfParts.groupBy { it.warehouse ?: "" }.keys.toMutableList()
                warehouseListValues = warehouseListValues.filter {
                    !(it.lowercase()).startsWith(TECHNICIAN_PREFIX)
                }.toMutableList()

                warehouseListValues.addAll(wareHouseListSaved)
                warehouseListValues = warehouseListValues.distinct().toMutableList()
                warehouseListValues =
                    warehouseListValues.sortedBy { it.lowercase() }.toMutableList()
                warehouseInitialList =
                    warehouseInitialList.plus(warehouseListValues).toMutableList()

                withContext(Dispatchers.Main) {
                    _warehouseList.value = Pair(selectedOption, warehouseInitialList)
                }
            }
        }
    }

    fun filterListByTech(value: String) = viewModelScope.launch {
        techSearchQuery = value
        if (value.isEmpty()) {
            withContext(Dispatchers.Main) {
                _warehouseItemList.value = listOfPartsBackUp.filter {
                    (it.warehouse ?: "").lowercase().startsWith(TECHNICIAN_PREFIX)
                }
            }

        } else {
            withContext(Dispatchers.Default) {
                val listFiltered =
                    listOfPartsBackUp.filter { it.warehouse?.contains(value, true) ?: true }
                withContext(Dispatchers.Main) {
                    _warehouseItemList.value = listFiltered
                }
            }
        }
    }

    fun setFilterSelected(selected: String) {
        this.selectedOption = selected

        when (selectedOption) {
            "All" -> {
                techSearchQuery = ""
                _warehouseItemList.value = listOfPartsBackUp
                _showTechFilter.value = false
            }
            "Technicians" -> {
                viewModelScope.launch {
                    withContext(Dispatchers.Default) {
                        filterListByTech(techSearchQuery)
                        withContext(Dispatchers.Main) {
                            _showTechFilter.value = true
                        }
                    }
                }
            }
            else -> {
                techSearchQuery = ""
                viewModelScope.launch {
                    withContext(Dispatchers.Default) {
                        val filteredList = listOfPartsBackUp.filter { it.warehouse == selected }
                        withContext(Dispatchers.Main) {
                            _warehouseItemList.value = filteredList
                            _showTechFilter.value = false
                        }
                    }
                }
            }
        }
    }
}