package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class ActivityCode : RealmObject() {
    @PrimaryKey
    @SerializedName("ActivityCodeID")
    var activityCodeId = 0

    @SerializedName("ActivityCodeName")
    var activityCodeName: String? = null

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("ActivityCodeCategoryID")
    var activityCodeCategoryId = 0

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null

    companion object {
        const val ACTIVITY_CODE_NAME_QUERY = "activityCodeName"
        const val ACTIVITY_CODE_ID = "activityCodeId"
    }
}