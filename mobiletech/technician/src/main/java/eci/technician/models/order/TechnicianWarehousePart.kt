package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey

open class TechnicianWarehousePart(


) : RealmObject() {

    @PrimaryKey
    var customId: String = ""

    @SerializedName("ItemId")
    var itemId: Int = 0

    @SerializedName("Item")
    var item: String? = null

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("AvailableQty")
    var availableQty: Double = 0.0

    @SerializedName("DefaultPrice")
    var defaultPrice: Double = 0.0

    @SerializedName("Warehouse")
    var warehouse: String? = null

    @SerializedName("WarehouseTypeID")
    var warehouseType: Int = 0

    @SerializedName("IsTechnicianWarehouse")
    var isTechnicianWarehouse: Boolean = false

    @SerializedName("Bins")
    var bins: RealmList<Bin> = RealmList()

    var usedQty: Double = 0.0

    @SerializedName("WarehouseId")
    var warehouseID: Int = 0

    /**
     * Use isDisabled to make some UI changes, set comparing with customer/techWarehouse availability
     */
    @Ignore
    var isDisabled: Boolean = false

    /**
     * Use availableQuantityUI for UI purpose
     */
    @Ignore
    var availableQuantityUI: Double = 0.0


    object COLUMNS {
        const val ITEM = "item"
        const val ITEM_ID = "itemId"
        const val CUSTOM_ID = "customId"
        const val IS_TECHNICIAN_WAREHOUSE = "isTechnicianWarehouse"
        const val WAREHOUSE_ID = "warehouseID"
    }

    fun generateCustomId() {
        customId = "${itemId}_${warehouseID}"
    }

    fun getCalculatedQuantityInDatabase(): Double {
        availableQuantityUI = bins.map { it.updateAvailableQuantityUI(itemId, warehouseID) }.sum()
        return availableQuantityUI

    }

    fun updateAvailableQuantityUIForMyWarehouse(): Double {
        val usedInDB: Double = bins.map {
            it.usedPartsInDB(partItemId = itemId, warehouseID)
        }.sum()
        availableQuantityUI = availableQty - usedInDB
        return availableQuantityUI
    }

    fun isFromTechWarehouse(technicianWarehouseId: Int): Boolean {
        return technicianWarehouseId == warehouseID
    }
}

