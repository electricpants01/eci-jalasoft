package eci.technician.models.filters

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class CallPriorityFilter(
        @PrimaryKey
        var id: String = "",
        var isFromGroupCalls: Boolean = false,
        var isFromTechnicianCalls: Boolean = false,
        var priorityName: String? = "",
        var isChecked: Boolean = false
) : RealmObject() {
    object COLUMNS {
        const val ID = "id"
        const val IS_CHECKED = "isChecked"
        const val PRIORITY_NAME = "priorityName"
        const val IS_FROM_GROUP_CALLS = "isFromGroupCalls"
        const val IS_FROM_TECHNICIAN_CALLS = "isFromTechnicianCalls"
    }
}