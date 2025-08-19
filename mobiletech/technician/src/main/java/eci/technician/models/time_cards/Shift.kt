package eci.technician.models.time_cards

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DateTimeHelper.formatDateYear
import eci.technician.helpers.DateTimeHelper.getDateFromStringWithoutTimeZone
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.math.RoundingMode
import java.text.DecimalFormat

open class Shift : RealmObject() {
    @PrimaryKey
    @SerializedName("ShiftId")
    var shiftId: String? = null

    @SerializedName("Date")
    var date: String? = null

    @SerializedName("IsShiftClosed")
    var isShiftClosed = false

    @SerializedName("TotalHours")
    var totalHours = 0.0
    var payPeriodId: String? = null
    val title: String
        get() {
            val e = getDateFromStringWithoutTimeZone(date!!)
            var s = ""
            if (e != null) {
                s = formatDateYear(e)
            }
            if (!isShiftClosed) {
                s = "$s (current)"
            }
            return s
        }
    val formattedTotalHours: String
        get() = String.format("Total hours: %s", truncatedTotalHours)

    private val truncatedTotalHours: String
        private get() {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.roundingMode = RoundingMode.DOWN
            decimalFormat.minimumFractionDigits = 2
            return decimalFormat.format(totalHours)
        }

    companion object {
        const val PAY_PERIOD_ID = "payPeriodId"
        const val SHIFT_ID = "shiftId"
        const val DATE = "date"
    }
}