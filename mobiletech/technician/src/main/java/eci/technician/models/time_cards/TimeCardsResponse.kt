package eci.technician.models.time_cards

import com.google.gson.annotations.SerializedName

data class TimeCardsItemResponse(
    @SerializedName("TimeCardEntryDate")
    val timeCardEntryDate: String?,
    @SerializedName("TimeCardClocks")
    val timeCardClocks: List<TimeCardClock>
)

data class TimeCardClock(
    @SerializedName("ClockIn")
    val clockIn: String?,
    @SerializedName("ClockInString")
    val clockInString: String?,
    @SerializedName("ClockOut")
    val clockOut: String?,
    @SerializedName("ClockOutString")
    val clockOutString: String?,
    @SerializedName("TimeEntries")
    val timeEntries: List<TimeEntry>
)

data class TimeEntry(
    @SerializedName("ActivityCode")
    val activityCode: String?,
    @SerializedName("ActivityCodeDescription")
    val activityCodeDescription: String?,
    @SerializedName("ActivityCodeID")
    val activityCodeId: Int,
    @SerializedName("Description")
    val description: String?,
    @SerializedName("Hours")
    val hours: Double,
    @SerializedName("IsConflict")
    val isConflict: Boolean,
    @SerializedName("IsGap")
    val isGap: Boolean,
    @SerializedName("IsSplitted")
    val isSplit: Boolean,
    @SerializedName("Preference")
    val preference: Int,
    @SerializedName("Source")
    val source: String?,
    @SerializedName("TempEntryID")
    val tempEntryId: Int,
    @SerializedName("TimeEntryType")
    val timeEntryType: String?,
    @SerializedName("TimeIn")
    val timeIn: String?,
    @SerializedName("TimeInString")
    val timeInString: String?,
    @SerializedName("TimeOut")
    val timeOut: String?,
    @SerializedName("TimeOutString")
    val timeOutString: String?
)