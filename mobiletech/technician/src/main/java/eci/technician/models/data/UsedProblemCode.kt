package eci.technician.models.data

import io.realm.RealmObject

open class UsedProblemCode : RealmObject() {
    var callId:Int = 0
    var problemCodeId:Int = 0
    var problemCodeName: String? = null
    var description: String? = null

    companion object {
        const val CALL_ID = "callId"
        const val PROBLEM_CODE_NAME = "problemCodeName"
    }
}