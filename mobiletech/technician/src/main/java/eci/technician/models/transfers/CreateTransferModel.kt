package eci.technician.models.transfers

import com.google.gson.annotations.SerializedName

data class CreateTransferModel(

    @SerializedName("Description") val description: String,
    @SerializedName("DestinationWarehouseId") val destinationWarehouseId: Int,
    @SerializedName("DestinationBinId") val destinationBinId: Int,
    @SerializedName("ItemId") val itemId: Int,
    @SerializedName("Quantity") val quantity: Int,
    @SerializedName("SerialNumber") val serialNumber: String,
    @SerializedName("SourceWarehouseId") val sourceWarehouseId: Int,
    @SerializedName("SourceBinId") val sourceBinId: Int
)