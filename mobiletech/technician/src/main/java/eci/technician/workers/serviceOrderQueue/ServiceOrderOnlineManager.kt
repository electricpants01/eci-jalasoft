package eci.technician.workers.serviceOrderQueue

import eci.technician.helpers.AppAuth
import eci.technician.models.order.IncompleteRequests
import eci.technician.models.order.ServiceOrder
import io.realm.Realm

/**
 * These methods receives as parameter a ServiceCall from the API's RESPONSES without any change
 * DO NOT USE THESE METHODS WITH ORDERS FROM THE LOCAL DATABASE
 */
object ServiceOrderOnlineManager {

    private fun isAssistant(serviceOrder: ServiceOrder): Boolean {
        val techCode = AppAuth.getInstance().technicianUser.technicianCode
        val techNumber = AppAuth.getInstance().technicianUser.technicianNumber
        return techCode != serviceOrder.technicianNumber && serviceOrder.labors.map { it.technicianId }
            .contains(techNumber)
    }

    fun canArrive(serviceOrder: ServiceOrder): Boolean {
        val techNumber = AppAuth.getInstance().technicianUser.technicianNumber

        if (isAssistant(serviceOrder)) {
            val labor = serviceOrder.labors.find { it.technicianId == techNumber } ?: return false
            labor.let {
                return it.arriveTime == null && it.dispatchTime != null
            }
        } else {
            val code = serviceOrder.statusCode_Code?.trim()
            return "D" == code && serviceOrder.arriveTime == null
        }
    }

    fun canComplete(serviceOrder: ServiceOrder): Boolean {
        val techNumber = AppAuth.getInstance().technicianUser.technicianNumber
        if (isAssistant(serviceOrder)) {
            val labor = serviceOrder.labors.find { it.technicianId == techNumber } ?: return false
            labor.let {
                return it.departureTime == null && it.arriveTime != null
            }
        } else {
            val code = serviceOrder.statusCode_Code?.trim()
            return serviceOrder.labors.map { it.departureTime == null }.isEmpty() && "D" == code

        }
    }

    fun canDispatch(serviceOrder: ServiceOrder): Boolean {
        val techNumber = AppAuth.getInstance().technicianUser.technicianNumber
        val serviceOrderCode = serviceOrder.statusCode_Code?.trim()
        if (isAssistant(serviceOrder)) {
            val labor = serviceOrder.labors.find { it.technicianId == techNumber } ?: return false
            return labor.dispatchTime == null && "H" != serviceOrderCode
        } else {
            return serviceOrderCode == "P" || serviceOrderCode == "S"
        }
    }

    fun hasPendingChanges(callId: Int): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            val unSyncItems = realm.where(IncompleteRequests::class.java)
                .equalTo(IncompleteRequests.CALL_ID, callId).findAll()
            unSyncItems.size > 1

        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

}