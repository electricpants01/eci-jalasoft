package eci.technician.helpers.ErrorHelper

import android.util.Log
import eci.technician.models.ProcessingResult
import eci.technician.models.order.ServiceOrder
import eci.technician.tools.Constants
import eci.technician.workers.serviceOrderQueue.ServiceOrderOnlineManager
import io.realm.Realm

class ServiceOrderErrorManager {

    /**
     * This method verifies with the current list of SC if the request can be skipped or not
     */
    fun shouldBeDiscardedOnError(
        callId: Int,
        requestType: String,
        processingResult: ProcessingResult?
    ): Boolean {
        val realm = Realm.getDefaultInstance()
        val serviceOrder =
            realm.where(ServiceOrder::class.java).equalTo(ServiceOrder.CALL_NUMBER_ID, callId)
                .findFirst()
                ?: return true
        processingResult?.let {
            if (it.formattedErrors.contains("OK to Invoice")) {
                return true
            }
        }
        var shouldDiscard = false
        try {
            serviceOrder?.let {
                shouldDiscard =
                    when (requestType) {
                        "DispatchCall" -> serviceOrder.canArrive()
                        "ArriveCall" -> serviceOrder.canComplete()
                        "UnDispatchCall" -> serviceOrder.canDispatch()
                        "ScheduleCall" -> serviceOrder.statusCode_Code?.trim() == Constants.CALL_STATUS_TRIMMED.SCHEDULED.value
                        "HoldRelease" -> serviceOrder.statusCode_Code?.trim() == Constants.CALL_STATUS_TRIMMED.PENDING.value
                        "OnHoldCall" -> serviceOrder.statusCode_Code?.trim() == Constants.CALL_STATUS_TRIMMED.HOLD.value
                        else -> false
                    }
            }

        } catch (e: Exception) {
            Log.e("shouldBeDiscardedOnError", "Exception", e)
            shouldDiscard = false
        } finally {
            realm.close()
            return shouldDiscard
        }
    }


    /**
     * This method verifies
     * if the ServiceCall (SERVICE CALL RECEIVED FROM THE RESPONSE WITHOUT CHANGES)
     * has changes we can handle and discard the current incompleteRequest
     */
    fun shouldBeDiscardedOnError(
        callId: Int,
        requestType: String,
        processingResult: ProcessingResult?,
        apiServiceOrder: ServiceOrder
    ): Boolean {
        processingResult?.let {
            if (it.formattedErrors.contains("OK to Invoice")) {
                return true
            }
        }
        var shouldDiscard = false
        try {
            apiServiceOrder.let { so ->
                shouldDiscard =
                    when (requestType) {
                        "DispatchCall" -> ServiceOrderOnlineManager.canArrive(so)
                        "ArriveCall" -> ServiceOrderOnlineManager.canComplete(so)
                        "UnDispatchCall" -> ServiceOrderOnlineManager.canDispatch(so)
                        "ScheduleCall" -> so.statusCode_Code?.trim() == Constants.CALL_STATUS_TRIMMED.SCHEDULED.value
                        "HoldRelease" -> so.statusCode_Code?.trim() == Constants.CALL_STATUS_TRIMMED.PENDING.value
                        "OnHoldCall" -> so.statusCode_Code?.trim() == Constants.CALL_STATUS_TRIMMED.HOLD.value
                        else -> false
                    }
            }

        } catch (e: Exception) {
            Log.e("shouldBeDiscardedOnError", "Exception", e)
            shouldDiscard = false
        } finally {
            return shouldDiscard
        }
    }

}