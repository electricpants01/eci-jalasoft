package eci.technician.models.equipment

import com.google.gson.annotations.SerializedName

data class EquipmentSearchModel(
        @SerializedName("EquipmentCode")
        val equipmentCode: String? = "",
        @SerializedName("EquipmentId")
        val equipmentId: Int?     = -1,
        @SerializedName("ItemCode")
        val itemCode: String? = "",
        @SerializedName("MakeCode")
        val makeCode: String? = "",
        @SerializedName("ModelCode")
        val modelCode: String? = "",
        @SerializedName("SerialNumber")
        val serialNumber: String? = "",
        )