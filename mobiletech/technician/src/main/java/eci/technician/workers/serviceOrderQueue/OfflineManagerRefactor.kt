package eci.technician.workers.serviceOrderQueue

import android.content.Context
import android.util.Log
import eci.technician.MainApplication
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.data.UsedPart
import eci.technician.models.gps.GPSLocation
import eci.technician.models.order.HoldCode
import eci.technician.models.order.IncompleteRequests
import eci.technician.models.order.StatusChangeModel
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.repository.IncompleteRequestsRepository.setIncompleteToFail
import eci.technician.repository.IncompleteRequestsRepository.setIncompleteToInProgress
import eci.technician.repository.IncompleteRequestsRepository.setIncompleteToSuccess
import eci.technician.repository.OfflineRequestRepository
import eci.technician.tools.Constants
import eci.technician.workers.OfflineManager
import eci.technician.workers.serviceOrderQueue.ServiceOrderOfflineUtils.handleClockError
import eci.technician.workers.serviceOrderQueue.ServiceOrderOfflineUtils.handleMainActionsError
import eci.technician.workers.serviceOrderQueue.ServiceOrderOfflineUtils.handleUnexpectedError
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.coroutines.flow.collect
import java.util.*
import kotlin.collections.HashMap


object OfflineManagerRefactor {

    const val TAG = "OfflineManagerRefactor"
    const val EXCEPTION = "Exception"
    const val GENERIC_ERROR = "Something went wrong with the connection"

    /**
     * Main actions [DispatchCall, ArriveCall, UnDispatchCall, UpdateLabor]
     */
    @Synchronized
    suspend fun performServiceCallMainActions(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        completion: () -> Unit
    ) {
        val incompleteReqId = firstIncomplete.id
        val callId = firstIncomplete.callId ?: return
        val requestType = firstIncomplete.requestType ?: return
        val map = HashMap<String, Any>()
        map["CallId"] = callId
        map["Odometer"] = firstIncomplete.savedOdometer ?: 0
        map["ActivityCodeId"] = firstIncomplete.activityCodeId
        map["ActionTime"] = firstIncomplete.actionTime ?: Date()
        map["StatusCode_Code"] = firstIncomplete.callStatusCode ?: ""

        if (firstIncomplete.requestType == Constants.REQUEST_UPDATE_LABOR) {
            map["ActionType"] = firstIncomplete.assistActionType ?: 1
            map["TechnicianId"] = firstIncomplete.technicianId
                ?: AppAuth.getInstance().technicianUser.technicianNumber
        }
        try {
            OfflineRequestRepository.dispatchArriveUnDispatchAction(requestType, map)
                .collect { value ->
                    when (value) {
                        is Resource.Success -> {
                            value.data?.let {
                                ServiceOrderOfflineUtils.saveUpdatedServiceCall(it.result, false)
                            }
                            setIncompleteToSuccess(incompleteReqId)
                            OfflineManager.retryWorker(appContext)
                            completion.invoke()
                        }
                        is Resource.Error -> {
                            val error = value.error?.second ?: "error"
                            handleMainActionsError(value, requestType, callId, appContext,
                                onSuccess = {
                                    setIncompleteToSuccess(incompleteReqId)
                                    OfflineManager.retryWorker(appContext)
                                    completion.invoke()
                                },
                                onError = {
                                    setIncompleteToFail(incompleteReqId, error)
                                    OfflineManager.stopWorker(appContext)
                                    ErrorHandler.get()
                                        .notifyListeners(value.error, requestType, callId, "")
                                })
                        }
                        is Resource.Loading -> {
                            setIncompleteToInProgress(incompleteReqId)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, requestType, callId)
            completion.invoke()
        }
    }

    /**
     * Performs LaunchIn, LaunchOut, BreakIn, BreakOut
     */
    @Synchronized
    suspend fun performClockActionsOffline(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        completion: () -> Unit
    ) {
        val action = firstIncomplete.requestType ?: return
        val incompleteReqId = firstIncomplete.id
        val changeStatusModel: ChangeStatusModel = if (firstIncomplete.savedOdometer != null) {
            ChangeStatusModel(firstIncomplete.savedOdometer ?: 0.0)
        } else {
            ChangeStatusModel()
        }
        if (MainApplication.lastLocation != null) {
            changeStatusModel.gpsLocation =
                GPSLocation.fromAndroidLocation(MainApplication.lastLocation)
        }
        changeStatusModel.actionTime = firstIncomplete.dateAdded
        try {
            OfflineRequestRepository.requestClockActions(action, changeStatusModel)
                .collect { value ->
                    when (value) {
                        is Resource.Success -> {
                            setIncompleteToSuccess(incompleteReqId)
                            OfflineManager.retryWorker(appContext)
                            completion.invoke()
                        }
                        is Resource.Error -> {
                            val error = value.error?.second ?: "error"
                            handleClockError(value, action,
                                onSuccess = {
                                    setIncompleteToSuccess(incompleteReqId)
                                    OfflineManager.retryWorker(appContext)
                                    completion.invoke()
                                },
                                onError = {
                                    setIncompleteToFail(incompleteReqId, error)
                                    OfflineManager.stopWorker(appContext)
                                    ErrorHandler.get().notifyListeners(value.error, action, -1, "")
                                    completion.invoke()
                                })
                        }
                        is Resource.Loading -> {
                            setIncompleteToInProgress(incompleteReqId)
                            completion.invoke()
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, action, -1)
            completion.invoke()
        }
    }

    /**
     * Performs OnHoldCall action
     */
    @Synchronized
    suspend fun performOnHoldOffline(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        completion: () -> Unit
    ) {
        val callId = firstIncomplete.callId ?: return
        val incompleteReqId = firstIncomplete.id
        val realm = Realm.getDefaultInstance()
        val statusChangeModel = StatusChangeModel()
        val usageStatusId = 2
        val requestType = firstIncomplete.requestType ?: return
        statusChangeModel.callId = callId
        statusChangeModel.actionTime = firstIncomplete.actionTime
        statusChangeModel.codeId = firstIncomplete.holdCodeId
        statusChangeModel.statusCodeCode = firstIncomplete.callStatusCode
        if (firstIncomplete.holdCodeTypeId == HoldCode.WAITING_FOR_PARTS_TYPE_ID) {
            val usedParts: RealmResults<UsedPart> = realm
                .where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, statusChangeModel.callId)
                .equalTo(UsedPart.USAGE_STATUS_ID, usageStatusId)
                .equalTo(UsedPart.HOLD_CODE_ID, firstIncomplete.holdCodeId)
                .findAll()

            val usedPartMaps: MutableList<Map<String, Any>> = ArrayList(usedParts.size)
            for (part in usedParts) {
                if (part.isSent) continue
                val map: MutableMap<String, Any> = java.util.HashMap()
                map["CallId"] = part.callId
                map["ItemId"] = part.itemId
                if (part.localDescription != null) {
                    map["Description"] = part.localDescription ?: ""
                }
                part.serialNumber?.let {
                    map["SerialNumber"] = it
                }
                map["Quantity"] = part.quantity
                map["UsageStatusId"] = part.usageStatusId
                map["BinId"] = 0
                map["WarehouseId"] = 0
                usedPartMaps.add(map)
            }
            statusChangeModel.usedParts = usedPartMaps
        } else {
            statusChangeModel.usedParts = ArrayList()
        }
        val usedPartAddedLocally = realm.where(UsedPart::class.java)
            .equalTo(UsedPart.ADDED_LOCALLY, true)
            .equalTo(UsedPart.SENT, false)
            .equalTo(UsedPart.CALL_ID, firstIncomplete.callId)
            .equalTo(UsedPart.HOLD_CODE_ID, firstIncomplete.holdCodeId)
            .findAll()
        try {
            OfflineRequestRepository.requestOnHoldAction(statusChangeModel).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        realm.executeTransaction {
                            usedPartAddedLocally.deleteAllFromRealm()
                        }
                        value.data?.let {
                            ServiceOrderOfflineUtils.saveUpdatedServiceCall(it.result, false)
                        }
                        setIncompleteToSuccess(incompleteReqId)
                        OfflineManager.retryWorker(appContext)
                        completion.invoke()
                    }
                    is Resource.Error -> {
                        handleMainActionsError(value, requestType, callId, appContext,
                            onSuccess = {
                                realm.executeTransaction {
                                    usedPartAddedLocally.deleteAllFromRealm()
                                }
                                setIncompleteToSuccess(incompleteReqId)
                                OfflineManager.retryWorker(appContext)
                                completion.invoke()
                            },
                            onError = {
                                val error = value.error?.second ?: "error"
                                setIncompleteToFail(incompleteReqId, error)
                                OfflineManager.stopWorker(appContext)
                                ErrorHandler.get().notifyListeners(value.error, requestType, callId,"")
                                completion.invoke()
                            })
                    }
                    is Resource.Loading -> {
                        setIncompleteToInProgress(incompleteReqId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, requestType, callId)
            completion.invoke()
        } finally {
            realm.close()
        }
    }

    /**
     * Perform UpdateItemDetails action
     */
    @Synchronized
    suspend fun performUpdateDetailsOffline(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        completion: () -> Unit
    ) {
        val incompleteReqId = firstIncomplete.id
        val callId = firstIncomplete.callId ?: return
        val equipmentId = firstIncomplete.equipmentId
        val itemType = firstIncomplete.itemType ?: return
        val requestType = firstIncomplete.requestType ?: return
        val map = HashMap<String, Any>()
        map["Id"] = equipmentId
        map["Value"] = firstIncomplete.newValue ?: ""
        map["ItemType"] = itemType

        try {
            OfflineRequestRepository.requestUpdateEquipmentDetails(action = requestType, map = map)
                .collect { value ->
                    when (value) {
                        is Resource.Success -> {
                            setIncompleteToSuccess(incompleteReqId)
                            OfflineManager.retryWorker(appContext)
                            completion.invoke()
                        }
                        is Resource.Error -> {
                            val error = value.error?.second ?: "error"
                            setIncompleteToFail(incompleteReqId, error)
                            ErrorHandler.get().notifyListeners(value.error, requestType, callId,"")
                            OfflineManager.stopWorker(appContext)
                            completion.invoke()
                        }
                        is Resource.Loading -> {
                            setIncompleteToInProgress(incompleteReqId)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, requestType, callId)
            completion.invoke()
        }

    }

    /**
     * Perform ScheduleCall action
     */
    @Synchronized
    suspend fun performScheduleCallOffline(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        completion: () -> Unit
    ) {
        val requestType = firstIncomplete.requestType ?: return
        val incompleteReqId = firstIncomplete.id
        val callId = firstIncomplete.callId ?: return
        val statusChangeModel = StatusChangeModel()
        statusChangeModel.actionTime = firstIncomplete.actionTime
        statusChangeModel.callId = callId
        statusChangeModel.statusCodeCode = firstIncomplete.callStatusCode

        try {
            OfflineRequestRepository.requestScheduleCall(statusChangeModel).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        value.data?.let {
                            ServiceOrderOfflineUtils.saveUpdatedServiceCall(it.result, false)
                        }
                        setIncompleteToSuccess(incompleteReqId)
                        OfflineManager.retryWorker(appContext)
                        completion.invoke()
                    }
                    is Resource.Error -> {
                        handleMainActionsError(value, requestType, callId, appContext,
                            onSuccess = {
                                setIncompleteToSuccess(incompleteReqId)
                                OfflineManager.retryWorker(appContext)
                                completion.invoke()
                            },
                            onError = {
                                val error = value.error?.second ?: "error"
                                setIncompleteToFail(incompleteReqId, error)
                                ErrorHandler.get().notifyListeners(value.error, requestType, callId,"")
                                OfflineManager.stopWorker(appContext)
                                completion.invoke()
                            })
                    }
                    is Resource.Loading -> {
                        setIncompleteToInProgress(incompleteReqId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, requestType, callId)
            completion.invoke()
        }

    }

    /**
     * Perform HoldRelease action
     */
    @Synchronized
    suspend fun performReleaseCallOffline(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        completion: () -> Unit
    ) {
        val requestType = firstIncomplete.requestType ?: return
        val incompleteReqId = firstIncomplete.id
        val callId = firstIncomplete.callId ?: return
        val statusChangeModel = StatusChangeModel()
        statusChangeModel.actionTime = firstIncomplete.actionTime
        statusChangeModel.callId = callId
        statusChangeModel.statusCodeCode = firstIncomplete.callStatusCode

        try {
            OfflineRequestRepository.requestHoldRelease(statusChangeModel).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        value.data?.let {
                            ServiceOrderOfflineUtils.saveUpdatedServiceCall(it.result, false)
                        }
                        setIncompleteToSuccess(incompleteReqId)
                        OfflineManager.retryWorker(appContext)
                        completion.invoke()
                    }
                    is Resource.Error -> {
                        handleMainActionsError(value, requestType, callId, appContext,
                            onSuccess = {
                                setIncompleteToSuccess(incompleteReqId)
                                OfflineManager.retryWorker(appContext)
                                completion.invoke()
                            },
                            onError = {
                                val error = value.error?.second ?: "error"
                                setIncompleteToFail(incompleteReqId, error)
                                ErrorHandler.get().notifyListeners(value.error, requestType, callId,"")
                                OfflineManager.stopWorker(appContext)
                                completion.invoke()
                            })
                    }
                    is Resource.Loading -> {
                        setIncompleteToInProgress(incompleteReqId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, requestType, callId)
            completion.invoke()
        }
    }

    /**
     * Perform DepartureCall, IncompleteCall actions
     */
    @Synchronized
    suspend fun performDepartCallOffline(
        firstIncomplete: IncompleteRequests,
        appContext: Context,
        isIncomplete: Boolean,
        completion: () -> Unit
    ) {
        val incompleteReqId = firstIncomplete.id
        val requestType = firstIncomplete.requestType ?: return
        val callId = firstIncomplete.callId ?: return
        val statusChange = ServiceOrderOfflineUtils.prepareStatusChangeModelForDepart(
            firstIncomplete,
            isIncomplete
        )

        try {
            OfflineRequestRepository.requestCompleteCall(requestType, statusChange)
                .collect { value ->
                    when (value) {
                        is Resource.Success -> {
                            ServiceOrderOfflineUtils.deleteDataOnSuccess(callId)
                            setIncompleteToSuccess(incompleteReqId)
                            OfflineManager.retryWorker(appContext)
                            completion.invoke()
                        }
                        is Resource.Error -> {
                            handleMainActionsError(value, requestType, callId, appContext,
                                onSuccess = {
                                    ServiceOrderOfflineUtils.deleteDataOnSuccess(callId)
                                    setIncompleteToSuccess(incompleteReqId)
                                    OfflineManager.retryWorker(appContext)
                                    completion.invoke()
                                },
                                onError = {
                                    val err = value.error?.second ?: "error"
                                    setIncompleteToFail(incompleteReqId, err)
                                    ErrorHandler.get().notifyListeners(value.error, requestType, callId,"")
                                    OfflineManager.stopWorker(appContext)
                                    completion.invoke()
                                })
                        }
                        is Resource.Loading -> {
                            setIncompleteToInProgress(incompleteReqId)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            handleUnexpectedError(incompleteReqId, GENERIC_ERROR, appContext, requestType, callId)
            completion.invoke()
        }
    }

}