package eci.technician.models.sort

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ServiceOrderSort(
    @PrimaryKey
    var callNumberCode:String? = "",
    var index:Int? = -2
) : RealmObject() {
    companion object {
        const val CALL_NUMBER_CODE = "callNumberCode"
        const val HAS_OFFLINE_CHANGES = -1
    }
}
