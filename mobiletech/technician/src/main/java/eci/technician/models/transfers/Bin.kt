package eci.technician.models.transfers

import com.google.gson.annotations.SerializedName

data class Bin(

    @SerializedName("BinId") val binId: Int,
    @SerializedName("Bin") val bin: String?,
    @SerializedName("BinAvailableQty") val binAvailableQty: Double,
    @SerializedName("BinDiscription") val binDescription: String?,
    @SerializedName("Description") val description: String?,
    @SerializedName("SerialNumber") val serialNumber: String?,
    @SerializedName("Id") val id: String,
    @SerializedName("IsDefaultBin") val isDefaultBin: Boolean,
) {
    var updatedBinQty: Double = binAvailableQty
}

