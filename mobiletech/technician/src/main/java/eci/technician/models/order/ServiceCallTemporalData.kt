package eci.technician.models.order

import android.graphics.Bitmap
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ServiceCallTemporalData(


) : RealmObject() {
    @PrimaryKey
    var id: Int? = null
    var incompleteCodeId: String? = null
    var activityCodeId: Int? = null
    var signatureByteArray: ByteArray? = null
    var signatureName: String? = null
    var preventiveMaintenance:Boolean = false
    var description: String? = null

    object COLUMNS {
        const val CALL_NUMBER_ID = "id"
    }
}
