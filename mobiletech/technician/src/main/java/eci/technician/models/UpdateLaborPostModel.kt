package eci.technician.models

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.AppAuth
import eci.technician.repository.PartsRepository


/**
 * Will change the status for the assistant
 * @param actionType
 * 1 -> Dispatch
 * 2 -> Arrive
 * 3 -> Complete
 */
data class UpdateLaborPostModel(

    @SerializedName("ActionType")
    var actionType: Int = -1,
    @SerializedName("CallId")
    var callId: Int = -1,
    @SerializedName("TechnicianId")
    var technicianId: Int = -1,
    @SerializedName("UsedParts")
    var usedParts: MutableList<UsedPartPostModel> = mutableListOf()
) {
    data class UsedPartPostModel(
        @SerializedName("CallId")
        var callId: Int = -1,
        @SerializedName("ItemId")
        var itemId: Int = -1,
        @SerializedName("BinId")
        var binId: Int = -1,
        @SerializedName("Quantity")
        var quantity: Double = 0.0,
        @SerializedName("UsageStatusId")
        var usageStatusId: Int = -1,
        @SerializedName("SerialNumber")
        var serialNumber: String? = null,
        @SerializedName("WarehouseId")
        var warehouseId: Int = -1
    )

    companion object {
        const val ACTION_TYPE_DISPATCH = 1
        const val ACTION_TYPE_ARRIVE = 2
        const val ACTION_TYPE_COMPLETE = 3

        fun createInstanceForComplete(orderId: Int): UpdateLaborPostModel {
            val parts = PartsRepository.getNotSentPartsByOrderId(orderId)
            val techId = AppAuth.getInstance().technicianUser.technicianNumber
            return UpdateLaborPostModel(
                ACTION_TYPE_COMPLETE,
                orderId,
                techId,
                parts.map {
                    UsedPartPostModel(
                        it.callId,
                        it.itemId,
                        it.binId,
                        it.quantity,
                        it.usageStatusId,
                        it.serialNumber,
                        it.warehouseID
                    )
                }.toMutableList()
            )
        }

    }
}
