package eci.technician.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.signalr.messenger.Conversation
import eci.technician.activities.serviceOrderFilter.filterModels.*
import eci.technician.helpers.AppAuth
import eci.technician.helpers.FilterHelper
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.filters.FilterCriteria
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.FilterRepository
import eci.technician.repository.RealmLiveData
import eci.technician.repository.ServiceOrderRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlin.coroutines.coroutineContext

class OrderFragmentViewModel : ViewModel() {

    companion object {
        const val TAG = "OrderFragmentViewModel"
        const val EXCEPTION = "Exception"
    }

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val filterRepository = FilterRepository(ioDispatcher)

    var isGroupFilter = false

    var lastGroupNameSelected = "randomText"
    var lastGroupNameLoaded = ""

    var textToSearch = ""
    var shouldShowMessage = true

    private var _swipeLoading: MutableLiveData<Boolean> = MutableLiveData()
    val swipeLoading: LiveData<Boolean> = _swipeLoading

    private var _networkError: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val networkError: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _networkError

    private var _serviceOrderList: MutableLiveData<List<ServiceOrder>> = MutableLiveData()
    val serviceOrderList: LiveData<List<ServiceOrder>> = _serviceOrderList

    private var _priorityList: MutableLiveData<List<PriorityFilter>> = MutableLiveData()
    val priorityList: LiveData<List<PriorityFilter>> = _priorityList

    private var _callTypeList: MutableLiveData<List<CallTypeFilter>> = MutableLiveData()
    val callTypeList: LiveData<List<CallTypeFilter>> = _callTypeList

    private var _statusList: MutableLiveData<List<StatusFilter>> = MutableLiveData()
    val statusList: LiveData<List<StatusFilter>> = _statusList

    private var _dateList: MutableLiveData<List<DateFilter>> = MutableLiveData()
    val dateFilter: LiveData<List<DateFilter>> = _dateList

    private var _sortList: MutableLiveData<List<CallSortItem>> = MutableLiveData()
    val sortList: LiveData<List<CallSortItem>> = _sortList

    private var _filterCriteria: MutableLiveData<FilterCriteria> = MutableLiveData()
    val filterCriteria: LiveData<FilterCriteria> = _filterCriteria

    private var _technicalsFilterList: MutableLiveData<List<TechnicianFilter>> = MutableLiveData()
    val technicianFilterList: LiveData<List<TechnicianFilter>> = _technicalsFilterList

    private var _groupList: MutableLiveData<List<GroupFilter>> = MutableLiveData()
    val groupList: LiveData<List<GroupFilter>> = _groupList

    private var _filterCriteriaFlow: MutableLiveData<FilterCriteria> = MutableLiveData()
    val filterCriteriaFlow: LiveData<FilterCriteria> = _filterCriteriaFlow

    fun fetchTechnicianActiveServiceCalls(forceUpdate:Boolean = false) = viewModelScope.launch {
        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .getTechnicianActiveServiceCallsFlow(forceUpdate).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        _swipeLoading.value = false
                    }
                    is Resource.Error -> {
                        _swipeLoading.value = false
                        val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                        _networkError.value = ViewModelUtils.Event(pair)
                    }
                    is Resource.Loading -> {
                        _swipeLoading.value = true
                    }
                }
            }
    }

    fun getFilterCriteriaFlow() = viewModelScope.launch {
        try {
            filterRepository.filterCriteriaFlow(isGroupFilter).safeCollect {
                it?.let {
                    _filterCriteriaFlow.value = it
                } ?: kotlin.run {
                    if (isGroupFilter){
                        filterRepository.getFilterCriteriaForGroupCalls()
                    }
                }
            }.runCatching {
                Log.i(TAG, EXCEPTION)
            }
        }catch (e:Exception){
            Log.i(TAG, EXCEPTION, e)
        }

    }

    fun getServiceOrder() = viewModelScope.launch {
        try {
            val serviceOrderFlow = ServiceOrderRepository.getServiceCallList()
            val filterFlow = filterRepository.filterCriteriaFlow(isGroupFilter)
            serviceOrderFlow.combine(filterFlow){ serviceOrder, _ ->
                serviceOrder
            }.safeCollect {
                if (AppAuth.getInstance().technicianUser.isRestrictCallOrder) {
                    val list = FilterHelper.filterByQuantity(it.toMutableList())
                    _serviceOrderList.value = list
                } else {
                    val listFiltered = applyServiceCallFilters(it.toMutableList())
                    val list = ServiceOrderRepository.moveDispatchedServiceOrdersOnTop(listFiltered)
                    _serviceOrderList.value = list
                }
            }.runCatching {
                Log.i(TAG, EXCEPTION)
            }
        }catch (e:Exception){
            Log.i(TAG, "getServiceOrder", e)
        }
    }

    private suspend inline fun <T> Flow<T>.safeCollect(crossinline action: suspend (T) -> Unit) {
        collect {
            coroutineContext.ensureActive()
            action(it)
        }
    }

    fun applyServiceCallFilters(list: MutableList<ServiceOrder>): MutableList<ServiceOrder> {
        val filterCriteria = filterRepository.getFilterCriteriaForTechnicianCalls()
        var listFiltered = list
        filterCriteria?.callSortItemSelected?.let { value ->
            listFiltered = FilterHelper.sortServiceOrdersByDate(listFiltered, value)
        }
        filterCriteria?.callDateSelected?.let { value ->
            val filterDate = FilterHelper.convertIntToFilterDate(value)
            listFiltered = FilterHelper.filterServiceOrdersByDate(filterDate, listFiltered)
        }
        filterCriteria?.callStatusSelected?.let { value ->
            val serviceCallStatus = FilterHelper.convertIntToServiceCallStatus(value)
            listFiltered = FilterHelper.filterServiceOrderByStatus(serviceCallStatus, listFiltered)
        }
        filterCriteria?.callPrioritySelected?.let { value ->
            listFiltered = FilterHelper.filterServiceOrdersByPriority(value, listFiltered)
        }
        filterCriteria?.callTypeFilterSelected?.let { value ->
            listFiltered = FilterHelper.filterServiceOrdersByCallType(value, listFiltered)
        }
        return listFiltered
    }

    fun applyGroupServiceCallFilter(list: MutableList<GroupCallServiceOrder>): MutableList<GroupCallServiceOrder> {
        val filterCriteria = filterRepository.getFilterCriteriaForGroupCalls()
        var listFiltered = list
        filterCriteria?.callSortItemSelected?.let { value ->
            listFiltered = FilterHelper.sortGroupServiceOrdersByDate(listFiltered, value)
        }
        filterCriteria?.callDateSelected?.let { value ->
            val filterDate = FilterHelper.convertIntToFilterDate(value)
            listFiltered = FilterHelper.filterGroupServiceOrdersByDate(filterDate, listFiltered)
        }
        filterCriteria?.callStatusSelected?.let { value ->
            val serviceCallStatus = FilterHelper.convertIntToServiceCallStatus(value)
            listFiltered =
                FilterHelper.filterGroupServiceOrderByStatus(serviceCallStatus, listFiltered)
        }
        filterCriteria?.callPrioritySelected?.let { value ->
            listFiltered = FilterHelper.filterGroupServiceOrderByPriority(value, listFiltered)
        }

        filterCriteria?.callTechnicianNumberIdSelected?.let { value ->
            listFiltered = FilterHelper.filterGroupServiceOrderByTechnician(value, listFiltered)
        }
        filterCriteria?.callTypeFilterSelected?.let { value ->
            listFiltered = FilterHelper.filterGroupServiceOrdersByCallType(value, listFiltered)
        }
        return listFiltered
    }

    fun getConversations(): RealmLiveData<Conversation> {
        return DatabaseRepository.getInstance().conversations
    }

    fun checkIfCompletedStatus(
        callId: Int,
        error: Pair<ErrorType, String?>,
        isCompleted: (Boolean, Pair<ErrorType, String?>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = ServiceOrderRepository.checkIfCompletedStatus(callId)
            withContext(Dispatchers.Main) {
                isCompleted.invoke(res, error)
            }
        }
    }

    fun isShowingMessage() {
        shouldShowMessage = false
        viewModelScope.launch {
            delay(10000)
            shouldShowMessage = true
        }
    }

    fun fetchPriorityListFromDB() = viewModelScope.launch {
        if (isGroupFilter) {
            val filterCriteria = filterRepository.getFilterCriteriaForGroupCalls()
            val list = filterRepository.getGroupPriorityFilterList()
                .map { PriorityFilter(it.priorityName ?: "", it.isChecked, it.id) }
            filterCriteria?.let {
                it.callPriorityIdSelected?.let { priorityId ->
                    list.forEach { priorityItem ->
                        if (priorityItem.priorityId == priorityId) {
                            priorityItem.isChecked = true
                        }
                    }
                }
            }
            _priorityList.value = list
        } else {
            val filterCriteria = filterRepository.getFilterCriteriaForTechnicianCalls()
            val list = filterRepository.getTechnicianPriorityFilterList()
                .map { PriorityFilter(it.priorityName ?: "", it.isChecked, it.id) }
            filterCriteria?.let {
                it.callPriorityIdSelected?.let { priorityId ->
                    list.forEach { priorityItem ->
                        if (priorityItem.priorityId == priorityId) {
                            priorityItem.isChecked = true
                        }
                    }
                }
            }
            _priorityList.value = list
        }

    }

    fun fetchTechniciansListFromDB() = viewModelScope.launch {
        val filterCriteriaDB = filterRepository.getFilterCriteriaForGroupCalls()
        val list = filterRepository.getCallTechnicianForFilterList().map {
            TechnicianFilter(it.technicianNumberId, it.technicianName ?: "", false)
        }
        filterCriteriaDB?.let {
            list.forEach { technicianItem ->
                if (technicianItem.technicianNumberId == filterCriteriaDB.callTechnicianNumberIdSelected) {
                    technicianItem.isChecked = true
                }
            }
        }

        _technicalsFilterList.value = list

    }

    fun fetchCallTypeListFromDB() = viewModelScope.launch {
        if (isGroupFilter) {
            val filterCriteria = filterRepository.getFilterCriteriaForGroupCalls()
            val list = filterRepository.getGroupCallTypeList().map {
                CallTypeFilter(
                    it.callTypeId,
                    it.callTypeDescription ?: "",
                    it.callTypeCode ?: "",
                    false
                )
            }
            filterCriteria?.let {
                list.forEach { callTypeItem ->
                    if (callTypeItem.callTypeId == filterCriteria.callTypeIdFilterSelected) {
                        callTypeItem.isChecked = true
                    }
                }
            }
            _callTypeList.value = list
        } else {
            val filterCriteria = filterRepository.getFilterCriteriaForTechnicianCalls()
            val list = filterRepository.getTechnicianCallTypeList().map {
                CallTypeFilter(
                    it.callTypeId,
                    it.callTypeDescription ?: "",
                    it.callTypeCode ?: "",
                    false
                )
            }
            filterCriteria?.let {
                list.forEach { callTypeItem ->
                    if (callTypeItem.callTypeId == filterCriteria.callTypeIdFilterSelected) {
                        callTypeItem.isChecked = true
                    }
                }
            }
            _callTypeList.value = list
        }

    }

    fun fetchDateList() = viewModelScope.launch {
        val filterCriteriaDB =
            if (isGroupFilter) filterRepository.getFilterCriteriaForGroupCalls() else filterRepository.getFilterCriteriaForTechnicianCalls()
        val list = filterRepository.getTechnicianDateList()
        filterCriteriaDB?.let {
            list.forEach { dateItem ->
                if (dateItem.id == filterCriteriaDB.callDateSelected) {
                    dateItem.isChecked = true
                }
            }
        }
        _dateList.value = list
    }


    fun fetchCallStatusList() = viewModelScope.launch {
        val filterCriteriaDB =
            if (isGroupFilter) filterRepository.getFilterCriteriaForGroupCalls() else filterRepository.getFilterCriteriaForTechnicianCalls()
        val list = filterRepository.getTechnicianStatusList()
        filterCriteriaDB?.let {
            list.forEach { callStatusItem ->
                if (callStatusItem.id == filterCriteriaDB.callStatusSelected) {
                    callStatusItem.isChecked = true
                }
            }
        }
        _statusList.value = list
    }

    fun fetchSortItemsList() = viewModelScope.launch {
        val filterCriteriaDB =
            if (isGroupFilter) filterRepository.getFilterCriteriaForGroupCalls() else filterRepository.getFilterCriteriaForTechnicianCalls()
        val list = filterRepository.getTechnicianSortList()
        filterCriteriaDB?.let {
            list.forEach { callSortItem ->
                if (callSortItem.id == filterCriteriaDB.callSortItemSelected) {
                    callSortItem.isChecked = true
                }
            }
        }
        _sortList.value = list
    }

    fun saveOpenFilters(
        sortOpen: Boolean,
        whenOpen: Boolean,
        statusOpen: Boolean,
        priorityOpen: Boolean,
        callTypeOpen: Boolean,
        groupOpen: Boolean,
        technicianOpen: Boolean
    ) = viewModelScope.launch {
        if (isGroupFilter) {
            filterRepository.saveOpenFiltersForGroup(
                sortOpen = sortOpen,
                whenOpen = whenOpen,
                statusOpen = statusOpen,
                priorityOpen = priorityOpen,
                callTypeOpen = callTypeOpen,
                groupOpen = groupOpen,
                techniciansOpen = technicianOpen
            )
        } else {
            filterRepository.saveOpenFiltersForTechnicians(
                sortOpen,
                whenOpen,
                statusOpen,
                priorityOpen,
                callTypeOpen
            )
        }

    }

    fun fetchFilterCriteriaFromDB() = viewModelScope.launch {
        val techCriteria = filterRepository.getFilterCriteriaForTechnicianCalls() ?: return@launch
        val groupCriteria = filterRepository.getFilterCriteriaForGroupCalls() ?: return@launch
        _filterCriteria.value = if (isGroupFilter) groupCriteria else techCriteria

    }

    fun saveCheckedFilters(
        dateList: List<DateFilter>,
        sortList: List<CallSortItem>,
        callTypeList: MutableList<CallTypeFilter>,
        statusList: MutableList<StatusFilter>,
        priorityList: List<PriorityFilter>,
        technicianList: MutableList<TechnicianFilter>,
        groupList: MutableList<GroupFilter>
    ) = viewModelScope.launch {
        val checkedDate = dateList.find { it.isChecked }
        val checkedSort = sortList.find { it.isChecked }
        val checkedCallType = callTypeList.find { it.isChecked }
        val checkedStatus = statusList.find { it.isChecked }
        val checkedPriority = priorityList.find { it.isChecked }
        val checkedTechnicians = technicianList.find { it.isChecked }
        val checkedGroup = groupList.find { it.isChecked }

        if (isGroupFilter) {
            filterRepository.saveCheckedFiltersForGroups(
                checkedDate,
                checkedSort,
                checkedCallType,
                checkedStatus,
                checkedPriority,
                checkedTechnicians,
                checkedGroup
            )
        } else {
            filterRepository.saveCheckedFiltersForTechnician(
                checkedDate,
                checkedSort,
                checkedCallType,
                checkedStatus,
                checkedPriority
            )
        }
    }

    fun clearFilterData() {
        viewModelScope.launch {
            if (isGroupFilter) {
                filterRepository.deleteFilterGroupCriteria()
            } else {
                filterRepository.deleteFilterTechnicianCriteria()
            }
        }
    }

    fun fetchGroupsForFilter() = viewModelScope.launch {
        val filterCriteria = filterRepository.getFilterCriteriaForGroupCalls()
        val techId = AppAuth.getInstance().technicianUser?.id ?: return@launch
        filterRepository.getGroupsForTechnician(techId).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    val groupList =
                        filterRepository.createGroupsList(resource.data?.result ?: "").map {
                            GroupFilter(it.groupName, it.description, false)
                        }
                    filterCriteria?.let {
                        groupList.forEach { groupItem ->
                            if (groupItem.groupName == filterCriteria.groupNameSelected) {
                                groupItem.isChecked = true
                            }
                        }
                    }
                    _groupList.value = groupList
                }
                is Resource.Error -> {
                    _groupList.value = mutableListOf()
                }
                is Resource.Loading -> {
                    // do nothing
                }
            }
        }
    }

}