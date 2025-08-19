package eci.technician.repository

import android.util.Log
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.UpdateManager
import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.create_call.CallType
import eci.technician.models.data.UsedPart
import eci.technician.models.data.UsedProblemCode
import eci.technician.models.data.UsedRepairCode
import eci.technician.models.filters.CallPriorityFilter
import eci.technician.models.filters.TechnicianCallType
import eci.technician.models.order.CallPriority
import eci.technician.models.order.ServiceCallLabor
import eci.technician.models.order.ServiceOrder
import eci.technician.models.order.StatusChangeModel
import eci.technician.models.sort.ServiceOrderSort
import eci.technician.workers.serviceOrderQueue.ServiceOrderOnlineManager
import io.realm.Realm
import io.realm.kotlin.toFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object ServiceOrderRepository {
    const val TAG = "ServiceOrderRepository"
    const val EXCEPTION = "Exception"

    enum class ServiceOrderStatus {
        PENDING, DISPATCHED, ARRIVED, ON_HOLD, SCHEDULED, COMPLETED, UNAVAILABLE_ERROR
    }

    fun getServiceCallList(): Flow<List<ServiceOrder>> {
        val realm = Realm.getDefaultInstance()
        return try {
            realm.where(ServiceOrder::class.java)
                .equalTo(ServiceOrder.COMPLETED, false)
                .findAll()
                .toFlow()
                .catch {
                    Log.e(TAG, EXCEPTION)
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            flow { emit(listOf())}
        } finally {
            realm.close()
        }
    }

    fun moveDispatchedServiceOrdersOnTop(mutableList: MutableList<ServiceOrder>): MutableList<ServiceOrder> {

        val list: MutableList<ServiceOrder> = mutableListOf()
        mutableList.forEach {
            val status = getServiceOrderStatusByCallNumberId(it.callNumber_ID)
            if (status == ServiceOrderStatus.DISPATCHED || status == ServiceOrderStatus.ARRIVED) {
                list.add(0, it)
            } else {
                list.add(it)
            }
        }
        return list
    }

    fun getOneServiceCallByCallId(callId: Int): Flow<Resource<List<ServiceOrder>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall { api.getOneServiceCallByIdSuspend(callId) })
        }
    }

    fun cancelServiceCallByIdFromServer(statusChangeModel: StatusChangeModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.cancelCall(statusChangeModel) }
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
        }
    }

    fun checkIfCompletedStatus(callId: Int): Boolean {
        val realm = Realm.getDefaultInstance()
        try {
            val serviceOrder = realm.where(ServiceOrder::class.java)
                .equalTo(ServiceOrder.CALL_NUMBER_ID, callId)
                .findFirst() ?: return false
            return serviceOrder.completedCall
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
            return false
        } finally {
            realm.close()
        }
    }

    suspend fun getOneServiceCallByIdAndSave(callId: Int): Flow<Resource<List<ServiceOrder>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall { api.getOneServiceCallByIdSuspend(callId) }.also { resource ->
                if (resource is Resource.Success) {
                    val list = resource.data ?: return@also
                    verifyOneServiceCallResponse(
                        callId,
                        list,
                        onSuccess = { serviceOrder ->
                            saveServiceOrderFromResponse(serviceOrder, false)
                        },
                        onReassigned = {
                            PartsRepository.deleteAllPartsByOrderId(
                                callId,
                                CoroutineScope(Dispatchers.IO)
                            )
                        },
                        onUnavailable = {
                            PartsRepository.deleteAllPartsByOrderId(
                                callId,
                                CoroutineScope(Dispatchers.IO)
                            )
                        })
                }
            })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun verifyOneServiceCallResponse(
        callId: Int,
        list: List<ServiceOrder>,
        onSuccess: suspend (serviceOrder: ServiceOrder) -> Unit,
        onReassigned: suspend (callId: Int) -> Unit,
        onUnavailable: suspend (callId: Int) -> Unit
    ) {
        if (!list.isNullOrEmpty()) {
            val serviceCall = list.first()
            val currentUserTechId = AppAuth.getInstance().technicianUser.technicianNumber
            if (serviceCall.technicianNumberId == currentUserTechId
                || serviceCall.labors.map { labor -> labor.technicianId }
                    .contains(currentUserTechId)
            ) {
                onSuccess.invoke(serviceCall)
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


    fun saveServiceOrderFromResponse(serviceOrderFromResponse: ServiceOrder, forceSave: Boolean) {
        if (forceSave) {
            saveServiceOrderFromResponse(serviceOrderFromResponse)
        }
        if (ServiceOrderOnlineManager.hasPendingChanges(serviceOrderFromResponse.callNumber_ID)) {
            return
        } else {
            saveServiceOrderFromResponse(serviceOrderFromResponse)
        }
    }


    private fun saveServiceOrderFromResponse(serviceOrderFromResponse: ServiceOrder) {
        val serviceCallToSave =
            UpdateManager.updateServiceOrderLocally(serviceOrderFromResponse)

        deletePartsThatAreNotInResponseForOneServiceCall(serviceCallToSave)
        GlobalScope.launch {
            UpdateManager.deleteLaborsThatAreNotInTheResponseByServiceCall(serviceOrderFromResponse)

        }
        val callPriorityFilter =  CallPriorityFilter(
            "TECHNICIAN_CALL_${serviceOrderFromResponse.callPriority}",
            false,
            isFromTechnicianCalls = true,
            priorityName = serviceOrderFromResponse.callPriority,
            isChecked = false
        )

        val realm = Realm.getDefaultInstance()
        try {
            val callTypes = realm.where(CallType::class.java).findAll()
            val callTypesFiltered = callTypes.find {
                serviceOrderFromResponse.callType?.contains(it.callTypeDescription ?: "") == true
            }

            callTypesFiltered?.let {callType ->
                val technicianCallType = TechnicianCallType(
                    callType.id,
                    callType.active,
                    callType.callTypeCode,
                    callType.callTypeDescription,
                    callType.callTypeId,
                    callType.companyId,
                    false
                )
                realm.executeTransaction {
                    realm.insertOrUpdate(technicianCallType)
                }
            }

            realm.executeTransaction {
                realm.insertOrUpdate(callPriorityFilter)
                realm.insertOrUpdate(serviceCallToSave)
                realm.insertOrUpdate(serviceCallToSave.labors)
                realm.insertOrUpdate(serviceCallToSave.parts)
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    private fun deletePartsThatAreNotInResponseForOneServiceCall(serviceOrderFromResponseUpdated: ServiceOrder) {
        val partsFromResponse = serviceOrderFromResponseUpdated.parts
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val partsToDelete = realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, serviceOrderFromResponseUpdated.callNumber_ID)
                    .and()
                    .not()
                    .`in`(
                        UsedPart.CUSTOM_USED_PART_ID,
                        partsFromResponse.map { it.customId }.toTypedArray()
                    )
                    .findAll()
                for (usedPart in partsToDelete) {
                    usedPart?.let {
                        if (!it.isAddedLocally) {
                            it.deleteFromRealm()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(UpdateManager.TAG, UpdateManager.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun deleteServiceOrderFromDbById(callNumberId: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction { realm1 ->
                val serviceOrderToDelete = realm1.where(ServiceOrder::class.java)
                    .equalTo(ServiceOrder.CALL_NUMBER_ID, callNumberId).findFirst()
                serviceOrderToDelete?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getServiceOrderByCallId(serviceOrderCallNumberId: Int): ServiceOrder? {
        val realm = Realm.getDefaultInstance()
        var serviceOrder: ServiceOrder? = null
        try {
            val entity = realm.where(ServiceOrder::class.java)
                .equalTo(ServiceOrder.CALL_NUMBER_ID, serviceOrderCallNumberId)
                .findFirst()
            entity?.let {
                serviceOrder = realm.copyFromRealm(it)
            }
            return serviceOrder
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return serviceOrder
    }

    suspend fun canShowNeededPartsIndicator(callNumberId: Int): Boolean {
        return if (isServiceOrderAssist(callNumberId)) {
            false
        } else {
            val parts = PartsRepository.getAllSentNeededPartsByOrderId(callNumberId)
            parts.isNotEmpty()
        }
    }

    fun isServiceOrderAssist(callNumberId: Int): Boolean {
        val realm = Realm.getDefaultInstance()
        var isAssist = false
        try {
            val serviceOrder = getServiceOrderByCallId(callNumberId)
            val labors = getLaborsByCallId(callNumberId)
            val techCode = AppAuth.getInstance().technicianUser.technicianCode
            val techNumber = AppAuth.getInstance().technicianUser.technicianNumber
            serviceOrder?.let {
                isAssist =
                    techCode != it.technicianNumber && labors.map { labor -> labor.technicianId }
                        .contains(techNumber)
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return isAssist
    }


    fun getServiceOrderStatusByCallNumberId(callNumberId: Int): ServiceOrderStatus {
        val isAssist = isServiceOrderAssist(callNumberId)
        val serviceOrder =
            getServiceOrderByCallId(callNumberId) ?: return ServiceOrderStatus.UNAVAILABLE_ERROR

        val labors = getLaborsByCallId(callNumberId)
        val realm = Realm.getDefaultInstance()
        try {
            if (isAssist) {
                if (serviceOrder.statusCode_Code?.trim()?.uppercase() == "H") {
                    return ServiceOrderStatus.ON_HOLD
                }
                val assistLabor =
                    labors.find { it.technicianId == AppAuth.getInstance().technicianUser.technicianNumber }
                        ?: return ServiceOrderStatus.UNAVAILABLE_ERROR
                when {
                    assistLabor.dispatchTime == null && assistLabor.arriveTime == null && assistLabor.departureTime == null -> {
                        return ServiceOrderStatus.PENDING
                    }
                    assistLabor.dispatchTime != null && assistLabor.arriveTime == null && assistLabor.departureTime == null -> {
                        return ServiceOrderStatus.DISPATCHED
                    }
                    assistLabor.dispatchTime != null && assistLabor.arriveTime != null && assistLabor.departureTime == null -> {
                        return ServiceOrderStatus.ARRIVED
                    }
                    assistLabor.dispatchTime != null && assistLabor.arriveTime != null && assistLabor.departureTime != null -> {
                        return ServiceOrderStatus.COMPLETED
                    }
                    else -> {
                        return ServiceOrderStatus.PENDING
                    }

                }

            } else {
                when (serviceOrder.statusCode_Code?.trim()?.uppercase()) {
                    "P" -> {
                        return ServiceOrderStatus.PENDING
                    }
                    "D" -> {
                        return if (serviceOrder.dispatchTime != null && serviceOrder.arriveTime != null) {
                            ServiceOrderStatus.ARRIVED
                        } else {
                            ServiceOrderStatus.DISPATCHED
                        }
                    }
                    "S" -> {
                        return ServiceOrderStatus.SCHEDULED
                    }
                    "H" -> {
                        return ServiceOrderStatus.ON_HOLD
                    }
                    else -> {
                        return ServiceOrderStatus.PENDING
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
            return ServiceOrderStatus.PENDING

        } finally {
            realm.close()
        }
    }


    private fun getLaborsByCallId(callNumberId: Int): List<ServiceCallLabor> {
        val realm = Realm.getDefaultInstance()
        var labors = listOf<ServiceCallLabor>()
        try {
            val laborsEntity = realm.where(ServiceCallLabor::class.java)
                .equalTo(ServiceCallLabor.CALL_ID, callNumberId).findAll()
            labors = realm.copyFromRealm(laborsEntity)
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return labors
    }

    private fun deleteAllIndex() {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction { realm1 ->
                val serviceOrderToDelete = realm1.where(ServiceOrderSort::class.java)
                    .findAll()
                serviceOrderToDelete?.deleteAllFromRealm()
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    private fun createNewIndexes(serviceOrderList: List<ServiceOrder>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                serviceOrderList.forEach { serviceOrder ->
                    val newIndex =
                        ServiceOrderSort(
                            serviceOrder.callNumber_Code,
                            serviceOrder.index
                        )
                    realm.insertOrUpdate(newIndex)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun findIndex(callNumberCode: String): ServiceOrderSort {
        val realm = Realm.getDefaultInstance()
        var targetIndex = ServiceOrderSort("", -2)
        return try {
            realm
                .where(ServiceOrderSort::class.java)
                .equalTo(ServiceOrderSort.CALL_NUMBER_CODE, callNumberCode)
                .findFirst()?.let {
                    targetIndex = it
                }
            targetIndex
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
            targetIndex
        } finally {
            realm.close()
        }
    }

    fun assignIndexBasedOnRestriction(serviceOrders: List<ServiceOrder>): List<ServiceOrder> {
        deleteAllIndex()
        serviceOrders.forEachIndexed { index, serviceOrder ->
            serviceOrder.index = index + 1
        }
        createNewIndexes(serviceOrders)
        return serviceOrders
    }

    fun shouldShowWarningOnUnDispatchByCallId(callId: Int): Boolean {
        var res: Boolean
        val realm = Realm.getDefaultInstance()
        try {
            val parts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, callId)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .findAll()
            val problemCodes = realm.where(UsedProblemCode::class.java)
                .equalTo(UsedProblemCode.CALL_ID, callId)
                .findAll()
            val repairCodes = realm.where(UsedRepairCode::class.java)
                .equalTo(UsedRepairCode.CALL_ID, callId)
                .findAll()

            val softDeletedParts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, callId)
                .equalTo(UsedPart.SENT, true)
                .equalTo(UsedPart.DELETABLE, true)
                .findAll()
            res = (parts.size + problemCodes.size + repairCodes.size + softDeletedParts.size) > 0
        } catch (e: Exception) {
            Log.e(PartsRepository.TAG, PartsRepository.EXCEPTION, e)
            res = false
        } finally {
            realm.close()
        }
        return res
    }

    fun getServiceOrderPriorityById(priorityName: String): CallPriority? {
        //TODO check null error
        val realm = Realm.getDefaultInstance()
        var callPriority: CallPriority? = null
        return try {
            callPriority = realm.where(CallPriority::class.java)
                .equalTo(CallPriority.PRIORITY_NAME, priorityName)
                .findFirst()
            callPriority?.let {
                callPriority = realm.copyFromRealm(it)
            }
            callPriority
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            callPriority
        } finally {
            realm.close()
        }
    }

    suspend fun getAllCallPriorities(): Flow<Resource<List<CallPriority>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getPriorities2() }
            if (resource is Resource.Success) {
                resource.data?.let { prioritiesList ->
                    updateCallPriorities(prioritiesList)
                }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun updateCallPriorities(prioritiesList: List<CallPriority>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(prioritiesList)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

}