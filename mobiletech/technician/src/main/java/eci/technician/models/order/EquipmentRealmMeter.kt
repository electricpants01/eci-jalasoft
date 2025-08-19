package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class EquipmentRealmMeter : RealmObject() {
    @SerializedName("EquipmentId")
    var equipmentId = 0

    @SerializedName("MeterType")
    var meterType: String? = null

    @SerializedName("Display")
    var display:Double? = null // null for empty meter from eauto

    @SerializedName("Actual")
    var actual:Double?  = null // null for empty meter from eauto

    @SerializedName("MeterTypeId")
    var meterTypeId = 0

    @PrimaryKey
    @SerializedName("MeterId")
    var meterId = 0

    @SerializedName("Required")
    var isRequired = false

    @SerializedName("RequireMeteronServiceCalls")
    var isRequiredMeterOnServiceCalls = false

    @SerializedName("IsValid")
    var isValidMeter = false
    var isMeterSet = false
    var initialDisplay = 0.0
    var userLastMeter = 0.0

    companion object {
        const val EQUIPMENT_ID = "equipmentId"
        const val METER_ID = "meterId"
    }
}