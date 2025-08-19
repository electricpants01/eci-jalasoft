package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DecimalsHelper
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.io.Serializable

@RealmClass
open class Part : RealmObject() {
    @PrimaryKey
    var customId: String = ""

    @SerializedName("ItemId")
    var itemId = 0

    @SerializedName("Item")
    var item: String? = null

    @SerializedName("BinId")
    var bindId: Int = 0

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("AvailableQty")
    var availableQty: Double = 0.0

    @SerializedName("DefaultPrice")
    var defaultPrice: Double = 0.0

    @SerializedName("Warehouse")
    var warehouse: String? = null

    @SerializedName("isTechnicianWarehouse")
    var isTechnicianWarehouse: Boolean = false

    @SerializedName("WarehouseId")
    var warehouseID: Int = 0
    var localActionType: String = ""


    companion object {
        const val CUSTOM_ID = "customId"
        const val ITEM_ID = "itemId"
        const val ITEM = "item"
    }

    fun amountWithCurrency(): String {
        return DecimalsHelper.getAmountWithCurrency(defaultPrice)
    }

    fun generateCustomId() {
        this.customId = "${itemId}"
    }

    fun getSerializedPart(): SerializedPart {
        val serializedPart = SerializedPart()
        serializedPart.customId = customId
        serializedPart.itemId = itemId
        serializedPart.bindId = bindId
        serializedPart.description = description
        serializedPart.availableQty = availableQty
        serializedPart.isTechnicianWarehouse = isTechnicianWarehouse
        serializedPart.warehouseID = warehouseID
        return serializedPart
    }

    inner class SerializedPart : Serializable {

        var customId: String = ""
        var itemId = 0
        var item: String? = null
        var bindId: Int = 0
        var description: String? = null
        var availableQty: Double = 0.0
        var defaultPrice: Double = 0.0
        var warehouse: String? = null
        var isTechnicianWarehouse: Boolean = false
        var warehouseID: Int = 0
        var localActionType: String = ""
    }
}

