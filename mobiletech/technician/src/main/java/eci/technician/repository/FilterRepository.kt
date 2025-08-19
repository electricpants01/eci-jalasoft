package eci.technician.repository

import android.util.Log
import com.google.gson.reflect.TypeToken
import eci.technician.R
import eci.technician.activities.serviceOrderFilter.filterModels.*
import eci.technician.helpers.api.retroapi.ApiUtils
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.TechnicianGroup
import eci.technician.models.filters.*
import eci.technician.tools.Settings
import io.realm.Realm
import io.realm.kotlin.toFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FilterRepository(private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default) {

    companion object {
        const val TAG = "FilterRepository"
        const val EXCEPTION = "Exception"
    }

    fun filterCriteriaFlow(isGroupCall: Boolean): Flow<FilterCriteria?> {
        val realm = Realm.getDefaultInstance()
        return try {
            val filterCriteriaId = if (isGroupCall) 1 else 2
            realm.where(FilterCriteria::class.java)
                .equalTo(FilterCriteria.COLUMNS.ID, filterCriteriaId)
                .findFirst()
                .toFlow().catch {
                    Log.i(TAG, EXCEPTION)
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            flow { emit(null) }
        } finally {
            realm.close()
        }
    }

    suspend fun getTechnicianPriorityFilterList(): List<CallPriorityFilter> {
        return withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val list = realm.where(CallPriorityFilter::class.java)
                    .equalTo(CallPriorityFilter.COLUMNS.IS_FROM_TECHNICIAN_CALLS, true)
                    .findAll()
                    .sort(CallPriorityFilter.COLUMNS.PRIORITY_NAME)
                realm.copyFromRealm(list)
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                emptyList()
            } finally {
                realm.close()
            }
        }
    }

    suspend fun getGroupPriorityFilterList(): List<CallPriorityFilter> {
        return withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val list = realm.where(CallPriorityFilter::class.java)
                    .equalTo(CallPriorityFilter.COLUMNS.IS_FROM_GROUP_CALLS, true)
                    .findAll()
                    .sort(CallPriorityFilter.COLUMNS.PRIORITY_NAME)
                realm.copyFromRealm(list)
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                emptyList()
            } finally {
                realm.close()
            }
        }
    }


    suspend fun getTechnicianCallTypeList(): List<TechnicianCallType> {
        return withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val list = realm.where(TechnicianCallType::class.java).findAll()
                    .sort(TechnicianCallType.COLUMNS.CALL_TYPE_DESCRIPTION)
                realm.copyFromRealm(list)
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                emptyList()
            } finally {
                realm.close()
            }
        }
    }

    suspend fun getGroupCallTypeList(): List<GroupCallType> {
        return withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val list = realm.where(GroupCallType::class.java)
                    .equalTo(GroupCallType.COLUMNS.ACTIVE, true)
                    .findAll()
                    .sort(GroupCallType.COLUMNS.CALL_TYPE_CODE)
                realm.copyFromRealm(list)
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                emptyList()
            } finally {
                realm.close()
            }
        }
    }

    suspend fun getTechnicianStatusList(): List<StatusFilter> {
        return withContext(defaultDispatcher) {
            listOf(
                StatusFilter(1, R.string.filter_pending, false),
                StatusFilter(2, R.string.filter_dispatch, false),
                StatusFilter(3, R.string.filter_on_hold, false),
                StatusFilter(4, R.string.filter_scheduled, false)
            )
        }
    }

    suspend fun getTechnicianDateList(): List<DateFilter> {
        return withContext(defaultDispatcher) {
            listOf(
                DateFilter(1, R.string.filter_today, false),
                DateFilter(2, R.string.filter_yesterday, false),
                DateFilter(3, R.string.filter_last_seven_days, false),
                DateFilter(4, R.string.filter_last_thirty_days, false)
            )
        }
    }

    suspend fun getTechnicianSortList(): List<CallSortItem> {
        return withContext(defaultDispatcher) {
            listOf(
                CallSortItem(1, R.string.date_received, false),
                CallSortItem(2, R.string.schedule_time, false),
            )
        }
    }

    suspend fun getCallTechnicianForFilterList(): List<CallTechnicianFilter> {
        return withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val list = realm.where(CallTechnicianFilter::class.java)
                    .findAll()
                    .sort(CallTechnicianFilter.COLUMNS.TECHNICIAN_NAME)
                realm.copyFromRealm(list)
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                emptyList()
            } finally {
                realm.close()
            }
        }
    }

    suspend fun saveOpenFiltersForTechnicians(
        sortOpen: Boolean,
        whenOpen: Boolean,
        statusOpen: Boolean,
        priorityOpen: Boolean,
        callTypeOpen: Boolean
    ) {
        withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val filterCriteria = getFilterCriteriaForTechnicianCalls()
                realm.executeTransaction {
                    filterCriteria?.let { filterCriteria ->
                        filterCriteria.isSortByDateOpen = sortOpen
                        filterCriteria.isDateFilterOpen = whenOpen
                        filterCriteria.isCallStatusFilterOpen = statusOpen
                        filterCriteria.isCallPriorityFilterOpen = priorityOpen
                        filterCriteria.isCallTypeFilterOpen = callTypeOpen
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
    }

    suspend fun saveOpenFiltersForGroup(
        sortOpen: Boolean,
        whenOpen: Boolean,
        statusOpen: Boolean,
        priorityOpen: Boolean,
        callTypeOpen: Boolean,
        groupOpen: Boolean,
        techniciansOpen: Boolean
    ) {
        withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            try {
                val filterCriteria = getFilterCriteriaForGroupCalls()
                realm.executeTransaction {
                    filterCriteria?.let { filterCriteria ->
                        filterCriteria.isSortByDateOpen = sortOpen
                        filterCriteria.isDateFilterOpen = whenOpen
                        filterCriteria.isCallStatusFilterOpen = statusOpen
                        filterCriteria.isCallPriorityFilterOpen = priorityOpen
                        filterCriteria.isCallTypeFilterOpen = callTypeOpen
                        filterCriteria.isGroupFilterOpen = groupOpen
                        filterCriteria.isCallTechnicianFilterOpen = techniciansOpen
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
    }


    fun getFilterCriteriaForTechnicianCalls(): FilterCriteria? {
        var filterCriteria: FilterCriteria? = null
        val technicianFilterId = 2

        /**
         * TECHNICIAN ACTIVE FILTER CRITERIA ID = 2
         */
        val localRealm = Realm.getDefaultInstance()
        try {
            filterCriteria =
                localRealm.where(FilterCriteria::class.java)
                    .equalTo(FilterCriteria.COLUMNS.ID, technicianFilterId)
                    .findFirst()
            if (filterCriteria == null) {
                localRealm.executeTransaction { realm1: Realm ->
                    val fCriteria = FilterCriteria(
                        2,
                        filterForGroup = false,
                        filterForServiceOrderList = true,
                        callTypeFilterSelected = "",
                        callTypeIdFilterSelected = -1,
                        callPrioritySelected = "",
                        callPriorityIdSelected = "",
                        callTechnicianNameSelected = "",
                        callTechnicianNumberIdSelected = -1,
                        isDateFilterOpen = true,
                        isGroupFilterOpen = false,
                        isCallTypeFilterOpen = false,
                        isCallPriorityFilterOpen = false,
                        isCallTechnicianFilterOpen = false,
                        isCallStatusFilterOpen = false,
                        isSortByDateOpen = false,
                        callStatusSelected = -1,
                        /**
                         * Default sort is 1 = createDate
                         */
                        callSortItemSelected = 1,
                        callDateSelected = -1
                    )
                    realm1.insertOrUpdate(fCriteria)
                }
                filterCriteria = localRealm.where(FilterCriteria::class.java)
                    .equalTo(FilterCriteria.COLUMNS.ID, technicianFilterId).findFirst()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            localRealm.close()
        }
        return filterCriteria
    }

    fun getFilterCriteriaForGroupCalls(): FilterCriteria? {
        var filterCriteria: FilterCriteria?
        val groupFilterCriteriaId = 1

        /**
         * GroupCalls FILTER CRITERIA ID = 1
         */
        val localRealm = Realm.getDefaultInstance()
        filterCriteria =
            localRealm.where(FilterCriteria::class.java)
                .equalTo(FilterCriteria.COLUMNS.ID, groupFilterCriteriaId)
                .findFirst()
        try {
            if (filterCriteria == null) {
                localRealm.executeTransaction { realm1: Realm ->
                    val fCriteria = FilterCriteria(
                        1,
                        filterForGroup = true,
                        filterForServiceOrderList = false,
                        callTypeFilterSelected = "",
                        callTypeIdFilterSelected = -1,
                        callPrioritySelected = "",
                        callPriorityIdSelected = "",
                        callTechnicianNameSelected = "",
                        callTechnicianNumberIdSelected = -1,
                        isDateFilterOpen = true,
                        isGroupFilterOpen = false,
                        isCallTypeFilterOpen = false,
                        isCallPriorityFilterOpen = false,
                        isCallTechnicianFilterOpen = false,
                        isCallStatusFilterOpen = false,
                        isSortByDateOpen = false,
                        callStatusSelected = -1,
                        callSortItemSelected = 1,
                        callDateSelected = -1
                    )
                    realm1.insertOrUpdate(fCriteria)
                }
                filterCriteria = localRealm.where(FilterCriteria::class.java)
                    .equalTo(FilterCriteria.COLUMNS.ID, groupFilterCriteriaId).findFirst()
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            localRealm.close()
        }
        return filterCriteria
    }

    suspend fun saveCheckedFiltersForTechnician(
        checkedDate: DateFilter?,
        checkedSort: CallSortItem?,
        checkedCallType: CallTypeFilter?,
        checkedStatus: StatusFilter?,
        checkedPriority: PriorityFilter?
    ) {
        withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            val filterCriteria = getFilterCriteriaForTechnicianCalls()
            try {
                realm.executeTransaction {
                    filterCriteria?.callPriorityIdSelected = checkedPriority?.priorityId
                    filterCriteria?.callPrioritySelected = checkedPriority?.priorityName
                    filterCriteria?.callStatusSelected = checkedStatus?.id ?: -1
                    filterCriteria?.callTypeIdFilterSelected = checkedCallType?.callTypeId ?: -1
                    filterCriteria?.callTypeFilterSelected = checkedCallType?.callTypeDescription
                    filterCriteria?.callDateSelected = checkedDate?.id ?: -1
                    filterCriteria?.callSortItemSelected = checkedSort?.id ?: -1
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }

    }

    suspend fun saveCheckedFiltersForGroups(
        checkedDate: DateFilter?,
        checkedSort: CallSortItem?,
        checkedCallType: CallTypeFilter?,
        checkedStatus: StatusFilter?,
        checkedPriority: PriorityFilter?,
        checkedTechnician: TechnicianFilter?,
        checkedGroup: GroupFilter?
    ) {
        withContext(defaultDispatcher) {
            val realm = Realm.getDefaultInstance()
            val filterCriteria = getFilterCriteriaForGroupCalls()
            try {
                realm.executeTransaction {
                    filterCriteria?.callPriorityIdSelected = checkedPriority?.priorityId
                    filterCriteria?.callPrioritySelected = checkedPriority?.priorityName
                    filterCriteria?.callStatusSelected = checkedStatus?.id ?: -1
                    filterCriteria?.callTypeIdFilterSelected = checkedCallType?.callTypeId ?: -1
                    filterCriteria?.callTypeFilterSelected = checkedCallType?.callTypeDescription
                    filterCriteria?.callDateSelected = checkedDate?.id ?: -1
                    filterCriteria?.callSortItemSelected = checkedSort?.id ?: -1
                    filterCriteria?.callTechnicianNameSelected = checkedTechnician?.technicianName
                    filterCriteria?.callTechnicianNumberIdSelected =
                        checkedTechnician?.technicianNumberId ?: -1
                    filterCriteria?.groupNameSelected = checkedGroup?.groupName
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }

    }

    suspend fun deleteFilterTechnicianCriteria() {
        withContext(defaultDispatcher) {
            val localRealm = Realm.getDefaultInstance()
            try {
                localRealm.executeTransaction { realm1: Realm ->
                    val fCriteria = FilterCriteria(
                        2,
                        filterForGroup = false,
                        filterForServiceOrderList = true,
                        callTypeFilterSelected = "",
                        callTypeIdFilterSelected = -1,
                        callPrioritySelected = "",
                        callPriorityIdSelected = "",
                        callTechnicianNameSelected = "",
                        callTechnicianNumberIdSelected = -1,
                        isDateFilterOpen = true,
                        isGroupFilterOpen = false,
                        isCallTypeFilterOpen = false,
                        isCallPriorityFilterOpen = false,
                        isCallTechnicianFilterOpen = false,
                        isCallStatusFilterOpen = false,
                        isSortByDateOpen = false,
                        callStatusSelected = -1,
                        /**
                         * Default Sort selected is 1 = Create Date
                         */
                        callSortItemSelected = 1,
                        callDateSelected = -1
                    )
                    realm1.insertOrUpdate(fCriteria)
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                localRealm.close()
            }
        }
    }

    suspend fun deleteFilterGroupCriteria() {
        withContext(defaultDispatcher) {
            val localRealm = Realm.getDefaultInstance()
            try {
                localRealm.executeTransaction { realm1: Realm ->
                    val fCriteria = FilterCriteria(
                        1,
                        filterForGroup = true,
                        filterForServiceOrderList = false,
                        callTypeFilterSelected = "",
                        callTypeIdFilterSelected = -1,
                        callPrioritySelected = "",
                        callPriorityIdSelected = "",
                        callTechnicianNameSelected = "",
                        callTechnicianNumberIdSelected = -1,
                        isDateFilterOpen = true,
                        isGroupFilterOpen = false,
                        isCallTypeFilterOpen = false,
                        isCallPriorityFilterOpen = false,
                        isCallTechnicianFilterOpen = false,
                        isCallStatusFilterOpen = false,
                        isSortByDateOpen = false,
                        callStatusSelected = -1,
                        callSortItemSelected = 1,
                        callDateSelected = -1,
                        groupNameSelected = ""
                    )
                    realm1.insertOrUpdate(fCriteria)
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                localRealm.close()
            }
        }
    }


    fun getGroupsForTechnician(techId: String): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getGroupByUserId2(techId) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    fun createGroupsList(responseBody: String): List<TechnicianGroup> {
        var list = listOf<TechnicianGroup>()
        return try {
            val listOfGroups = object : TypeToken<MutableList<TechnicianGroup>>() {}.type
            list = Settings.createGson().fromJson(responseBody, listOfGroups)
            list
        } catch (e: Exception) {
            Log.e(CodesRepository.TAG, CodesRepository.EXCEPTION, e)
            list
        }
    }


}