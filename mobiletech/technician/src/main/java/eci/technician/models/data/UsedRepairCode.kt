package eci.technician.models.data

import io.realm.RealmObject

open class UsedRepairCode : RealmObject() {
    var callId: Int = 0
    var repairCodeId: Int = 0
    var repairCodeName: String? = null
    var description: String? = null

    companion object {
        const val CALL_ID = "callId"
        const val REPAIR_CODE_NAME = "repairCodeName"
    }
}