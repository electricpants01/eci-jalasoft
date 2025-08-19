package eci.technician.models.time_cards

import android.content.Context
import eci.technician.R
import eci.technician.helpers.DateTimeHelper
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

data class ShiftUI(
    val shiftId: String = "",
    val title: String,
    val isShiftClosed: Boolean,
    val totalHours: String,
    val date: Date?
) {
    companion object {
        fun mapToShiftUI(shift: Shift, context: Context): ShiftUI {
            return ShiftUI(
                shiftId = shift.shiftId ?: "",
                title = getTitle(shift, context),
                isShiftClosed = shift.isShiftClosed,
                totalHours = getTotalHours(shift, context),
                date = getDate(shift)
            )
        }

        private fun getDate(shift: Shift): Date? {
            val dateString = shift.date ?: return null
            return DateTimeHelper.getDateFromString(dateString)
        }

        private fun getTitle(shift: Shift, context: Context): String {
            var res = ""
            shift.date?.let {
                val date = DateTimeHelper.getDateFromStringWithoutTimeZone(it)
                date?.let {
                    res = DateTimeHelper.formatDateYear(date, context)
                }
            }
            if (!shift.isShiftClosed) {
                return context.getString(R.string.shift_current, res)
            }
            return res
        }

        private fun getTotalHours(shift: Shift, context: Context): String {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.roundingMode = RoundingMode.DOWN
            decimalFormat.minimumFractionDigits = 2
            val hours = decimalFormat.format(shift.totalHours)
            return context.getString(R.string.shift_total_hours, hours)
        }
    }
}