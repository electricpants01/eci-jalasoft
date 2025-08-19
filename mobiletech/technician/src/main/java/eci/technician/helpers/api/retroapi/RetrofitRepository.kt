package eci.technician.helpers.api.retroapi

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ErrorHelper.RequestCodeHandler
import eci.technician.helpers.ErrorHelper.RequestError
import eci.technician.helpers.api.UpdateManager
import eci.technician.models.*
import eci.technician.models.create_call.CallType
import eci.technician.models.create_call.CreateSC
import eci.technician.models.create_call.CustomerItem
import eci.technician.models.create_call.EquipmentItem
import eci.technician.models.data.UsedPart
import eci.technician.models.equipment.EquipmentSearchModel
import eci.technician.models.field_transfer.PartRequestTransfer
import eci.technician.models.filters.CallPriorityFilter
import eci.technician.models.filters.CallTechnicianFilter
import eci.technician.models.filters.GroupCallType
import eci.technician.models.gps.CarInfo
import eci.technician.models.gps.UpdatePosition
import eci.technician.models.order.*
import eci.technician.models.parts.postModels.PartToDeletePostModel
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity
import eci.technician.models.serviceCallNotes.postModels.CreateNotePostModel
import eci.technician.models.serviceCallNotes.postModels.DeleteNotePostModel
import eci.technician.models.serviceCallNotes.postModels.UpdateNotePostModel
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteResponse
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteTypeResponse
import eci.technician.models.transfers.CreateTransferModel
import eci.technician.repository.*
import eci.technician.repository.ServiceOrderRepository.getOneServiceCallByIdAndSave
import eci.technician.tools.Settings
import eci.technician.workers.serviceOrderQueue.ServiceOrderOfflineUtils
import io.realm.Realm
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.UnknownHostException

class RetrofitRepository {


    companion object {
        val NOT_FOUND_CODE = 404
        const val TAG = "RetrofitRepository"
        const val EXCEPTION = "Exception"
    }

    object RetrofitRepositoryObject {
        private var myInstance: RetrofitRepository? = null
        fun getInstance(): RetrofitRepository {
            return if (myInstance != null) {
                myInstance as RetrofitRepository
            } else {
                RetrofitRepository()
            }
        }
    }

    fun getAllIncompleteCodes(): MutableLiveData<MutableList<IncompleteCode>> {
        val mIncompleteCodes: MutableLiveData<MutableList<IncompleteCode>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getAllIncompleteCodes()
            ?.enqueue(object : Callback<MutableList<IncompleteCode>> {
                override fun onResponse(
                    call: Call<MutableList<IncompleteCode>>,
                    response: Response<MutableList<IncompleteCode>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { incompleteListFromResponse ->
                            val realm = Realm.getDefaultInstance()
                            val resultsIncomplete = realm.where(IncompleteCode::class.java)
                                .equalTo(IncompleteCode.COLUMNS.IS_CHECKED, true).findAll()
                            realm.executeTransaction {
                                incompleteListFromResponse.forEach { incomplete ->
                                    resultsIncomplete.forEach { incompleteFirst ->
                                        if (incomplete.incompleteCodeId == incompleteFirst.incompleteCodeId) {
                                            incomplete.isChecked = true
                                        }
                                    }
                                }
                                realm.insertOrUpdate(incompleteListFromResponse)
                            }
                            realm.close()
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<IncompleteCode>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mIncompleteCodes
    }


    fun getTechnicianActiveServiceCallsFlow(forceUpdate: Boolean = false): Flow<Resource<MutableList<ServiceOrder>>> {
        return flow {
            val configuration = LastUpdateRepository.LastUpdateKeys.activeServiceCallsConfiguration()
            val lastUpdate = if (forceUpdate) "" else LastUpdateRepository.getLastUpdateString(configuration)
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getAllTechnicianActiveServiceCallsSuspend(lastUpdate) }
            if (resource is Resource.Success) {
                val list = resource.data ?: mutableListOf()
                UpdateManager.updateServiceCalls(list, configuration, lastUpdate)
            }
            emit(resource)
        }.catch {
            emit(Resource.Error("", null, Pair(ErrorType.SOMETHING_WENT_WRONG, "Unexpected error")))
        }.flowOn(Dispatchers.IO)
    }

    fun getGroupsByUserId(technicianId: String): MutableLiveData<MutableList<TechnicianGroup>> {
        val mGroups: MutableLiveData<MutableList<TechnicianGroup>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getGroupByUserId(technicianId)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (!it.isHasError) {
                                val listOfTechGroupsType =
                                    object : TypeToken<MutableList<TechnicianGroup>>() {}.type
                                if (it.result != null && it.result.isNotEmpty()) {
                                    val groupList: MutableList<TechnicianGroup> =
                                        Settings.createGson()
                                            .fromJson(it.result, listOfTechGroupsType)
                                    mGroups.value = groupList
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, response.message())
                        mGroups.value = mutableListOf()
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                    mGroups.value = mutableListOf()
                }
            })
        return mGroups
    }

    fun getGroupsByUserId2(
        technicianId: String,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<TechnicianGroup>>> {
        val mGroups: MutableLiveData<GenericDataResponse<MutableList<TechnicianGroup>>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getGroupByUserId(technicianId)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (!it.isHasError) {
                                val listOfTechGroupsType =
                                    object : TypeToken<MutableList<TechnicianGroup>>() {}.type
                                if (it.result != null && it.result.isNotEmpty()) {
                                    val groupList: MutableList<TechnicianGroup> =
                                        Settings.createGson()
                                            .fromJson(it.result, listOfTechGroupsType)
                                    mGroups.value =
                                        GenericDataResponse(RequestStatus.SUCCESS, groupList, null)
                                }
                            } else {
                                mGroups.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(response, it)
                                )
                            }
                        }
                    } else {
                        Log.d(TAG, response.message())
                        mGroups.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                    mGroups.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGroups
    }

    fun getGroupServiceCallListWithFilterType(
        filterType: Int,
        groupName: String?,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<GroupCallServiceOrder>>> {
        val mGroupServiceCall: MutableLiveData<GenericDataResponse<MutableList<GroupCallServiceOrder>>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getGroupServiceCallListWithFilterType(filterType, groupName)
            ?.enqueue(object : Callback<MutableList<GroupCallServiceOrder>> {
                override fun onResponse(
                    call: Call<MutableList<GroupCallServiceOrder>>,
                    response: Response<MutableList<GroupCallServiceOrder>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { serviceOrderListFromResponse ->
                            GlobalScope.launch {
                                withContext(Dispatchers.IO) {
                                    val prioritiesList = (serviceOrderListFromResponse.map {
                                        CallPriorityFilter(
                                            "GROUP_CALL_${it.callPriority}",
                                            true,
                                            isFromTechnicianCalls = false,
                                            priorityName = it.callPriority,
                                            isChecked = false
                                        )
                                    }).distinctBy { it.priorityName }
                                    val technicianList = serviceOrderListFromResponse.map {
                                        CallTechnicianFilter(
                                            it.technicianName,
                                            it.technicianNumberId
                                        )
                                    }.distinctBy { it.technicianNumberId }
                                    //here
                                    val realm = Realm.getDefaultInstance()
                                    val callTypes = realm.where(CallType::class.java).findAll()
                                    val callTypesFiltered = callTypes.filter {
                                        serviceOrderListFromResponse.map { groupCallServiceOrder -> groupCallServiceOrder.callType }
                                            .contains(it.callTypeDescription)
                                    }
                                    val groupCallType = callTypesFiltered.map {
                                        GroupCallType(
                                            it.id,
                                            it.active,
                                            it.callTypeCode,
                                            it.callTypeDescription,
                                            it.callTypeId,
                                            it.companyId,
                                            false
                                        )
                                    }
                                    realm.executeTransaction {
                                        realm.delete(GroupCallServiceOrder::class.java)
                                        realm.insertOrUpdate(serviceOrderListFromResponse)

                                        realm.where(GroupCallType::class.java).findAll()
                                            .deleteAllFromRealm()
                                        realm.insertOrUpdate(groupCallType)

                                        realm.where(CallPriorityFilter::class.java)
                                            .equalTo(
                                                CallPriorityFilter.COLUMNS.IS_FROM_GROUP_CALLS,
                                                true
                                            )
                                            .findAll().deleteAllFromRealm()
                                        realm.insertOrUpdate(prioritiesList)

                                        realm.delete(CallTechnicianFilter::class.java)
                                        realm.insert(technicianList)
                                    }
                                    realm.close()
                                    withContext(Dispatchers.Main) {
                                        mGroupServiceCall.value = GenericDataResponse(
                                            RequestStatus.SUCCESS,
                                            serviceOrderListFromResponse,
                                            null
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        mGroupServiceCall.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(
                    call: Call<MutableList<GroupCallServiceOrder>>,
                    t: Throwable
                ) {
                    Log.d(TAG, t.message.toString())
                    mGroupServiceCall.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGroupServiceCall
    }

    fun getHoldCodes(): MutableLiveData<MutableList<HoldCode>> {
        val mHolCodeList: MutableLiveData<MutableList<HoldCode>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getHoldCodes()
            ?.enqueue(object : Callback<MutableList<HoldCode>> {
                override fun onResponse(
                    call: Call<MutableList<HoldCode>>,
                    response: Response<MutableList<HoldCode>>
                ) {
                    if (response.isSuccessful) {
                        val holdCodesFromApi = mutableListOf<Int>()
                        response.body()?.let { holdCodes ->
                            val realm = Realm.getDefaultInstance()
                            try {
                                for (holdCode in holdCodes) {
                                    realm.executeTransaction {
                                        holdCodesFromApi.add(holdCode.onHoldCodeId)
                                        it.insertOrUpdate(holdCode)
                                    }
                                }
                                val holdCodesToDelete = realm.where(HoldCode::class.java)
                                    .not()
                                    .`in`(HoldCode.ON_HOLD_CODE_ID, holdCodesFromApi.toTypedArray())
                                    .findAll()
                                holdCodesToDelete.deleteAllFromRealm()
                            } catch (e: Exception) {
                                Log.e("RetrofitRepository", e.message.toString())
                            } finally {
                                realm.close()
                            }
                            mHolCodeList.value = holdCodes
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<HoldCode>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                    mHolCodeList.value = mutableListOf()
                }
            })
        return mHolCodeList
    }

    fun reassignServiceCall(map: HashMap<String, Any>): MutableLiveData<ProcessingResult?> {
        val mProcessingResult: MutableLiveData<ProcessingResult?> = MutableLiveData()
        RetrofitApiHelper.getApi()?.reassignServiceCall(map)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            mProcessingResult.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mProcessingResult
    }


    fun getAllEquipmentMeters(): MutableLiveData<MutableList<EquipmentRealmMeter>> {
        val mAllEquipmentMeters: MutableLiveData<MutableList<EquipmentRealmMeter>> =
            MutableLiveData()
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitApiHelper.getApi()
                    ?.getAllEquipmentMeters(AppAuth.getInstance().technicianUser.technicianNumber)
                if (response?.isSuccessful == true) {
                    response.body()?.let { allEquipmentMeters ->
                        val realm = Realm.getDefaultInstance()
                        try {
                            realm.executeTransaction {
                                realm.delete(EquipmentRealmMeter::class.java)
                                realm.insertOrUpdate(allEquipmentMeters)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, EXCEPTION, e)
                        } finally {
                            realm.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            }
        }

        return mAllEquipmentMeters
    }

    fun getAllCallTypes(): MutableLiveData<MutableList<CallType>> {
        val mCallTypesList: MutableLiveData<MutableList<CallType>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getAllCallTypes()
            ?.enqueue(object : Callback<MutableList<CallType>> {
                override fun onResponse(
                    call: Call<MutableList<CallType>>,
                    response: Response<MutableList<CallType>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { callTypeList ->
                            val realm = Realm.getDefaultInstance()
                            try {
                                realm.executeTransaction {
                                    realm.insertOrUpdate(callTypeList)
                                }
                            } catch (e: Exception) {

                            } finally {
                                realm.close()
                            }
                            mCallTypesList.value = callTypeList
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<CallType>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mCallTypesList
    }

    fun getCustomerDataByText(searchText: String): MutableLiveData<MutableList<CustomerItem>> {
        val mCustomerItemList: MutableLiveData<MutableList<CustomerItem>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getCustomerDataByText(searchText)
            ?.enqueue(object : Callback<MutableList<CustomerItem>> {
                override fun onResponse(
                    call: Call<MutableList<CustomerItem>>,
                    response: Response<MutableList<CustomerItem>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            mCustomerItemList.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<CustomerItem>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mCustomerItemList
    }

    fun getEquipmentDataByText(searchText: String): MutableLiveData<MutableList<EquipmentItem>> {
        val mEquipmentItemList: MutableLiveData<MutableList<EquipmentItem>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getEquipmentsDataByText(searchText)
            ?.enqueue(object : Callback<MutableList<EquipmentItem>> {
                override fun onResponse(
                    call: Call<MutableList<EquipmentItem>>,
                    response: Response<MutableList<EquipmentItem>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            mEquipmentItemList.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<EquipmentItem>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mEquipmentItemList
    }

    fun createServiceCall(createSC: CreateSC): MutableLiveData<ProcessingResult> {
        val mProcessingResult: MutableLiveData<ProcessingResult> = MutableLiveData()
        RetrofitApiHelper.getApi()?.createNewServiceCall(createSC)
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            mProcessingResult.value = it
                            if (!it.isHasError) {
                                if (AppAuth.getInstance().technicianUser.isRestrictCallOrder) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        getTechnicianActiveServiceCallsFlow(forceUpdate = true).collect { }
                                    }
                                } else {
                                    ServiceOrderOfflineUtils.saveUpdatedServiceCall(
                                        it.result,
                                        updateCustomerWarehousePart = true,
                                        updateEquipmentMeters = true
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mProcessingResult
    }

    fun getServiceCallProblemCodes(): MutableLiveData<MutableList<ProblemCode>> {
        val mProblemCodeList: MutableLiveData<MutableList<ProblemCode>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getServiceCalProblemCodes()
            ?.enqueue(object : Callback<MutableList<ProblemCode>> {
                override fun onResponse(
                    call: Call<MutableList<ProblemCode>>,
                    response: Response<MutableList<ProblemCode>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { problemCodeList ->
                            GlobalScope.launch {
                                withContext(Dispatchers.IO) {
                                    run {
                                        val realm = Realm.getDefaultInstance()
                                        try {
                                            realm.executeTransaction {
                                                realm.delete(ProblemCode::class.java)
                                                realm.insertOrUpdate(problemCodeList)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, EXCEPTION, e)
                                        } finally {
                                            realm.close()
                                        }
                                        withContext(Dispatchers.Main) {
                                            mProblemCodeList.value = problemCodeList
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<ProblemCode>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mProblemCodeList
    }

    fun getServiceCallRepairCodes(): MutableLiveData<MutableList<RepairCode>> {
        val mRepairCodeList: MutableLiveData<MutableList<RepairCode>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getServiceCallRepairCodes()
            ?.enqueue(object : Callback<MutableList<RepairCode>> {
                override fun onResponse(
                    call: Call<MutableList<RepairCode>>,
                    response: Response<MutableList<RepairCode>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { repairCodeList ->
                            GlobalScope.launch {
                                withContext(Dispatchers.IO) {
                                    run {
                                        val realm = Realm.getDefaultInstance()
                                        try {
                                            realm.executeTransaction {
                                                realm.delete(RepairCode::class.java)
                                                realm.insertOrUpdate(repairCodeList)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, EXCEPTION, e)
                                        } finally {
                                            realm.close()
                                        }
                                        withContext(Dispatchers.Main) {
                                            mRepairCodeList.value = repairCodeList
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<RepairCode>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mRepairCodeList
    }

    fun getAllActivityCallTypes(): MutableLiveData<ProcessingResult> {
        val mAllActivitiesCallTypesResult: MutableLiveData<ProcessingResult> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getAllActivityCallTypes()
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { responseProcessingResult ->
                            if (!responseProcessingResult.isHasError) {
                                if (responseProcessingResult.result != null && responseProcessingResult.result.isNotEmpty()) {
                                    GlobalScope.launch {
                                        withContext(Dispatchers.IO) {
                                            run {
                                                val realm = Realm.getDefaultInstance()
                                                try {
                                                    realm.executeTransaction {
                                                        val listOfActivityCallTypes = object :
                                                            TypeToken<MutableList<ActivityCode>>() {}.type
                                                        val activityCodeList: MutableList<ActivityCode> =
                                                            Settings.createGson().fromJson(
                                                                responseProcessingResult.result,
                                                                listOfActivityCallTypes
                                                            )
                                                        realm.delete(ActivityCode::class.java)
                                                        realm.insertOrUpdate(activityCodeList)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG, EXCEPTION, e)
                                                } finally {
                                                    realm.close()
                                                }
                                                withContext(Dispatchers.Main) {
                                                    mAllActivitiesCallTypesResult.value =
                                                        responseProcessingResult
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mAllActivitiesCallTypesResult
    }

    fun getCancelCodes(): MutableLiveData<MutableList<CancelCode>> {
        val mCancelCodesList: MutableLiveData<MutableList<CancelCode>> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getCancelCodes()
            ?.enqueue(object : Callback<MutableList<CancelCode>> {
                override fun onResponse(
                    call: Call<MutableList<CancelCode>>,
                    response: Response<MutableList<CancelCode>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { cancelCodeList ->
                            GlobalScope.launch {
                                withContext(Dispatchers.IO) {
                                    run {
                                        val realm = Realm.getDefaultInstance()
                                        try {
                                            realm.executeTransaction {
                                                realm.delete(CancelCode::class.java)
                                                realm.insertOrUpdate(cancelCodeList)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, EXCEPTION, e)
                                        } finally {
                                            realm.close()
                                        }
                                        withContext(Dispatchers.Main) {
                                            mCancelCodesList.value = cancelCodeList
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<CancelCode>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mCancelCodesList
    }

    fun getEquipmentMetersByEquipmentId(equipmentId: Int): MutableLiveData<MutableList<EquipmentRealmMeter>> {
        val mEquipmentMetersList: MutableLiveData<MutableList<EquipmentRealmMeter>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getEquipmentMeterByEquipmentId(equipmentId)
            ?.enqueue(object : Callback<MutableList<EquipmentRealmMeter>> {
                override fun onResponse(
                    call: Call<MutableList<EquipmentRealmMeter>>,
                    response: Response<MutableList<EquipmentRealmMeter>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { metersList ->
                            val realm = Realm.getDefaultInstance()
                            try {
                                realm.executeTransaction {
                                    realm.insertOrUpdate(metersList)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, EXCEPTION, e)
                            } finally {
                                realm.close()
                            }
                            mEquipmentMetersList.value = metersList
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<EquipmentRealmMeter>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }

            })
        return mEquipmentMetersList
    }

    fun getTechnicianCompetedServiceCalls(
        applicationContext: Context,
        scope: CoroutineScope
    ): MutableLiveData<GenericDataResponse<MutableList<CompletedServiceOrder>>> {
        val mCompletedServiceCallsList: MutableLiveData<GenericDataResponse<MutableList<CompletedServiceOrder>>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getTechnicianCompletedServiceCalls()
            ?.enqueue(object : Callback<MutableList<CompletedServiceOrder>?> {
                override fun onResponse(
                    call: Call<MutableList<CompletedServiceOrder>?>,
                    response: Response<MutableList<CompletedServiceOrder>?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { completedServiceCallsList ->
                            scope.launch(Dispatchers.IO) {
                                CompletedCallsRepository.persistCompletedCalls(
                                    completedServiceCallsList
                                )
                                withContext(Dispatchers.Main) {
                                    mCompletedServiceCallsList.value = GenericDataResponse(
                                        RequestStatus.SUCCESS,
                                        completedServiceCallsList,
                                        null
                                    )
                                }
                            }
                        }
                    } else {
                        mCompletedServiceCallsList.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(
                    call: Call<MutableList<CompletedServiceOrder>?>,
                    t: Throwable
                ) {
                    Log.d(TAG, t.message.toString())
                    mCompletedServiceCallsList.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, applicationContext)
                    )
                }
            })
        return mCompletedServiceCallsList
    }

    fun getPartsRequestsFromMe(): MutableLiveData<MutableList<PartRequestTransfer>> {
        val mPartRequestsFromMe: MutableLiveData<MutableList<PartRequestTransfer>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getPartsRequestsFromMe()
            ?.enqueue(object : Callback<MutableList<PartRequestTransfer>> {
                override fun onResponse(
                    call: Call<MutableList<PartRequestTransfer>>,
                    response: Response<MutableList<PartRequestTransfer>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            mPartRequestsFromMe.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<MutableList<PartRequestTransfer>>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return mPartRequestsFromMe
    }


    fun getEquipmentHistoryByEquipmentId(
        equipmentId: String,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<EquipmentHistoryModel>>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<MutableList<EquipmentHistoryModel>>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getEquipmentHistoryByEquipmentId(equipmentId)
            ?.enqueue(object : Callback<MutableList<EquipmentHistoryModel>> {
                override fun onResponse(
                    call: Call<MutableList<EquipmentHistoryModel>>,
                    response: Response<MutableList<EquipmentHistoryModel>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            Log.d(TAG, it.toString())
                            mGenericDataResponse.value =
                                GenericDataResponse(RequestStatus.SUCCESS, it, null)
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(
                    call: Call<MutableList<EquipmentHistoryModel>>,
                    t: Throwable
                ) {
                    Log.d(TAG, t.message.toString())
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun updateEquipmentHistoryByEquipmentId(
        equipmentId: String,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<EquipmentHistoryModel>>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<MutableList<EquipmentHistoryModel>>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.updateEquipmentHistoryByEquipmentId(equipmentId)
            ?.enqueue(object : Callback<MutableList<EquipmentHistoryModel>> {
                override fun onResponse(
                    call: Call<MutableList<EquipmentHistoryModel>>,
                    response: Response<MutableList<EquipmentHistoryModel>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            Log.d(TAG, it.toString())
                            mGenericDataResponse.value =
                                GenericDataResponse(RequestStatus.SUCCESS, it, null)
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(
                    call: Call<MutableList<EquipmentHistoryModel>>,
                    t: Throwable
                ) {
                    Log.d(TAG, t.message.toString())
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun getEquipmentByText(
        searchText: String,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<EquipmentSearchModel>>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<MutableList<EquipmentSearchModel>>> =
            MutableLiveData()
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                run {
                    RetrofitApiHelper.getApi()?.getEquipmentByText(searchText)
                        ?.enqueue(object : Callback<MutableList<EquipmentSearchModel>> {
                            override fun onResponse(
                                call: Call<MutableList<EquipmentSearchModel>>,
                                response: Response<MutableList<EquipmentSearchModel>>
                            ) {
                                if (response.isSuccessful) {
                                    response.body()?.let {
                                        mGenericDataResponse.value =
                                            GenericDataResponse(RequestStatus.SUCCESS, it, null)
                                    }
                                } else {
                                    mGenericDataResponse.value = GenericDataResponse(
                                        RequestStatus.ERROR,
                                        null,
                                        RequestCodeHandler.getMessageErrorFromResponse(
                                            response,
                                            null
                                        )
                                    )
                                }
                            }

                            override fun onFailure(
                                call: Call<MutableList<EquipmentSearchModel>>,
                                t: Throwable
                            ) {
                                Log.d(TAG, t.message.toString())
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorOnFailure(t, context)
                                )
                            }
                        })
                }
            }
        }
        return mGenericDataResponse
    }

    fun addAssistance(
        map: HashMap<String, Any>,
        context: Context
    ): MutableLiveData<GenericDataResponse<ServiceOrder>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ServiceOrder>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.addAssistance(map)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.SUCCESS,
                                Settings.createGson().fromJson(it.result, ServiceOrder::class.java),
                                null
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }


    fun updateLaborForAssist(
        context: Context,
        updateLaborPostModel: UpdateLaborPostModel,
        callId: Int
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                RetrofitApiHelper.getApi()?.updateLaborForAssist(updateLaborPostModel)
                    ?.enqueue(object : Callback<ProcessingResult> {
                        override fun onResponse(
                            call: Call<ProcessingResult>,
                            response: Response<ProcessingResult>
                        ) {
                            if (response.isSuccessful) {
                                response.body()?.let {
                                    if (it.isHasError) {
                                        mGenericDataResponse.value = GenericDataResponse(
                                            RequestStatus.ERROR,
                                            null,
                                            RequestCodeHandler.getMessageErrorFromResponse(
                                                response,
                                                it
                                            )
                                        )
                                    } else {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            getOneServiceCallByIdAndSave(callId).collect {  }
                                        }
                                        mGenericDataResponse.value = GenericDataResponse(
                                            RequestStatus.SUCCESS,
                                            it,
                                            null
                                        )
                                    }
                                }
                            } else {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(response, null)
                                )
                            }
                        }

                        override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestCodeHandler.getMessageErrorOnFailure(t, context)
                            )
                        }
                    })
            }


        }
        return mGenericDataResponse
    }


    fun getOneServiceCallByCallId(
        callId: Int,
        scope: CoroutineScope,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<ServiceOrder>>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<MutableList<ServiceOrder>>> =
            MutableLiveData()

        scope.launch(Dispatchers.IO) {
            RetrofitApiHelper.getApi()?.getOneServiceCallById(callId)
                ?.enqueue(object : Callback<MutableList<ServiceOrder>> {
                    override fun onResponse(
                        call: Call<MutableList<ServiceOrder>>,
                        response: Response<MutableList<ServiceOrder>>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                if (!it.isNullOrEmpty()) {
                                    val serviceCallFromResponse = it.first()
                                    val currentUserTechId =
                                        AppAuth.getInstance().technicianUser.technicianNumber
                                    if (serviceCallFromResponse.technicianNumberId == currentUserTechId
                                        || serviceCallFromResponse.labors.map { labor -> labor.technicianId }
                                            .contains(currentUserTechId)
                                    ) {
                                        ServiceOrderRepository.saveServiceOrderFromResponse(
                                            serviceCallFromResponse,
                                            false
                                        )
                                    } else {
                                        PartsRepository.deleteAllPartsByOrderId(callId, scope)
                                    }
                                } else {
                                    PartsRepository.deleteAllPartsByOrderId(callId, scope)
                                }
                                mGenericDataResponse.value =
                                    GenericDataResponse(RequestStatus.SUCCESS, it, null)
                            }
                        } else {
                            val error =
                                RequestCodeHandler.getMessageErrorFromResponse(response, null)
                            mGenericDataResponse.value =
                                GenericDataResponse(RequestStatus.ERROR, null, error)
                        }
                    }

                    override fun onFailure(call: Call<MutableList<ServiceOrder>>, t: Throwable) {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorOnFailure(t, context)
                        )
                    }
                })
        }
        return mGenericDataResponse
    }


    fun getOneServiceCallByCallIdSync(
        callId: Int,
        onSuccess: (serviceOrder: ServiceOrder) -> Unit,
        onReassigned: (callId: Int) -> Unit,
        onUnavailable: (callId: Int) -> Unit
    ) {
        val response = RetrofitApiHelper.getApi()?.getOneServiceCallById(callId)?.execute()
        if (response?.isSuccessful == true) {
            response.body()?.let {
                if (!it.isNullOrEmpty()) {
                    val serviceCallFromResponse = it.first()
                    val currentUserTechId =
                        AppAuth.getInstance().technicianUser.technicianNumber
                    if (serviceCallFromResponse.technicianNumberId == currentUserTechId
                        || serviceCallFromResponse.labors.map { labor -> labor.technicianId }
                            .contains(currentUserTechId)
                    ) {
                        onSuccess.invoke(serviceCallFromResponse)
                    } else {
                        /**
                         * If this SC is not mine -> delete from DB
                         */
                        onReassigned.invoke(callId)
                    }
                } else {
                    /**
                     * If the response is empty -> delete from DB
                     */
                    onUnavailable.invoke(callId)
                }
            }
        }
    }

    fun getOneGroupCallServiceByCallId(
        callId: Int,
        scope: CoroutineScope,
        context: Context
    ): MutableLiveData<GenericDataResponse<MutableList<GroupCallServiceOrder>>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<MutableList<GroupCallServiceOrder>>> =
            MutableLiveData()

        scope.launch(Dispatchers.IO) {
            RetrofitApiHelper.getApi()?.getOneGroupCallServiceByCallId(callId)
                ?.enqueue(object : Callback<MutableList<GroupCallServiceOrder>> {
                    override fun onResponse(
                        call: Call<MutableList<GroupCallServiceOrder>>,
                        response: Response<MutableList<GroupCallServiceOrder>>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                mGenericDataResponse.value =
                                    GenericDataResponse(RequestStatus.SUCCESS, it, null)
                            }
                        } else {
                            val error =
                                RequestCodeHandler.getMessageErrorFromResponse(response, null)
                            mGenericDataResponse.value =
                                GenericDataResponse(RequestStatus.ERROR, null, error)
                        }
                    }

                    override fun onFailure(
                        call: Call<MutableList<GroupCallServiceOrder>>,
                        t: Throwable
                    ) {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorOnFailure(t, context)
                        )
                    }
                })
        }
        return mGenericDataResponse
    }

    suspend fun getServiceCallNotesByCallId(
        callId: Int,
        context: Context,
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()

        RetrofitApiHelper.getApi()?.getServiceCallNotesByCallId(callId)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            if (processingResult.isHasError) {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(
                                        response,
                                        processingResult
                                    )
                                )
                            } else {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        run {
                                            var listOfNotes: MutableList<ServiceCallNoteResponse> =
                                                mutableListOf(
                                                    *Settings.createGson()
                                                        .fromJson(
                                                            processingResult.result,
                                                            Array<ServiceCallNoteResponse>::class.java
                                                        )
                                                )
                                            if (listOfNotes.isNullOrEmpty()) {
                                                listOfNotes = mutableListOf()
                                            }
                                            val listTosave = listOfNotes.map { note ->
                                                ServiceCallNoteEntity.convertToNoteEntity(note)
                                            }
                                            ServiceCallNotesRepository.saveNotes(listTosave)
                                            withContext(Dispatchers.Main) {
                                                mGenericDataResponse.value =
                                                    GenericDataResponse(
                                                        RequestStatus.SUCCESS,
                                                        processingResult,
                                                        null
                                                    )
                                            }
                                        }

                                    }
                                }
                            }
                        } ?: kotlin.run {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestError("asdf", "aaa")
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    when (t) {
                        is UnknownHostException -> {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.NOT_CONNECTED,
                                null,
                                RequestCodeHandler.getMessageErrorOnFailure(t, context)
                            )
                        }
                        else -> {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestCodeHandler.getMessageErrorOnFailure(t, context)
                            )
                        }
                    }
                }
            })
        return mGenericDataResponse
    }


    fun updateServiceCallNote(
        updateNotePostModel: UpdateNotePostModel,
        context: Context,
        customUUID: String
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()

        RetrofitApiHelper.getApi()?.updateServiceCallNote(updateNotePostModel)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            if (processingResult.isHasError) {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(
                                        response,
                                        processingResult
                                    )
                                )
                            } else {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        run {
                                            var listOfNotes: MutableList<ServiceCallNoteResponse> =
                                                mutableListOf(
                                                    *Settings.createGson()
                                                        .fromJson(
                                                            processingResult.result,
                                                            Array<ServiceCallNoteResponse>::class.java
                                                        )
                                                )
                                            if (listOfNotes.isNullOrEmpty()) {
                                                listOfNotes = mutableListOf()
                                            }
                                            val listTosave = listOfNotes.map { note ->
                                                ServiceCallNoteEntity.convertToNoteEntity(note)
                                            }
                                            ServiceCallNotesRepository.saveNoteFromResponse(
                                                listTosave,
                                                customUUID
                                            )
                                            withContext(Dispatchers.Main) {
                                                mGenericDataResponse.value =
                                                    GenericDataResponse(
                                                        RequestStatus.SUCCESS,
                                                        processingResult,
                                                        null
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: kotlin.run {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestError("asdf", "aaa")
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }


    fun createServiceCallNote(
        createNotePostModel: CreateNotePostModel,
        context: Context
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.createServiceCallNote(createNotePostModel)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            if (processingResult.isHasError) {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(
                                        response,
                                        processingResult
                                    )
                                )
                            } else {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        run {
                                            withContext(Dispatchers.Main) {
                                                mGenericDataResponse.value =
                                                    GenericDataResponse(
                                                        RequestStatus.SUCCESS,
                                                        processingResult,
                                                        null
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: kotlin.run {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestError("asdf", "aaa")
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun deleteServiceCallNoteByNoteDetailId(
        deleteNotePostModel: DeleteNotePostModel,
        context: Context
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.deleteServiceCallNoteByDetailId(deleteNotePostModel)
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            if (processingResult.isHasError) {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(
                                        response,
                                        processingResult
                                    )
                                )
                            } else {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        run {
                                            withContext(Dispatchers.Main) {
                                                mGenericDataResponse.value =
                                                    GenericDataResponse(
                                                        RequestStatus.SUCCESS,
                                                        processingResult,
                                                        null
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: kotlin.run {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestError("asdf", "aaa")
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun getServiceCallNotesTypes(context: Context): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getServiceCallNoteTypes()
            ?.enqueue(object : Callback<ProcessingResult?> {
                override fun onResponse(
                    call: Call<ProcessingResult?>,
                    response: Response<ProcessingResult?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            if (processingResult.isHasError) {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(
                                        response,
                                        processingResult
                                    )
                                )
                            } else {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        run {
                                            var listOfNotes: MutableList<ServiceCallNoteTypeResponse> =
                                                mutableListOf(
                                                    *Settings.createGson()
                                                        .fromJson(
                                                            processingResult.result,
                                                            Array<ServiceCallNoteTypeResponse>::class.java
                                                        )
                                                )
                                            if (listOfNotes.isNullOrEmpty()) {
                                                listOfNotes = mutableListOf()
                                            }
                                            val listTosave = listOfNotes.map { note ->
                                                ServiceCallNoteTypeEntity.convertToNoteTypeEntity(
                                                    note
                                                )
                                            }
                                            ServiceCallNotesRepository.saveNoteTypes(listTosave)
                                            withContext(Dispatchers.Main) {
                                                mGenericDataResponse.value =
                                                    GenericDataResponse(
                                                        RequestStatus.SUCCESS,
                                                        processingResult,
                                                        null
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: kotlin.run {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestError("asdf", "aaa")
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult?>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t, context)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun getCanonDetails(
        equipmentId: String,
        callNumber: String
    ): MutableLiveData<CanonDetailsResponse?> {
        val canonDetails: MutableLiveData<CanonDetailsResponse?> = MutableLiveData()
        RetrofitApiHelper.getApi()?.getCanonDetails(equipmentId, callNumber)
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (!it.isHasError) {
                                val gson = Gson()
                                var resultObject =
                                    gson.fromJson(it.result, JsonElement::class.java)
                                val finalJsonObject = Gson().fromJson(
                                    resultObject.asString,
                                    JsonElement::class.java
                                ).asJsonObject
                                val canonDetailsParsed =
                                    gson.fromJson(finalJsonObject, CanonDetailsResponse::class.java)
                                canonDetails.value = canonDetailsParsed
                            }
                        }
                    } else {
                        canonDetails.value = null
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                }
            })
        return canonDetails
    }

    fun deletePartsFromEa(
        partsToDelete: MutableList<PartToDeletePostModel>,
        localPartsToDelete: MutableList<UsedPart>,
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.deleteMaterialFromCall(partsToDelete)
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            if (processingResult.isHasError) {
                                mGenericDataResponse.value = GenericDataResponse(
                                    RequestStatus.ERROR,
                                    null,
                                    RequestCodeHandler.getMessageErrorFromResponse(
                                        response,
                                        processingResult
                                    )
                                )
                            } else {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        localPartsToDelete.forEach {
                                            PartsRepository.deletePart(it.customId)
                                        }
                                        withContext(Dispatchers.Main) {
                                            mGenericDataResponse.value =
                                                GenericDataResponse(
                                                    RequestStatus.SUCCESS,
                                                    processingResult,
                                                    null
                                                )
                                        }
                                    }
                                }
                            }

                        } ?: kotlin.run {
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.ERROR,
                                null,
                                RequestError("Something went wrong", "error")
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun getAllCallPriorities(): MutableLiveData<GenericDataResponse<MutableList<CallPriority>>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<MutableList<CallPriority>>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getPriorities()
            ?.enqueue(object : Callback<MutableList<CallPriority>> {
                override fun onResponse(
                    call: Call<MutableList<CallPriority>>,
                    response: Response<MutableList<CallPriority>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { callPriorities ->
                            val realm = Realm.getDefaultInstance()
                            try {
                                realm.executeTransaction {
                                    realm.insertOrUpdate(callPriorities)
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, e.message.toString())
                            } finally {
                                realm.close()
                            }
                            mGenericDataResponse.value =
                                GenericDataResponse(RequestStatus.SUCCESS, callPriorities, null)

                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(
                                response,
                                null
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<MutableList<CallPriority>>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun getAllWarehouses(): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getAllWarehouses()
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.SUCCESS,
                                processingResult,
                                null
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun createNewPartTranster(createTransferModel: CreateTransferModel): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.createNewPartTransfer(createTransferModel)
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.SUCCESS,
                                processingResult,
                                null
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t)
                    )
                }
            })
        return mGenericDataResponse
    }

    fun updateGpsLocation(
        updatePosition: UpdatePosition,
        onResponse: (ProcessingResult?) -> Unit
    ) {
        RetrofitApiHelper.getApi()?.updateGpsLocation(updatePosition)
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    onResponse.invoke(response.body())
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    Log.d(TAG, t.message.toString())
                    onResponse.invoke(null)
                }
            })
    }

    fun checkCar(onResponse: (GenericDataResponse<CarInfo>?) -> Unit) {
        var mGenericDataResponse: GenericDataResponse<CarInfo>? = null

        RetrofitApiHelper.getGPSApi()?.checkCar()
            ?.enqueue(object : Callback<CarInfo> {
                override fun onResponse(
                    call: Call<CarInfo>,
                    response: Response<CarInfo>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->
                            mGenericDataResponse = GenericDataResponse(
                                RequestStatus.SUCCESS,
                                processingResult,
                                null
                            )
                        }
                    } else {
                        mGenericDataResponse = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                    onResponse.invoke(mGenericDataResponse)
                }

                override fun onFailure(call: Call<CarInfo>, t: Throwable) {
                    mGenericDataResponse = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t)
                    )
                    onResponse.invoke(mGenericDataResponse)
                }
            })
    }

}