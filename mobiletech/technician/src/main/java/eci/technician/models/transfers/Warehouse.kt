package eci.technician.models.transfers

import com.google.gson.annotations.SerializedName

data class Warehouse(
    @SerializedName("WarehouseID") val warehouseID: Int,
    @SerializedName("Warehouse") var warehouse: String?,
    @SerializedName("WarehouseTypeID") var warehouseTypeID: Int,
    @SerializedName("Description") val description: String,
    @SerializedName("Bins") val bins: List<Bin>
) {
    var isTechnicianWarehouse = false
    companion object {
        const val COMPANY_TYPE = 1
        const val CUSTOMER_TYPE = 2
        const val TECH_TYPE = 3
    }
}