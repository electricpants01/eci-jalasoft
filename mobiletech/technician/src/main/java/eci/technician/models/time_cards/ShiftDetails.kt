package eci.technician.models.time_cards

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DateTimeHelper.formatTime
import eci.technician.helpers.TimeCardStatusHelper
import eci.technician.tools.Constants
import io.realm.RealmObject
import eci.technician.R
import java.util.Date

open class ShiftDetails : RealmObject() {
    @SerializedName("Reason")
    var reason: String? = null

    @SerializedName("Action")
    var action: String? = null
        get() = TimeCardStatusHelper.getInstance().getStatus(field)
        set(value) { field = TimeCardStatusHelper.getInstance().getStatus(value)}
    var closeAction = ""

    @SerializedName("State")
    var state = 0

    @SerializedName("Timestamp")
    var date: Date? = null
    var closeDate: Date? = null
    var shiftId: String? = null
    val formattedTime: String
        get() = formatTime((date)!!)
    val formattedCloseTime: String
        get() = if (closeDate == null) "" else formatTime(closeDate!!)
    val friendlyAction: String
        get() = when (TimeCardStatusHelper.getInstance().getStatus(action)) {
            Constants.STATUS_SIGNED_IN -> "Clocked In"
            Constants.STATUS_SIGNED_OUT -> "Clocked Out"
            Constants.STATUS_BRAKE_IN -> "Started Break"
            Constants.STATUS_BRAKE_OUT -> "Ended Break"
            Constants.STATUS_LUNCH_IN -> "Started Lunch"
            Constants.STATUS_LUNCH_OUT -> "Ended Lunch"
            else -> ""
        }
    val friendlyCloseAction: String
        get() {
            return when (TimeCardStatusHelper.getInstance().getStatus(closeAction)) {
                Constants.STATUS_SIGNED_IN -> "Clocked In"
                Constants.STATUS_SIGNED_OUT -> "Clocked Out"
                Constants.STATUS_BRAKE_IN -> "Started Break"
                Constants.STATUS_BRAKE_OUT -> "Ended Break"
                Constants.STATUS_LUNCH_IN -> "Started Lunch"
                Constants.STATUS_LUNCH_OUT -> "Ended Lunch"
                else -> ""
            }
        }

    fun hasCloseAction(): Boolean {
        return closeDate != null
    }

    val friendlyActionLetter: String
        get() {
            return when (TimeCardStatusHelper.getInstance().getStatus(action)) {
                Constants.STATUS_SIGNED_IN -> "C"
                Constants.STATUS_SIGNED_OUT -> "C"
                Constants.STATUS_BRAKE_IN -> "B"
                Constants.STATUS_BRAKE_OUT -> "B"
                Constants.STATUS_LUNCH_IN -> "L"
                Constants.STATUS_LUNCH_OUT -> "L"
                else -> ""
            }
        }
    val circleDrawable: Int
        get() {
            return when (TimeCardStatusHelper.getInstance().getStatus(action)) {
                Constants.STATUS_SIGNED_IN -> 1
                Constants.STATUS_SIGNED_OUT -> 1
                Constants.STATUS_BRAKE_IN -> 2
                Constants.STATUS_BRAKE_OUT -> 2
                Constants.STATUS_LUNCH_IN -> 3
                Constants.STATUS_LUNCH_OUT -> 3
                else -> R.mipmap.ic_launcher
            }
        }

    companion object {
        const val SHIFT_ID = "shiftId"
        const val TIMESTAMP = "date"
    }
}