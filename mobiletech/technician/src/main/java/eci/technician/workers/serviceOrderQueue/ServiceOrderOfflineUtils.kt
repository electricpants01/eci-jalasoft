package eci.technician.workers.serviceOrderQueue

import android.content.Context
import android.util.Log
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.ErrorHelper.ServiceOrderErrorManager
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.ProcessingResult
import eci.technician.models.TechnicianUser
import eci.technician.models.data.UsedPart
import eci.technician.models.data.UsedProblemCode
import eci.technician.models.data.UsedRepairCode
import eci.technician.models.order.*
import eci.technician.repository.IncompleteRequestsRepository.setIncompleteToFail
import eci.technician.repository.PartsRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.tools.Constants
import eci.technician.tools.Settings
import eci.technician.workers.OfflineManager
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*

object ServiceOrderOfflineUtils {
    const val TAG = "ServiceOrderOfflineUtils"
    const val EXCEPTION = "Exception"

    /**
     * Receives a ServiceCall as a String, converts it to a ServiceCall Object and save into realm
     */
    fun saveUpdatedServiceCall(
        result: String?,
        updateCustomerWarehousePart: Boolean,
        updateEquipmentMeters: Boolean = false
    ) {
        try {
            val serviceOrder: ServiceOrder? =
                Settings.createGson().fromJson(result, ServiceOrder::class.java)
            if (AppAuth.getInstance().technicianUser.technicianNumber == serviceOrder?.technicianNumberId) {
                GlobalScope.launch(Dispatchers.IO) {
                    if (serviceOrder != null) {
                        if (serviceOrder.labors == null) serviceOrder.labors = listOf()
                        ServiceOrderRepository.saveServiceOrderFromResponse(serviceOrder, false)
                    }
                }
                GlobalScope.launch(Dispatchers.IO) {
                    serviceOrder.equipmentId?.let {
                        if (it != 0 && updateEquipmentMeters) {
                            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                                .getEquipmentMetersByEquipmentId(it)
                        }
                    }
                }
                GlobalScope.launch(Dispatchers.IO) {
                    serviceOrder.customerWarehouseId.let {
                        if (it != 0 && updateCustomerWarehousePart) {
                            PartsRepository.getWarehousePartsById(it, savePartInDB = true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    /**
     * Handles the Request error, if there is a Backend error will verify if the request can be discarded
     * @param value is the Resource from the flow
     * @param requestType the actions that have been performed
     * @param callId the ServiceCallId
     * @param applicationContext the context to start/stop the worker
     * @param onSuccess if the verification retrieving a ServiceCall is success
     * @param onError any other error that are not BACKEND related
     */
    suspend fun handleMainActionsError(
        value: Resource<ProcessingResult>,
        requestType: String,
        callId: Int,
        applicationContext: Context,
        onSuccess: suspend () -> Unit,
        onError: suspend () -> Unit
    ) {
        when (value.error?.first ?: ErrorType.SOMETHING_WENT_WRONG) {
            ErrorType.BACKEND_ERROR -> {
                value.data?.let {
                    verifyResponseWithOneServiceCall(it,
                        requestType,
                        callId,
                        applicationContext,
                        onSuccess = {
                            onSuccess.invoke()
                        },
                        onError = {
                            onError.invoke()
                        })
                }
            }
            else -> {
                onError.invoke()
            }
        }
    }

    /**
     * The method verifies if the @param [processingResult] has an error that can be handled
     * depending on the @param [requestType] and the @param[callId], since it makes a request to
     * ServiceCall/GetServiceCallByCallId  it will trigger  @param [onSuccess] when the SC is ok or
     * reassigned or unavailabe
     * @param onSuccess -> The request will be skipped
     * @param onError -> The method has a valid error that should be shown in UI
     */
    private suspend fun verifyResponseWithOneServiceCall(
        processingResult: ProcessingResult,
        requestType: String,
        callId: Int,
        applicationContext: Context,
        onSuccess: suspend () -> Unit,
        onError: suspend () -> Unit
    ) {
        ServiceOrderRepository.getOneServiceCallByCallId(callId).collect { value ->
            when (value) {
                is Resource.Success -> {
                    val list = value.data ?: listOf()
                    ServiceOrderRepository.verifyOneServiceCallResponse(callId, list,
                        onSuccess = { serviceOrder ->
                            val shouldDiscard = ServiceOrderErrorManager().shouldBeDiscardedOnError(
                                callId,
                                requestType,
                                processingResult,
                                serviceOrder
                            )
                            if (shouldDiscard) {
                                ServiceOrderRepository.saveServiceOrderFromResponse(
                                    serviceOrder,
                                    false
                                )
                                onSuccess.invoke()
                            } else {
                                onError.invoke()
                            }
                        },
                        onReassigned = {
                            /**
                             * service Order reassigned
                             */
                            ErrorHandler.get().notifyListeners(
                                error = Pair(ErrorType.SOMETHING_WENT_WRONG, ""),
                                requestType = "SERVICE_CALL_REASSIGNED",
                                callId = callId,
                                data = ""
                            )
                            /**
                             * Delaying delete process, because it is faster than the notify handler
                             */
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(500)
                                ServiceOrderRepository.deleteServiceOrderFromDbById(callId)
                                PartsRepository.deleteAllPartsByOrderId(callId, this)
                                OfflineManager.retryWorker(applicationContext)
                            }
                            onSuccess.invoke()
                        },
                        onUnavailable = {
                            /**
                             * service Order deleted/completed
                             */
                            ErrorHandler.get().notifyListeners(
                                error = Pair(ErrorType.SOMETHING_WENT_WRONG, ""),
                                requestType = "SERVICE_CALL_CANCELED_OR_DELETED",
                                callId = callId,
                                data = ""
                            )
                            /**
                             * Delaying delete process, because it is faster than the notify handler
                             */
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(500)
                                ServiceOrderRepository.deleteServiceOrderFromDbById(callId)
                                PartsRepository.deleteAllPartsByOrderId(callId, this)
                                OfflineManager.retryWorker(applicationContext)
                            }
                            onSuccess.invoke()
                        }
                    )
                }
                is Resource.Error -> {
                    onError.invoke()
                }
                is Resource.Loading -> {
                    // do nothing
                }
            }
        }
    }

    /**
     * Verifies if the @param [value] has a backend error that can be handled
     * else return @param [onError] so it can be handled in the parent method
     */
    suspend fun handleClockError(
        value: Resource<ProcessingResult>,
        requestType: String,
        onSuccess: suspend () -> Unit,
        onError: suspend () -> Unit
    ) {
        when (value.error?.first ?: ErrorType.SOMETHING_WENT_WRONG) {
            ErrorType.BACKEND_ERROR -> {
                value.data?.let {
                    verifyClockAction(requestType, onSuccess = {
                        onSuccess.invoke()
                    }, onError = {
                        onError.invoke()
                    })
                }
            }
            else -> {
                onError.invoke()
            }
        }
    }

    /**
     * Depending on the @param [requestType], and the most recent state (retrieved), the method
     * verifies if the request can be skipped
     */
    private suspend fun verifyClockAction(
        requestType: String,
        onSuccess: suspend () -> Unit,
        onError: suspend () -> Unit,
    ) {
        retrieveUserData()
        val currentStatus = (AppAuth.getInstance().technicianUser.status ?: "").lowercase()
        when (requestType.lowercase()) {
            Constants.STATUS_BRAKE_IN.lowercase() -> {
                if (currentStatus == Constants.STATUS_BRAKE_IN.lowercase()) {
                    onSuccess.invoke()
                } else {
                    onError.invoke()
                }
            }
            Constants.STATUS_LUNCH_IN.lowercase() -> {
                if (currentStatus == Constants.STATUS_LUNCH_IN.lowercase()) {
                    onSuccess.invoke()
                } else {
                    onError.invoke()
                }
            }
            Constants.STATUS_LUNCH_OUT.lowercase(),
            Constants.STATUS_BRAKE_OUT.lowercase() -> {
                if (currentStatus == Constants.STATUS_SIGNED_IN.lowercase()) {
                    onSuccess.invoke()
                } else {
                    onError.invoke()
                }
            }
            else -> {
                onError.invoke()
            }
        }
    }

    fun prepareStatusChangeModelForDepart(
        firstIncomplete: IncompleteRequests,
        isIncomplete: Boolean
    ): StatusChangeModel {
        val callId = firstIncomplete.callId ?: return StatusChangeModel()
        val realm = Realm.getDefaultInstance()
        val statusChangeModel = StatusChangeModel()
        try {
            if (isIncomplete) {
                statusChangeModel.incompleteCodeId = firstIncomplete.incompleteCodeId
                statusChangeModel.description = firstIncomplete.description
            }
            val labor = Labor()
            labor.dispatchTimeString = firstIncomplete.completeCallDispatchTime
            labor.arriveTimeString = firstIncomplete.completeCallArriveTime
            statusChangeModel.labor = labor
            statusChangeModel.actionTime = firstIncomplete.actionTime
            statusChangeModel.callId = callId
            statusChangeModel.comments = firstIncomplete.comments
            statusChangeModel.statusCodeCode = firstIncomplete.callStatusCode
            if (!firstIncomplete.isAssist) {
                statusChangeModel.isPreventativeMaintenance =
                    firstIncomplete.isPreventiveMaintenance
            }
            statusChangeModel.contentType = "image/jpeg"

            val usedProblemCodes = realm.where(UsedProblemCode::class.java)
                .equalTo(UsedProblemCode.CALL_ID, firstIncomplete.callId).findAll()
            val problemCodes: MutableList<Int> = ArrayList(usedProblemCodes.size)
            for (problemCode in usedProblemCodes) {
                problemCodes.add(problemCode.problemCodeId)
            }

            val emailDetail = realm.where(EmailDetail::class.java)
                .equalTo(EmailDetail.COLUMNS.EMAIL_CALL_NUMBER_ID, firstIncomplete.callId)
                .findFirst()
            val emailDetailMap: MutableMap<String, Any> = java.util.HashMap()
            emailDetail?.let { emailDetail1 ->
                emailDetailMap["BCCAddress"] = emailDetail1.bccAddress
                emailDetailMap["CCAddress"] = emailDetail1.ccAddress
                emailDetailMap["EmailContent"] = emailDetail1.emailContent
                emailDetailMap["Subject"] = emailDetail1.subjectEmail
                emailDetailMap["ToAddress"] = emailDetail1.toAddress
            }

            statusChangeModel.emailDetail = emailDetailMap
            statusChangeModel.problemCodes = problemCodes
            val usedRepairCodes = realm.where(UsedRepairCode::class.java)
                .equalTo(UsedRepairCode.CALL_ID, firstIncomplete.callId).findAll()
            val repairCodes: MutableList<Int> = ArrayList(usedRepairCodes.size)
            for (repairCode in usedRepairCodes) {
                repairCodes.add(repairCode.repairCodeId)
            }
            statusChangeModel.repairCodes = repairCodes
            val equipmentMeters = realm.where(EquipmentRealmMeter::class.java)
                .equalTo(EquipmentMeter.EQUIPMENT_ID, firstIncomplete.equipmentId).findAll()

            if (!firstIncomplete.isAssist && equipmentMeters.isNotEmpty()) {
                val meters = mutableListOf<EquipmentMeter>()
                equipmentMeters.forEach {
                    val equipmentMeterFromDatabase = EquipmentMeter()
                    equipmentMeterFromDatabase.actual = it.actual ?: 0.0
                    equipmentMeterFromDatabase.display = it.display ?: 0.0
                    equipmentMeterFromDatabase.equipmentId = it.equipmentId
                    equipmentMeterFromDatabase.initialDisplay = it.initialDisplay
                    equipmentMeterFromDatabase.isMeterSet = it.isMeterSet
                    equipmentMeterFromDatabase.isValidMeter = it.isValidMeter
                    equipmentMeterFromDatabase.isRequired = it.isRequired
                    equipmentMeterFromDatabase.isRequiredMeterOnServiceCalls =
                        it.isRequiredMeterOnServiceCalls
                    equipmentMeterFromDatabase.meterId = it.meterId
                    equipmentMeterFromDatabase.meterType = it.meterType
                    equipmentMeterFromDatabase.meterTypeId = it.meterTypeId
                    equipmentMeterFromDatabase.userLastMeter = it.userLastMeter
                    meters.add(equipmentMeterFromDatabase)
                }
                for (meter in meters) {
                    val dbMeter = realm.where(ServiceCallMeter::class.java)
                        .equalTo("serviceOrderId", firstIncomplete.callId)
                        .equalTo("meterId", meter.meterId)
                        .findFirst()
                    if (dbMeter != null && dbMeter.isValid) {
                        meter.userLastMeter = dbMeter.userLastValue
                    } else {
                        meter.userLastMeter = meter.actual ?: 0.0
                    }

                    meter.isValidMeter =
                        !(meter.userLastMeter < meter.display ?: 0.0 && AppAuth.getInstance().technicianUser.isAllowMeterForce)
                    meter.display = meter.userLastMeter
                }
                statusChangeModel.meters = meters

            }
            val usedPartsUpdate = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, firstIncomplete.callId)
                .equalTo(UsedPart.ACTION_TYPE, "update")
                .findAll().toList()

            val usedPartsInsert = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, firstIncomplete.callId)
                .equalTo(UsedPart.ACTION_TYPE, "insert")
                .findAll().toList()

            val usedPartsDelete = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, firstIncomplete.callId)
                .equalTo(UsedPart.ACTION_TYPE, "delete")
                .findAll().toList()

            val usedParts = mutableListOf<UsedPart>()
            usedParts.addAll(usedPartsDelete)
            usedParts.addAll(usedPartsUpdate)
            usedParts.addAll(usedPartsInsert)

            val usedPartMaps: MutableList<Map<String, Any>> = ArrayList(usedParts.size)
            for (part in usedParts) {
                val map: MutableMap<String, Any> = java.util.HashMap()
                map["CallId"] = part.callId
                map["ItemId"] = part.itemId
                if (part.localDescription != null && isIncomplete) {
                    map["Description"] = part.localDescription ?: ""
                }
                map["Quantity"] = part.quantity
                map["ActionType"] = part.actionType
                map["DetailID"] = part.detailId
                map["UsageStatusId"] = part.localUsageStatusId
                part.serialNumber?.let {
                    map["SerialNumber"] = it
                }
                if (isIncomplete && (part.localUsageStatusId == UsedPart.NEEDED_STATUS_CODE)) {
                    map["BinId"] = 0
                    map["WarehouseId"] = 0
                } else {
                    map["BinId"] = part.binId
                    map["WarehouseId"] = part.warehouseID
                }
                usedPartMaps.add(map)
            }
            statusChangeModel.usedParts = usedPartMaps
            statusChangeModel.activityCodeId = firstIncomplete.activityCodeId
            statusChangeModel.fileContentBase64 = firstIncomplete.fileContentBase64
            statusChangeModel.fileName = firstIncomplete.fileName
            statusChangeModel.fileSize = firstIncomplete.fileSize
            statusChangeModel.signeeName = firstIncomplete.signeeName
            return statusChangeModel
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return statusChangeModel
        } finally {
            realm.close()
        }
    }

    fun deleteDataOnSuccess(callId: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            GlobalScope.launch {
                delay(1000)
                ServiceOrderRepository.deleteServiceOrderFromDbById(callId)
                PartsRepository.deleteAllPartsByOrderId(callId, this)
            }


            val incompleteCodesChecked: OrderedRealmCollection<IncompleteCode> =
                realm.where(IncompleteCode::class.java)
                    .equalTo(IncompleteCode.COLUMNS.IS_CHECKED, java.lang.Boolean.TRUE)
                    .findAll()
            val masterRepairCodes: RealmResults<UsedRepairCode> = realm
                .where(UsedRepairCode::class.java)
                .equalTo(UsedRepairCode.CALL_ID, callId)
                .findAll()
            val serviceCallProperty = realm.where(ServiceCallProperty::class.java)
                .equalTo(ServiceCallProperty.CALL_ID, callId)
                .findAll()
            val serviceCallTemporalData: RealmResults<ServiceCallTemporalData> = realm
                .where(ServiceCallTemporalData::class.java)
                .equalTo(ServiceCallTemporalData.COLUMNS.CALL_NUMBER_ID, callId)
                .findAll()
            val serviceCallMeters: RealmResults<ServiceCallMeter> =
                realm.where(ServiceCallMeter::class.java)
                    .equalTo(ServiceCallMeter.CALL_ID, callId)
                    .findAll()

            val usedPartAddedLocally = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .equalTo(UsedPart.SENT, false)
                .equalTo(UsedPart.CALL_ID, callId)
                .findAll()
            val usedProblemCodes = realm.where(UsedProblemCode::class.java)
                .equalTo(UsedProblemCode.CALL_ID, callId).findAll()
            val usedRepairCodes = realm.where(UsedRepairCode::class.java)
                .equalTo(UsedRepairCode.CALL_ID, callId).findAll()

            realm.executeTransaction {
                for (incompleteCode in incompleteCodesChecked) {
                    incompleteCode.isChecked = false
                }
                masterRepairCodes.deleteAllFromRealm()
                serviceCallProperty.deleteAllFromRealm()
                serviceCallTemporalData.deleteAllFromRealm()
                usedProblemCodes.deleteAllFromRealm()
                usedRepairCodes.deleteAllFromRealm()
                serviceCallMeters.deleteAllFromRealm()
                usedPartAddedLocally.deleteAllFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun retrieveUserData() {
        try {
            val response = RetrofitApiHelper.getApi()?.updateUser()?.execute()
            response?.body()?.let { result ->
                if (!result.isHasError) {
                    val technicianUser: TechnicianUser = Settings.createGson()
                        .fromJson(result.result, TechnicianUser::class.java)
                    AppAuth.getInstance().technicianUser = technicianUser
                    AppAuth.getInstance().sendGpsStatus()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    suspend fun handleUnexpectedError(
        incompleteReqId: String,
        error: String,
        applicationContext: Context,
        requestType: String,
        callId: Int
    ) {
        setIncompleteToFail(incompleteReqId, error)
        OfflineManager.stopWorker(applicationContext)
        ErrorHandler.get()
            .notifyListeners(
                error = Pair(
                    ErrorType.SOMETHING_WENT_WRONG,
                    OfflineManagerRefactor.GENERIC_ERROR
                ),
                requestType = requestType,
                callId = callId,
                data = OfflineManagerRefactor.GENERIC_ERROR
            )
    }
}