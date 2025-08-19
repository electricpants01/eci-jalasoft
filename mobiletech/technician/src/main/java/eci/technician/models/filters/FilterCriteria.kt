package eci.technician.models.filters

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class FilterCriteria(
    @PrimaryKey
    var id: Int = -1,
    var filterForGroup: Boolean = false,
    var filterForServiceOrderList: Boolean = false,
    var callTypeFilterSelected: String? = "",
    var callTypeIdFilterSelected: Int = -1,
    var callPrioritySelected: String? = "",
    var callPriorityIdSelected: String? = "",
    var callTechnicianNameSelected: String? = "",
    var callTechnicianNumberIdSelected: Int = -1,
    var isDateFilterOpen: Boolean = true,
    var isGroupFilterOpen: Boolean = true,
    var isCallTypeFilterOpen: Boolean = true,
    var isCallPriorityFilterOpen: Boolean = true,
    var isCallTechnicianFilterOpen: Boolean = true,
    var isCallStatusFilterOpen: Boolean = true,
    var isSortByDateOpen: Boolean = true,
    var callStatusSelected: Int = -1,
    var callSortItemSelected: Int = -1,
    var callDateSelected: Int = -1,
    var groupNameSelected: String? = ""
) : RealmObject() {
    object COLUMNS {
        const val ID = "id"
        const val isCallTypeFilterOpen = "isCallTypeFilterOpen"
        const val isCallPriorityFilterOpen = "isCallPriorityFilterOpen"
        const val isCallStatusFilterOpen = "isCallStatusFilterOpen"
        const val isDateFilterOpen = "isDateFilterOpen"
        const val isSortByDateOpen = "isSortByDateOpen"
    }
}