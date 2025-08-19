package eci.technician.models.transfers

import com.google.gson.annotations.SerializedName
import eci.technician.repository.PartsRepository

data class Part(
    @SerializedName("Warehouse") val warehouse: String,
    @SerializedName("WarehouseId") val warehouseId: Int,
    @SerializedName("ItemId") val itemId: Int,
    @SerializedName("Item") val item: String,
    @SerializedName("VendorItem") val vendorItem: String,
    @SerializedName("Description") val description: String,
    @SerializedName("AvailableQty") val availableQty: Double,
    @SerializedName("DefaultPrice") val defaultPrice: Double,
    @SerializedName("BinId") val binId: Int,
    @SerializedName("IsTechnicianWarehouse") val isTechnicianWarehouse: Boolean,
    @SerializedName("WarehouseTypeID") val warehouseTypeID: Int,
    @SerializedName("Bins") var bins: List<Bin>,
    @SerializedName("Id") val id: String
) {
    var updatedAvailableQty: Double = availableQty

    fun getCalculatedQuantityInDatabase(): Double {
        updatedAvailableQty = bins.map { it.updatedBinQty }.sum()
        return updatedAvailableQty

    }

}