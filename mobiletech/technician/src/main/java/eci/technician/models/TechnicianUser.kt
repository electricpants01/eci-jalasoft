package eci.technician.models

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.TimeCardStatusHelper


class TechnicianUser {
    @SerializedName("Id")
    var id: String? = null

    @SerializedName("TechnicianNumber")
    var technicianNumber = 0

    @SerializedName("TechnicianName")
    var technicianName: String? = null

    @SerializedName("TechnicianCode")
    var technicianCode: String? = null

    @SerializedName("Username")
    var username: String? = null

    @SerializedName("Status")
    var status: String? = null

    @SerializedName("AllowUndispatchCallAfterArrive")
    var isAllowUndispatchCallAfterArrive = false

    @SerializedName("AllowScheduleCall")
    var isAllowScheduleCall = false

    @SerializedName("AllowCancelCall")
    var isAllowCancelCall = false

    @SerializedName("AllowOnHold")
    var isAllowOnHold = false

    @SerializedName("CanUseAppWithoutGps")
    var isCanUseAppWithoutGps = true

    @SerializedName("CanUseGps")
    var isCanUseGps = true

    @SerializedName("AllowAddMaterial")
    var isAllowAddMaterial = false

    @SerializedName("AllowLaborRecordEdits")
    var isAllowEditLaborRecord = false

    @SerializedName("IsFileAttachementEnabled")
    var isFileAttachmentEnabled = false

    @SerializedName("IsEmailSetupEnabled")
    var isEmailSetupEnabled = false

    @SerializedName("GroupInfo")
    var groupInfo: List<GroupInfo>? = null

    @SerializedName("Technician_ID")
    var technicianId = 0

    @SerializedName("Email")
    var email: String? = null

    @SerializedName("FirstName")
    var firstName: String? = null

    @SerializedName("LastName")
    var lastName: String? = null

    @SerializedName("WarehouseId")
    var warehouseId = 0

    @SerializedName("CanUseChat")
    var isCanUseChat = false

    @SerializedName("AllowReassignment")
    var isAllowReassignment = false

    @SerializedName("AllowMeterForce")
    var isAllowMeterForce = false

    @SerializedName("AllowUnknownItems")
    var isAllowUnknownItems = false

    @SerializedName("AllowCreateCall")
    var isAllowCreateCall = false

    @SerializedName("AllowServiceCallNotes")
    var isAllowServiceCallNotes = false

    @SerializedName("RestrictCallOrder")
    var isRestrictCallOrder = false

    @SerializedName("RestrictCallOrderValue")
    val restrictCallOrderValue = -1

    @SerializedName("AllowWarehouseTransfers")
    var isAllowWarehousesTransfers = false

    val state: Int
        get() = TimeCardStatusHelper.getInstance().getState(status)

    fun friendlyState(): String {
        return when (state) {
            1 -> "Clocked In"
            2 -> "Clocked Out"
            3 -> "Started Lunch"
            4 -> "Clocked In"
            5 -> "Started Break"
            6 -> "Clocked In"
            else -> "Unknown"
        }
    }

    fun canClockIn(): Boolean {
        return state == 2
    }

    fun canClockOut(): Boolean {
        return state == 1 || state == 4 || state == 6
    }

    fun canLunchIn(): Boolean {
        return state == 1 || state == 4 || state == 6
    }

    fun canLunchOut(): Boolean {
        return state == 3
    }

    fun canBrakeIn(): Boolean {
        return state == 1 || state == 4 || state == 6
    }

    fun canBrakeOut(): Boolean {
        return state == 5
    }

    val isNotClockedIn: Boolean
        get() = (state != 1) && (state != 4) && (state != 6)
    val isClockedOut: Boolean
        get() = state == 2
    val canCreateCallPermission: Boolean
        get() = try {
            isAllowCreateCall
        } catch (e: Exception) {
            false
        }
    val isBreakOrLunchStarted: Boolean
        get() = state == 3 || state == 5
}
