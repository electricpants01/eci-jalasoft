package eci.technician.helpers.api

import android.util.Log
import eci.technician.helpers.AppAuth
import eci.technician.models.create_call.CallType
import eci.technician.models.data.UsedPart
import eci.technician.models.filters.CallPriorityFilter
import eci.technician.models.filters.TechnicianCallType
import eci.technician.models.lastUpdate.LastUpdate
import eci.technician.models.order.ServiceCallLabor
import eci.technician.models.order.ServiceOrder
import eci.technician.models.order.TechnicianWarehousePart
import eci.technician.repository.LastUpdateRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.tools.Constants
import eci.technician.workers.serviceOrderQueue.ServiceOrderOnlineManager
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UpdateManager {

    const val TAG = "UpdateManger"
    const val EXCEPTION = "Exception"

    /**
     * This method deletes the ServiceOrders that are not in the response when refreshing serviceOrders
     * @param serviceOrderListFromResponse is the list of ServiceOrders from the response
     */
    suspend fun deleteServiceOrdersThatAreNotInResponse(serviceOrderListFromResponse: MutableList<ServiceOrder>) {
        val realm = Realm.getDefaultInstance()
        try {
            val arrayOfServiceOrderId2: Array<Int> =
                serviceOrderListFromResponse.map { serviceOrder -> serviceOrder.callNumber_ID }
                    .toTypedArray()
            val serviceOrderToDelete = realm.where(ServiceOrder::class.java)
                .not()
                .`in`(ServiceOrder.CALL_NUMBER_ID, arrayOfServiceOrderId2)
                .findAll()
//            serviceOrderToDelete.forEach{
//                DatabaseRepository.getInstance().deleteTechnicianWarehouseParts(it.callNumber_ID)
//            }
            val usedPartsToDeleteFromDeletedServiceOrder = realm.where(UsedPart::class.java)
                .not()
                .`in`(UsedPart.CALL_ID, arrayOfServiceOrderId2)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .findAll()


            realm.executeTransaction {
                serviceOrderToDelete.deleteAllFromRealm()
                usedPartsToDeleteFromDeletedServiceOrder.deleteAllFromRealm()
            }


        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    /**
     * This method deletes the parts that are not in the response, excluding the parts that have been added locally
     * because there are SC still using them
     */
    suspend fun deletePartsThatAreNotInResponse(serviceOrdersFromResponse: MutableList<ServiceOrder>) {
        val partsFromResponse = getAllPartsFromResponse(serviceOrdersFromResponse)
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val partsToDelete = realm.where(UsedPart::class.java)
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
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    /**
     * This method deletes the labors that are not in the response
     * @param serviceOrdersFromResponse  receives the list of serviceOrders from the response
     */
    suspend fun deleteLaborsThatAreNotInResponse(serviceOrdersFromResponse: MutableList<ServiceOrder>) {
        val laborsFromResponse = getAllLaborsFromResponse(serviceOrdersFromResponse)
        val realm = Realm.getDefaultInstance()
        try {
            val laborsToDelete = realm.where(ServiceCallLabor::class.java)
                .not()
                .`in`(
                    ServiceCallLabor.LABOR_ID,
                    laborsFromResponse.map { it.laborId }.toTypedArray()
                )
                .findAll()
            realm.executeTransaction {
                laborsToDelete.deleteAllFromRealm() // delete-> deleted labors from EA
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun deleteLaborsThatAreNotInTheResponseByServiceCall(oneServiceOrderFromResponse: ServiceOrder) {
        val laborsFromResponse =
            updateLaborsLocally(oneServiceOrderFromResponse.labors.toMutableList())
        val realm = Realm.getDefaultInstance()
        try {
            val laborsToDelete = realm.where(ServiceCallLabor::class.java)
                .equalTo(ServiceCallLabor.CALL_ID, oneServiceOrderFromResponse.callNumber_ID)
                .and()
                .not()
                .`in`(
                    ServiceCallLabor.LABOR_ID,
                    laborsFromResponse.map { it.laborId }.toTypedArray()
                )
                .findAll()
            realm.executeTransaction {
                laborsToDelete.deleteAllFromRealm() // delete-> deleted labors from EA
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    /**
     * This method updates a single ServiceOrder
     */
    fun updateServiceOrderLocally(serviceOrder: ServiceOrder): ServiceOrder {
        val realm = Realm.getDefaultInstance()
        try {
            serviceOrder.parts = updateParts(serviceOrder.parts.toMutableList())
            serviceOrder.labors = updateLaborsLocally(serviceOrder.labors.toMutableList())
            val orderByStatus = serviceOrder.getStatusOrderForSorting()
            serviceOrder.statusOrder = orderByStatus
            val serviceOrderFromRealm = realm
                .where(ServiceOrder::class.java)
                .equalTo(ServiceOrder.CALL_NUMBER_ID, serviceOrder.callNumber_ID)
                .equalTo(ServiceOrder.COMPLETED, true).findFirst()
            if (serviceOrderFromRealm != null) {
                serviceOrder.completedCall = serviceOrderFromRealm.completedCall
            } else {
                serviceOrder.completedCall = false
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return serviceOrder
    }

    private fun updateLaborsLocally(labors: MutableList<ServiceCallLabor>): MutableList<ServiceCallLabor> {
        labors.forEach { labor -> labor.laborId = "${labor.callId}_${labor.technicianId}" }
        return labors
    }

    private fun updateParts(parts: MutableList<UsedPart>): MutableList<UsedPart> {
        parts.map { part -> updatePart(part) }

        return parts
    }

    /**
     * This methods updates a single Part, taking in account all the possible states of each part could have
     * because there are parts already loaded and manipulated
     */
    private fun updatePart(part: UsedPart): UsedPart {
        val realm = Realm.getDefaultInstance()
        try {
            part.customId = "${part.callId}_${part.itemId}_${part.detailId}_${part.warehouseID}"
            AppAuth.getInstance().technicianUser?.let {
                part.isSent = true
                part.isPartFromWarehouse = it.warehouseId == part.warehouseID
                part.isAddedLocally = false
            }
            val realmPart = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CUSTOM_USED_PART_ID, part.customId).findFirst()
            if (realmPart != null) {
                when (part.usageStatusId) {
                    Constants.USED_PART_USAGE_STATUS.NEEDED.value -> {
                        part.localUsageStatusId = part.usageStatusId
                    }
                    Constants.USED_PART_USAGE_STATUS.USED.value -> {
                        part.localUsageStatusId = part.usageStatusId

                    }
                    Constants.USED_PART_USAGE_STATUS.PENDING.value -> {
                        if (realmPart.localUsageStatusId == Constants.USED_PART_USAGE_STATUS.PENDING.value ||
                            realmPart.localUsageStatusId == Constants.USED_PART_USAGE_STATUS.USED.value
                        ) {
                            part.localUsageStatusId = realmPart.localUsageStatusId
                        } else {
                            part.localUsageStatusId = part.usageStatusId
                        }
                    }
                    else -> {
                        part.localUsageStatusId = part.usageStatusId
                    }
                }
                part.isHasBeenChangedLocally = realmPart.isHasBeenChangedLocally
                part.deletable = realmPart.deletable
                part.actionType =
                    if (realmPart.actionType.isEmpty()) (if (realmPart.deletable) "delete" else "update") else realmPart.actionType
            } else {
                part.localUsageStatusId = part.usageStatusId
                part.actionType = "update"
            }
            return part
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
            return part
        } finally {
            realm.close()
        }
    }

    /**
     * This method collects all the parts from the response
     */
    private fun getAllPartsFromResponse(serviceOrdersFromResponse: MutableList<ServiceOrder>): MutableList<UsedPart> {
        val usedPartsFromResponse: MutableList<UsedPart> = mutableListOf()
        serviceOrdersFromResponse.forEach {
            usedPartsFromResponse.addAll(it.parts)
        }
        return usedPartsFromResponse
    }

    /**
     * This method collects all the labors in the response
     */
    private fun getAllLaborsFromResponse(serviceOrdersFromResponse: MutableList<ServiceOrder>): MutableList<ServiceCallLabor> {
        val laborsFromResponse: MutableList<ServiceCallLabor> = mutableListOf()
        serviceOrdersFromResponse.forEach {
            laborsFromResponse.addAll(it.labors)
        }
        return laborsFromResponse
    }

    /**
     * This method update the technician warehouse parts
     */
    fun updateTechnicianWarehouseParts(parts: List<TechnicianWarehousePart>): List<TechnicianWarehousePart> {
        Log.d(TAG, "ON updateTechnicianWarehouseParts  WITH PARTS FORMWAREHOUSE ${parts.size}")
        val realm = Realm.getDefaultInstance()
        try {
            parts.forEach { part ->
                val usedParts =
                    realm.where(UsedPart::class.java).equalTo(UsedPart.ITEM_ID, part.itemId)
                        .equalTo(UsedPart.ADDED_LOCALLY, true).findAll()
                var usedPartsInApp = 0
                usedParts.forEachIndexed { index, usedPart ->
                    usedPartsInApp += usedPart.quantity.toInt()
                }
                part.usedQty = usedPartsInApp.toDouble()
            }
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        Log.d(TAG, "ON updateTechnicianWarehouseParts  will return  FORMWAREHOUSE ${parts.size}")

        return parts
    }

    fun updatePartsForOneCall(callId: Int, listOfPartsFromResponse: List<UsedPart>) {
        val parts = updateParts(listOfPartsFromResponse.toMutableList())
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(parts)
                val partsToDelete = realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, callId)
                    .not()
                    .`in`(
                        UsedPart.CUSTOM_USED_PART_ID,
                        parts.map { it.customId }.toTypedArray()
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
            Log.d(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun updateServiceCalls(
        serviceOrderListFromResponse: MutableList<ServiceOrder>,
        configuration: LastUpdateRepository.DBCacheConfiguration,
        lastUpdate: String
    ) {
        /**
         * [lastUpdate] empty means, the list is a complete list of all the activeServiceCalls
         */
        if (lastUpdate.isEmpty()){
            ServiceOrderRepository.assignIndexBasedOnRestriction(
                serviceOrderListFromResponse.toList()
            )
            val serviceOrderFilteredWithoutPendingChanges =
                serviceOrderListFromResponse.filter {
                    !ServiceOrderOnlineManager.hasPendingChanges(it.callNumber_ID)
                }

            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction {
                    //START FUNCTION TO UPDATE PRIORITIES LIST
                    val prioritiesList =
                        serviceOrderListFromResponse.map {
                            CallPriorityFilter(
                                "TECHNICIAN_CALL_${it.callPriority}",
                                false,
                                isFromTechnicianCalls = true,
                                priorityName = it.callPriority,
                                isChecked = false
                            )
                        }.distinctBy { it.priorityName }
                    realm.where(CallPriorityFilter::class.java).equalTo(
                        CallPriorityFilter.COLUMNS.IS_FROM_TECHNICIAN_CALLS,
                        true
                    ).findAll().deleteAllFromRealm()
                    realm.insertOrUpdate(prioritiesList)
                    //START FUNCTION TO UPDATE CALL_TYPE LIST
                    val callTypes = realm.where(CallType::class.java).findAll()
                    val callTypesFiltered = callTypes.filter {
                        serviceOrderListFromResponse.map { technicianCallServiceOrder -> technicianCallServiceOrder.callType }
                            .contains(it.callTypeDescription)
                    }
                    val technicianCallType = callTypesFiltered.map {
                        TechnicianCallType(
                            it.id,
                            it.active,
                            it.callTypeCode,
                            it.callTypeDescription,
                            it.callTypeId,
                            it.companyId,
                            false
                        )
                    }
                    realm.where(TechnicianCallType::class.java).findAll().deleteAllFromRealm()
                    realm.insertOrUpdate(technicianCallType)

                    val serviceOrdersFromResponseUpdated = mutableListOf<ServiceOrder>()
                    val serviceOrdersLaborsTemporalUpdate = mutableListOf<ServiceCallLabor>()
                    val serviceOrdersPartsTemporalUpdate = mutableListOf<UsedPart>()
                    for (serviceOrder in serviceOrderFilteredWithoutPendingChanges) {
                        val serviceOrderTemporalUpdate = updateServiceOrderLocally(serviceOrder)
                        serviceOrdersFromResponseUpdated.add(serviceOrderTemporalUpdate)
                        serviceOrdersLaborsTemporalUpdate.addAll(serviceOrderTemporalUpdate.labors)
                        serviceOrdersPartsTemporalUpdate.addAll(serviceOrderTemporalUpdate.parts)
                    }
                    realm.insertOrUpdate(serviceOrdersFromResponseUpdated)
                    realm.insertOrUpdate(serviceOrdersLaborsTemporalUpdate)
                    realm.insertOrUpdate(serviceOrdersPartsTemporalUpdate)
                    CoroutineScope(Dispatchers.IO).launch {
                        deleteServiceOrdersThatAreNotInResponse(serviceOrderListFromResponse)
                        deletePartsThatAreNotInResponse(serviceOrdersFromResponseUpdated)
                        deleteLaborsThatAreNotInResponse(serviceOrdersFromResponseUpdated)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                return
            } finally {
                realm.close()
            }
        }
        /**
         * [lastUpdate] not empty means, we only receive a list of updatedSC
         */
        else {
            serviceOrderListFromResponse.forEach {
                ServiceOrderRepository.saveServiceOrderFromResponse(it, false)
            }
        }
        LastUpdateRepository.updateCacheDate(configuration)
    }

}