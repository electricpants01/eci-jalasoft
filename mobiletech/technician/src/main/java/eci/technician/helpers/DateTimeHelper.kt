package eci.technician.helpers

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import eci.technician.tools.Constants
import java.text.SimpleDateFormat
import java.util.*


object DateTimeHelper {
    fun formatTimeDateWithDay(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
        )
    }

    fun formatTimeDate(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
    }

    fun formatTimeDate(date: Date, context: Context): String {
        return DateUtils.formatDateTime(
            context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
    }

    fun formatTimeDateYear(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatDateYear(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatDateYear(date: Date, context: Context): String {
        return DateUtils.formatDateTime(
            context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatFullDateYear(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatDate(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE
        )
    }

    fun formatTime(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_TIME
        )
    }

    fun formatTime(date: Date, context: Context): String {
        return DateUtils.formatDateTime(
            context,
            date.time,
            DateUtils.FORMAT_SHOW_TIME
        )
    }

    fun formatTimeDateYearPhoto(date: Date): String {
        return DateUtils.formatDateTime(
            AppAuth.getInstance().context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        ) + ", " + DateFormat.format("hh:mm:ss a", date).toString()
    }

    fun formatTimeDataAllowedPhotoName(date: Date): String {
        return DateFormat.format("MMM_d_yyyy_HHmmss", date).toString()
    }

    fun getShortFormattedDate(date: Date): String {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(date)
    }

    fun getDateFromStringWithoutTimeZone(dateString: String): Date? {
        return try {
            val backendFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
            val format = SimpleDateFormat(backendFormat, Locale.getDefault())
            val date = format.parse(dateString)
            date
        } catch (e: Error) {
            null
        }
    }

    fun getDateFromString(dateString: String): Date? {
        return try {
            val backendFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS Z"
            val format = SimpleDateFormat(backendFormat, Locale.getDefault())
            val date = format.parse(dateString)
            date
        } catch (e: Error) {
            null
        }
    }

    fun formatDateToBackendFormat(date: Date): String {
        val backendFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS Z"
        return DateFormat.format(backendFormat, date).toString()
    }

    fun Date.toBackendFormat(): String {
        val backendFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS Z"
        val format = SimpleDateFormat(backendFormat, Locale.getDefault())
        return format.format(this)
    }

    fun Date.toTimeCardFormat(): String {
        val stringFormat = "yyyy-MM-dd"
        val format = SimpleDateFormat(stringFormat, Locale.getDefault())
        return format.format(this)
    }

    fun Date.minus31Days(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = this
        calendar.add(Calendar.DAY_OF_MONTH, -31)
        return calendar.time
    }

    fun Date.plusMinutes(minutes: Int): Date {
        val minutesInMillis = minutes * 60 * 1000
        val timeDate = this.time + minutesInMillis
        return Date(timeDate)
    }

    fun Date.minusMinutes(minutes: Int): Date {
        val minutesInMillis = minutes * 60 * 1000
        val timeDate = this.time - minutesInMillis
        return Date(timeDate)
    }

    fun Date.toCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = this
        return calendar
    }

    fun getDateFromGMTString(dateString: String): Date? {
        return try {
            val backendFormat = "yyy-MM-dd'T'HH:mm:ss"
            val format = SimpleDateFormat(backendFormat, Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(dateString)
            date
        } catch (e: Exception) {
            null
        }
    }


    fun getDateFromStringForAttachment(dateString: String): Date {
        return try {
            val backendFormat = "yyyy-MM-dd HH:mm:ss"
            val format = SimpleDateFormat(backendFormat, Locale.getDefault())
            val date = format.parse(dateString)
            return date ?: Date()
        } catch (e: Error) {
            Date()
        }
    }

    fun getPMDateFromString(dateString: String): Date? {
        return try {
            val backendFormat = "yyyy-MM-dd"
            val format = SimpleDateFormat(backendFormat, Locale.getDefault())
            val date = format.parse(dateString)
            date
        } catch (e: Error) {
            null
        }
    }

    fun getTimeBetweenCurrentTimeAndDate(date: Date): Int {
        val currentTime = Calendar.getInstance()
        val incomingDate = Calendar.getInstance()
        incomingDate.time = date
        val differenceInMillis = currentTime.timeInMillis - incomingDate.timeInMillis
        return (differenceInMillis / Constants.MINUTE_IN_MILLIS).toInt()
    }
}
