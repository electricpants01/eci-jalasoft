package eci.technician.models.filters

import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass
open class CallTechnicianFilter(
        var technicianName: String? = "",
        var technicianNumberId: Int = 0,
        var isChecked: Boolean = false
) : RealmObject() {
    object COLUMNS {
        const val TECHNICIAN_NAME = "technicianName"
        const val TECHNICIAN_NUMBER_ID = "technicianNumberId"
    }
}