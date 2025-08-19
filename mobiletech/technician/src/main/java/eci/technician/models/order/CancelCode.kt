package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class CancelCode : RealmObject() {
    @PrimaryKey
    @SerializedName("CancelCodeId")
    var cancelCodeId = 0

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("Code")
    var code: String? = null

    @SerializedName("Active")
    var isActive = false

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null
    var checked = false
}