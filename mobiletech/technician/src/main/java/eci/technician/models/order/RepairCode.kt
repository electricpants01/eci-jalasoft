package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class RepairCode : RealmObject() {
    @PrimaryKey
    @SerializedName("RepairCodeId")
    var repairCodeId:Int = 0

    @SerializedName("RepairCodeName")
    var repairCodeName: String? = null

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("Active")
    var isActive:Boolean = false

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null

    companion object {
        const val REPAIR_CODE_NAME_QUERY = "repairCodeName"
    }
}