package eci.technician.activities.transfers

import android.util.Log
import androidx.lifecycle.*
import eci.technician.helpers.AppAuth
import eci.technician.helpers.DecimalsHelper
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.ProcessingResult
import eci.technician.models.transfers.Bin
import eci.technician.models.transfers.CreateTransferModel
import eci.technician.models.transfers.Part
import eci.technician.models.transfers.Warehouse
import eci.technician.repository.PartsRepository
import eci.technician.repository.TransfersRepository
import eci.technician.tools.Settings
import eci.technician.viewmodels.ViewModelUtils.Event
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TransferViewModel : ViewModel() {

    private val _sourceSelectWarehouse = MutableLiveData<Warehouse?>()
    var sourceSelectWarehouse: LiveData<Warehouse?> = _sourceSelectWarehouse
    private val _sourceAvailableQuantity = MutableLiveData<Double?>(null)
    var sourceAvailableQuantity: LiveData<Double?> = _sourceAvailableQuantity
    private val _sourceUsedQuantity = MutableLiveData<Double>(null)
    var sourceUsedQuantity: LiveData<Double> = _sourceUsedQuantity

    private val _sourceSelectedPart = MutableLiveData<Part?>()
    var sourceSelectedPart: LiveData<Part?> = _sourceSelectedPart

    private val _sourceSelectBin = MutableLiveData<Bin?>()
    var sourceSelectBin: LiveData<Bin?> = _sourceSelectBin

    private val _destinationSelectedWarehouse = MutableLiveData<Warehouse?>()
    var destinationSelectedWarehouse: LiveData<Warehouse?> = _destinationSelectedWarehouse

    private val _destinationSelectedBin = MutableLiveData<Bin?>()
    var destinationSelectedBin: LiveData<Bin?> = _destinationSelectedBin

    private val _isLoading = MutableLiveData<Boolean>(true)
    var isLoading: LiveData<Boolean> = _isLoading

    private val _warehouseList = MutableLiveData<List<Warehouse>>()
    var warehouseList: LiveData<List<Warehouse>> = _warehouseList

    private var _availablePartsResponseEvent: MutableLiveData<Event<Boolean>> = MutableLiveData()
    val availablePartsResponseEvent: LiveData<Event<Boolean>> = _availablePartsResponseEvent

    private var _availablePartsLoader: MutableLiveData<Boolean> = MutableLiveData()
    val availablePartsLoader: LiveData<Boolean> = _availablePartsLoader

    private val _networkError = MutableLiveData<Event<Pair<ErrorType, String?>>>()
    var networkError: LiveData<Event<Pair<ErrorType, String?>>> = _networkError
    var selectingSource = true
    var description = "No description"

    var formattedAvailableQuantity = Transformations.map(_sourceAvailableQuantity) { quantity ->
        if (quantity == null) "0"
        else DecimalsHelper.getValueFromDecimal(quantity)
    }

    var formattedUsedQuantity = Transformations.map(_sourceUsedQuantity) { quantity ->
        if (quantity == null) "0"
        else DecimalsHelper.getValueFromDecimal(quantity)
    }

    var setFocusToQuantity = false


    var techWarehouse: Warehouse? = null
    var isResettingWarehouse = false
    var transferType: Int = Warehouse.COMPANY_TYPE
    var customerWarehouseId: Int = 0
    var filteredWarehouseList = Transformations.map(_warehouseList) { list ->
        TransferFilterHelper.filterWarehouses(list,this)
    }

    fun resetSource() {
        _sourceSelectWarehouse.value = null
        _sourceSelectedPart.value = null
        _sourceSelectBin.value = null
    }

    fun setDefaultDestinationBin() {
        if (_destinationSelectedWarehouse.value != null) {
            refreshDestinationWarehouse()
        }
        val defaultBin = _destinationSelectedWarehouse.value?.bins?.let {
            getDefaultBin(
                it, false
            )
        }
        if (defaultBin != null) {
            setDestinationSelectedBin(defaultBin)
        }
    }

    private fun refreshDestinationWarehouse() {
        if (_warehouseList.value?.isNotEmpty() == true) {
            val warehouseId = _destinationSelectedWarehouse.value?.warehouseID
            _destinationSelectedWarehouse.value =
                _warehouseList.value?.filter { warehouse -> warehouse.warehouseID == warehouseId }
                    ?.get(0)
        }
    }

    @Synchronized
    fun getWarehouses() {
        viewModelScope.launch {
            val partId = sourceSelectedPart.value?.itemId ?: 0
            TransfersRepository.getWarehouses(partId).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        Log.d("transfers", "getting warehouse")
                        _isLoading.value = false
                        setWarehouseList(value.data?.result?.let {
                            TransfersRepository.createWarehouseListFromJSON(
                                it
                            )
                        })
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION,
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            ErrorType.NOT_SUCCESSFUL,
                            ErrorType.BACKEND_ERROR,
                            ErrorType.HTTP_EXCEPTION,
                            ErrorType.SOMETHING_WENT_WRONG -> {
                                _networkError.value = Event(pairError)
                            }
                        }
                    }
                    is Resource.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    fun resetAll() {
        _sourceSelectWarehouse.value = null
        _sourceSelectedPart.value = null
        _sourceSelectBin.value = null
        _sourceAvailableQuantity.value = 0.0
        _sourceUsedQuantity.value = 0.0
        description = "No description"
        _destinationSelectedWarehouse.value = null
        _destinationSelectedBin.value = null
        selectingSource = true
    }

    fun getCreateTransferModel(): CreateTransferModel {
        return CreateTransferModel(
            createDescription(),
            destinationSelectedWarehouse.value?.warehouseID ?: 0,
            destinationSelectedBin.value?.binId ?: 0,
            sourceSelectedPart.value?.itemId ?: 0,
            sourceUsedQuantity.value?.toInt() ?: 0,
            sourceSelectBin.value?.serialNumber ?: "",
            sourceSelectWarehouse?.value?.warehouseID ?: 0,
            sourceSelectBin.value?.binId ?: 0
        )
    }

    private fun createDescription(): String {
        val origin = sourceSelectWarehouse?.value?.warehouse ?: "-"
        val destiny = destinationSelectedWarehouse.value?.warehouse ?: "-"
        return "MobileTech Warehouse Transfer from $origin to $destiny"
    }

    fun retrieveWarehouseList(processingResult: ProcessingResult?): List<Warehouse> {
        processingResult?.result?.let {
            return Settings.createGson()
                .fromJson(
                    it, Array<Warehouse>::class.java
                ).toList()
        }
        return listOf()
    }

    fun updateListPartQuantity(list: List<Part>): List<Part> {
        list.forEach { part ->
            part.bins = updateListBinQuantity(part.bins, part.itemId, part.warehouseId)
            part.updatedAvailableQty = part.getCalculatedQuantityInDatabase()
        }
        return list
    }

    fun filterOnlyPositiveAvailable(list: List<Part>): List<Part> {
        return list.filter { part -> part.updatedAvailableQty >= 1.0 }
    }

    fun updateListBinQuantity(list: List<Bin>, partId: Int, warehouseId: Int): List<Bin> {
        list.forEach { bin ->
            bin.updatedBinQty = bin.binAvailableQty - PartsRepository.getUsedQuantityBins(
                bin.binId,
                partId,
                warehouseId,
                bin.serialNumber ?: ""
            )
        }
        return list
    }

    fun setSourceWarehouse(warehouse: Warehouse) {
        _sourceSelectWarehouse.value = warehouse
    }

    fun setSourcePart(part: Part?) {
        _sourceSelectedPart.value = part
    }

    fun setSourceSelectedBin(value: Bin?) {
        _sourceSelectBin.value = value
    }

    fun setAvailableQuantity(value: Double) {
        _sourceAvailableQuantity.value = value
    }

    fun setSourceUsedQuantity(value: Double) {
        _sourceUsedQuantity.value = value
    }

    fun setDestinationWarehouse(warehouse: Warehouse?) {
        _destinationSelectedWarehouse.value = warehouse
    }

    fun setDestinationSelectedBin(value: Bin?) {
        _destinationSelectedBin.value = value
    }

    fun setWarehouseList(warehouseList: List<Warehouse>?) {
        warehouseList?.let {
            _warehouseList.value = it
            if (techWarehouse == null) {
                techWarehouse = TransferFilterHelper.filterTechWarehouse(it)
            }
            if (_destinationSelectedWarehouse != null && warehouseList.isNotEmpty()) {
                setDefaultDestinationBin()
            }
        }
    }

    fun setLoading(value: Boolean) {
        _isLoading.value = value
    }

    fun getDefaultBin(bins: List<Bin>, isForSource: Boolean): Bin? {
        var defaultBin: Bin? = null
        bins.forEach { bin ->
            if (bin.isDefaultBin) {
                if (!isForSource)
                    return bin
                if (bin.binAvailableQty > 0 && defaultBin != null)
                    return null
                if (bin.binAvailableQty > 0) {
                    defaultBin = bin
                }
            }
        }
        return defaultBin
    }

    fun getSortedBinList(bins: List<Bin>): List<Bin> {
        var binList = bins
        if (containsAtLeastOneSerial(bins))
            binList = binList.sortedWith(compareBy({ !it.isDefaultBin }, { it.bin }))
        else
            binList = binList.sortedBy { bin -> bin.bin }
        return binList
    }

    private fun containsAtLeastOneSerial(bins: List<Bin>): Boolean {
        bins.forEach { bin ->
            if (!bin.serialNumber.isNullOrBlank())
                return true
        }
        return false
    }


    @Synchronized
    fun getAvailablePartsForTech() = viewModelScope.launch {
        PartsRepository.getAvailablePartsByWarehouseForTech(forceUpdate = true).collect { value ->
            when (value) {
                is Resource.Success, is Resource.Error -> {
                    _availablePartsResponseEvent.value = Event(true)
                    _availablePartsLoader.value = false
                }
                is Resource.Loading -> {
                    _availablePartsLoader.value = true
                }
            }
        }
    }
}

