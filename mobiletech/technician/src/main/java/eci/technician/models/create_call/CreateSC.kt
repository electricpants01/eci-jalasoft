package eci.technician.models.create_call

import com.google.gson.annotations.SerializedName
import java.util.*

data class CreateSC(
        @SerializedName("Caller")
        var caller: String? = null,
        @SerializedName("LocationRemarks")
        var locationRemarks: String? = null,
        @SerializedName("Description")
        var description: String? = null,
        @SerializedName("EquipmentId")
        var equipmentId: Int? = null,
        @SerializedName("CallTypeId")
        var callTypeId: Int? = null,
        @SerializedName("CallReceivedDate")
        var requestDate: Date? = null,
        @SerializedName("TechnicianId")
        var technicianId: Int? = null
) {

}