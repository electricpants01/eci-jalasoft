package eci.technician.repository

import android.content.Context
import android.util.Log
import eci.technician.R
import eci.technician.helpers.DateTimeHelper
import eci.technician.helpers.DateTimeHelper.minus31Days
import eci.technician.helpers.DateTimeHelper.plusMinutes
import eci.technician.helpers.DateTimeHelper.toBackendFormat
import eci.technician.helpers.DateTimeHelper.toCalendar
import eci.technician.helpers.api.retroapi.*
import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.models.ProcessingResult
import eci.technician.models.time_cards.*
import eci.technician.tools.Settings
import eci.technician.weekview.weekviewp.EventType
import eci.technician.weekview.weekviewp.WeekViewEvent
import io.realm.Realm
import io.realm.kotlin.toFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

object TechnicianTimeRepository {
    const val TAG = "TechnicianTimeRepository"
    const val EXCEPTION = "Exception"

    fun clockInUser(changeStatusModel: ChangeStatusModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            val resource = safeCall {
                api.clockInUser(changeStatusModel)
            }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(
                        Resource.Error(
                            "",
                            pair = Pair(
                                ErrorType.BACKEND_ERROR,
                                "${response.formattedErrors} (Server Error)"
                            )
                        )
                    )
                } else {
                    emit(Resource.Success(response))
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }


    /**
     * On ClockInActions It is needed to do some operations
     *  - Update all AvailablePartsByWarehouse for offline
     *  - Refresh all the active service calls
     *  - Update all the call priorities
     *  - Get Payperiods
     */
    suspend fun fetchClockInUpdateActions(onSuccess: () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            PartsRepository.getAvailablePartsByWarehouseForOffline(forceUpdate = false).collect { }
            fetchShifts().collect { }
            RetrofitRepository.RetrofitRepositoryObject.getInstance().apply {
                getTechnicianActiveServiceCallsFlow().collect { value ->
                    when (value) {
                        is Resource.Success -> {
                            withContext(Dispatchers.Main) {
                                onSuccess.invoke()
                            }
                        }
                        else -> {
                            // do nothing
                        }
                    }
                }
                getAllCallPriorities()
            }
        }
    }


    /**
     * On ClockOutActions It is needed to do some operations
     *  - GetPayPeriods
     */
    suspend fun fetchClockOutActions(onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            fetchShifts().collect { }
        }
    }


    fun clockOutUser(changeStatusModel: ChangeStatusModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            val resource = safeCall {
                api.clockOutUser(changeStatusModel)
            }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(
                        Resource.Error(
                            "",
                            pair = Pair(
                                ErrorType.BACKEND_ERROR,
                                "${response.formattedErrors} (Server Error)"
                            )
                        )
                    )
                } else {
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    fun fetchShifts(): Flow<Resource<ProcessingResult>> {
        val currentDate = Date()
        val map: MutableMap<String, Any> = HashMap()
        map["DateFrom"] = currentDate.minus31Days().toBackendFormat()
        map["DateTo"] = currentDate.toBackendFormat()
        map["DateValue"] =
            "${currentDate.minus31Days().toBackendFormat()}-${currentDate.toBackendFormat()}"

        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            val resource = safeCall { api.getPayPeriodsShiftsByPayPeriod(map) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    val list = createShiftListByResponse(response.result)
                    saveShiftsInDB(list)
                    updateLastShift(list)
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getShiftDetailByShiftId(
        shiftId: String,
        shouldDelete: Boolean
    ): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getShiftDetailsByShiftId(shiftId) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    val list = createShiftDetailListFromResponse(response.result)
                    saveShiftDetailsInDb(list, shouldDelete, shiftId)
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }


    fun createShiftDetailListFromResponse(resultBody: String): MutableList<ShiftDetails> {
        var list = mutableListOf<ShiftDetails>()
        try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<ShiftDetails>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    private fun saveShiftDetailsInDb(
        list: MutableList<ShiftDetails>,
        shouldDelete: Boolean,
        shiftId: String
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                if (shouldDelete) {
                    realm.delete(ShiftDetails::class.java)
                }
                list.forEach { it.shiftId = shiftId }
                realm.insertOrUpdate(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    private fun createShiftListByResponse(resultBody: String): MutableList<Shift> {
        var list = mutableListOf<Shift>()
        try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<Shift>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    private fun updateLastShift(list: MutableList<Shift>) {
        if (list.isNotEmpty()) {
            val firstShift: Shift? = list.first()
            firstShift?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    firstShift.shiftId?.let { getShiftDetailByShiftId(it, true) }
                }
            }
        }
    }

    private fun saveShiftsInDB(list: MutableList<Shift>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getShiftsOffline(): Flow<List<Shift>> {
        val realm = Realm.getDefaultInstance()
        return try {
            realm.where(Shift::class.java).findAll().toFlow()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            flow { }
        } finally {
            realm.close()
        }
    }

    fun getTimeCards(date: String): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getTimeCardsEntry(date) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    val list = createTimeCardListFromResponse(response.result)
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    fun createTimeCardListFromResponse(result: String): List<TimeCardsItemResponse> {
        var list = mutableListOf<TimeCardsItemResponse>()
        try {
            list = mutableListOf(
                *Settings.createGson().fromJson(result, Array<TimeCardsItemResponse>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    fun createWeekEventListFromTimeCards(
        timeCardList: MutableList<TimeCardsItemResponse>,
        context: Context
    ): MutableList<WeekViewEvent> {
        val list = mutableListOf<WeekViewEvent>()
        timeCardList.forEach {
            it.timeCardClocks.forEach { timeCardItem ->
                timeCardItem.clockIn?.let { clockString ->
                    val clockInDate = DateTimeHelper.getDateFromGMTString(clockString) ?: return@let
                    val weekViewEvent = WeekViewEvent(
                        Random().nextLong(),
                        context.getString(R.string.clock_in),
                        clockInDate.toCalendar(),
                        clockInDate.plusMinutes(2).toCalendar(),
                        EventType.CLOCK_IN,
                        context.getColor(R.color.colorClockIn)
                    )
                    list.add(weekViewEvent)
                }
                timeCardItem.clockOut?.let { clockString ->
                    val clockOutDate =
                        DateTimeHelper.getDateFromGMTString(clockString) ?: return@let
                    val weekViewEvent = WeekViewEvent(
                        Random().nextLong(),
                        context.getString(R.string.clock_out),
                        clockOutDate.toCalendar(),
                        clockOutDate.plusMinutes(2).toCalendar(),
                        EventType.CLOCK_OUT,
                        context.getColor(R.color.red)
                    )
                    list.add(weekViewEvent)
                }
                timeCardItem.timeEntries.forEach { timeEntry ->
                    timeEntry.timeIn?.let { timeIn ->
                        DateTimeHelper.getDateFromGMTString(timeIn)?.let { timeInDate ->
                            val timeOutDate =
                                DateTimeHelper.getDateFromGMTString(timeEntry.timeOut ?: "")
                                    ?: Date()
                            val weekViewEvent = WeekViewEvent(
                                Random().nextLong(),
                                getTextForWeekViewEvent(timeEntry, context),
                                timeInDate.toCalendar(),
                                timeOutDate.toCalendar(),
                                when (timeEntry.timeEntryType ?: "") {
                                    "B" -> EventType.BREAK
                                    "L" -> EventType.LUNCH
                                    else -> EventType.NONE
                                },
                                when (timeEntry.timeEntryType ?: "") {
                                    "B" -> context.getColor(R.color.colorBrakeCalendar)
                                    "L" -> context.getColor(R.color.colorLunchCalendar)
                                    else -> context.getColor(R.color.colorClockOut)
                                }
                            )
                            list.add(weekViewEvent)
                        }

                    }
                }
            }
        }
        return list
    }

    private fun getTextForWeekViewEvent(timeEntry: TimeEntry, context: Context): String {
        val res = StringBuilder()
        when (timeEntry.timeEntryType ?: "") {
            "B" -> res.append(context.getString(R.string.break_string))
            "L" -> res.append(context.getString(R.string.lunch))
            else -> res.append("")
        }
        timeEntry.timeIn?.let { timeIn ->
            DateTimeHelper.getDateFromGMTString(timeIn)?.let { timeInDate ->
                val timeInString = DateTimeHelper.formatTime(timeInDate, context)
                res.append(" $timeInString - ")
            }
        }
        timeEntry.timeOut?.let { timeOut ->
            DateTimeHelper.getDateFromGMTString(timeOut)?.let { timeInDate ->
                val timeOutString = DateTimeHelper.formatTime(timeInDate, context)
                res.append(timeOutString)
            }
        } ?: kotlin.run {
            res.append(" ${context.getString(R.string.ongoing)}")
        }
        return res.toString()
    }

}