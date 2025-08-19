package eci.technician.models.data

import android.content.Context
import android.content.res.Resources
import androidx.core.content.ContextCompat
import com.google.gson.annotations.SerializedName
import eci.technician.R
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class UsedPart : RealmObject() {
    /**
     * For the "usageStatusId"  column
     * 1  -> used
     * 2  -> needed
     * 3  -> pending
     */
    @PrimaryKey
    var customId: String

    @SerializedName("CallId")
    var callId: Int = 0

    @SerializedName("ItemId")
    var itemId: Int = 0

    @SerializedName("BinId")
    var binId: Int = 0

    @SerializedName("Bin")
    var binName: String? = ""

    @SerializedName("Quantity")
    var quantity: Double = 0.0

    @SerializedName("UsageStatusId")
    var usageStatusId: Int = 0

    @SerializedName("Item")
    var partName: String? = null

    @SerializedName("Description")
    var partDescription: String? = null

    @SerializedName("DetailID")
    var detailId: Int = 0

    @SerializedName("WarehouseId")
    var warehouseID: Int = 0

    @SerializedName("WareHouseName")
    var warehouseName: String? = null

    @SerializedName("ActionType")
    var actionType: String = ""

    @SerializedName("SerialNumber")
    var serialNumber: String? = ""

    var deletable: Boolean = false
    var isDeleteWhenRefreshing: Boolean = true
    var isPartFromWarehouse: Boolean = false
    var isAddedLocally: Boolean = false
    var isSent: Boolean = false
    var isHasBeenChangedLocally: Boolean = false
    var localUsageStatusId: Int = 0
    var localDescription: String? = null
    var holdCodeId: Int = 0



    init {
        customId = "${callId}_${itemId}_${detailId}_${warehouseID}"
    }

    companion object {
        const val CALL_ID = "callId"
        const val DETAIL_ID = "detailId"
        const val ITEM_ID = "itemId"
        const val BIN_ID = "binId"
        const val WAREHOUSE_ID = "warehouseID"
        const val QUANTITY = "quantity"
        const val USAGE_STATUS_ID = "usageStatusId"
        const val LOCAL_USAGE_STATUS_ID = "localUsageStatusId"
        const val PART_NAME = "partName"
        const val PART_DESCRIPTION = "partDescription"
        const val SENT = "isSent"
        const val PART_FROM_WAREHOUSE = "isPartFromWarehouse"
        const val ADDED_LOCALLY = "isAddedLocally"
        const val WAREHOUSE_NAME = "warehouseName"
        const val DELETABLE = "deletable"
        const val DELETABLE_WHEN_REFRESHING = "isDeleteWhenRefreshing"
        const val CUSTOM_USED_PART_ID = "customId"
        const val CHANGED_LOCALLY = "isHasBeenChangedLocally"
        const val HOLD_CODE_ID = "holdCodeId"
        const val ACTION_TYPE = "actionType"
        const val SERIAL_NUMBER = "serialNumber"

        const val USED_STATUS_CODE = 1
        const val NEEDED_STATUS_CODE = 2
        const val PENDING_STATUS_CODE = 3
        const val ACTION_INSERT = "insert"
        const val ACTION_UPDATE = "update"
        const val ACTION_DELETE = "delete"

        fun createInstance(
            callId: Int,
            itemId: Int,
            partName: String,
            partDescription: String,
            quantity: Double,
            localUsageStatus: Int,
            warehouseId: Int,
            warehouseName: String,
            bindId: Int,
            binName: String,
            serialNumber:String
        ): UsedPart {
            val usedPart = UsedPart()
            usedPart.customId = UUID.randomUUID().toString()
            usedPart.callId = callId
            usedPart.itemId = itemId
            usedPart.quantity = quantity
            usedPart.localUsageStatusId = localUsageStatus
            usedPart.usageStatusId = localUsageStatus
            usedPart.isSent = false
            usedPart.isAddedLocally = true
            usedPart.partName = partName
            usedPart.warehouseID = warehouseId
            usedPart.binId = bindId
            usedPart.actionType = ACTION_INSERT
            usedPart.warehouseName = warehouseName
            usedPart.partDescription = partDescription
            usedPart.binName = binName
            usedPart.serialNumber = serialNumber
            return usedPart
        }

        fun createNeededPartInstance(
            callId: Int,
            itemId: Int,
            partName: String,
            partDescription: String,
            quantity: Double,
            localUsageStatus: Int,
            warehouseId: Int,
            warehouseName: String,
            bindId: Int,
            holdCodeId: Int = -1
        ): UsedPart {
            val usedPart = UsedPart()
            usedPart.customId = UUID.randomUUID().toString()
            usedPart.callId = callId
            usedPart.itemId = itemId
            usedPart.quantity = quantity
            usedPart.localUsageStatusId = localUsageStatus
            usedPart.usageStatusId = localUsageStatus
            usedPart.isSent = false
            usedPart.isAddedLocally = true
            usedPart.partName = partName
            usedPart.warehouseID = warehouseId
            usedPart.binId = bindId
            usedPart.actionType = ACTION_INSERT
            usedPart.warehouseName = warehouseName
            usedPart.partDescription = partDescription
            usedPart.localDescription = partDescription
            if (holdCodeId != -1) {
                usedPart.holdCodeId = holdCodeId
            }

            return usedPart
        }

        fun getFormattedUsedPartData(usedPartsData: List<UsedPart>, currentWarehouseId: Int, context: Context): String{
            val sb = StringBuilder()
            val usedPartSize = usedPartsData.size
            usedPartsData.forEachIndexed { index, part ->
                sb.append(context.resources.getString(R.string.used_part_item,part.partName))
                sb.append("\n")
                sb.append(context.resources.getString(R.string.used_part_description,part.partDescription))
                sb.append("\n")
                sb.append(String.format(Locale.getDefault(),context.resources.getString(R.string.used_part_quantity),part.quantity))
                if (part.warehouseID != currentWarehouseId) {
                    sb.append("\n")
                    sb.append(context.resources.getString(R.string.used_part_warehouse_name,part.warehouseName))
                }
                if (index != usedPartSize - 1) sb.append("\n\n")
                else sb.append("\n")
            }
            return sb.toString()
        }
    }

    fun isFromTechWarehouse(technicianWarehouseId: Int): Boolean {
        return technicianWarehouseId == warehouseID
    }

}