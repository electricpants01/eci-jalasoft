package eci.technician.helpers

import android.content.Context
import android.util.Log
import eci.technician.R
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.models.order.ServiceOrder
import java.util.*

object FilterHelper {
    private val TAG = "FilterHelper"
    private val EXCEPTION = "Exception"

    enum class DateFilter {
        TODAY, YESTERDAY, LAST_SEVEN_DAYS, LAST_THIRTY_DAYS, NOT_SELECTED
    }

    enum class ServiceCallStatus {
        NOT_SELECTED, PENDING, DISPATCHED, ON_HOLD, SCHEDULED;
    }

    fun getStringTitleForDateFilter(dateFilter: DateFilter, context: Context): String {
        when (dateFilter) {
            DateFilter.YESTERDAY -> {
                return context.getString(R.string.filter_yesterday)
            }
            DateFilter.TODAY -> {
                return context.getString(R.string.filter_today)
            }
            DateFilter.LAST_SEVEN_DAYS -> {
                return context.getString(R.string.filter_last_seven_days)
            }
            DateFilter.LAST_THIRTY_DAYS -> {
                return context.getString(R.string.filter_last_thirty_days)
            }
            else -> {
                return context.getString(R.string.filter_no_selected)
            }
        }
    }

    fun getStringForStatusFilter(statusFilter: ServiceCallStatus, context: Context): String {
        when (statusFilter) {
            ServiceCallStatus.PENDING -> {
                return context.getString(R.string.callStatusPending)
            }
            ServiceCallStatus.DISPATCHED -> {
                return context.getString(R.string.callStatusDispatched)
            }
            ServiceCallStatus.ON_HOLD -> {
                return context.getString(R.string.callStatusOnHold)
            }
            ServiceCallStatus.SCHEDULED -> {
                return context.getString(R.string.callStatusScheduled)
            }
            else -> {
                return context.getString(R.string.filter_no_selected)
            }
        }
    }

    fun convertIntToFilterDate(value: Int):DateFilter {
        return when(value){
            1 -> DateFilter.TODAY
            2 -> DateFilter.YESTERDAY
            3 -> DateFilter.LAST_SEVEN_DAYS
            4 -> DateFilter.LAST_THIRTY_DAYS
            else -> DateFilter.NOT_SELECTED
        }
    }

    fun filterServiceOrdersByDate(
        filterDate: DateFilter,
        serviceOrderList: MutableList<ServiceOrder>
    ): MutableList<ServiceOrder> {
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]
        var dateFrom: Date?
        var dateTo: Date?
        val localList: MutableList<ServiceOrder> = mutableListOf()

        when (filterDate) {
            DateFilter.YESTERDAY -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                c1.add(Calendar.DAY_OF_MONTH, -1)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.TODAY -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 23, 59, 59)
                c2.add(Calendar.DAY_OF_MONTH, 0)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.LAST_SEVEN_DAYS -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                c1.add(Calendar.DAY_OF_MONTH, -6)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 23, 59, 59)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.LAST_THIRTY_DAYS -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                c1.add(Calendar.DAY_OF_MONTH, -29)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 23, 59, 59)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.NOT_SELECTED -> {
                return serviceOrderList
            }
        }
    }

    fun filterGroupServiceOrdersByDate(
        filterDate: DateFilter,
        serviceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]
        var dateFrom: Date?
        var dateTo: Date?
        val localList: MutableList<GroupCallServiceOrder> = mutableListOf()

        when (filterDate) {
            DateFilter.YESTERDAY -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                c1.add(Calendar.DAY_OF_MONTH, -1)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.TODAY -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 23, 59, 59)
                c2.add(Calendar.DAY_OF_MONTH, 0)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.LAST_SEVEN_DAYS -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                c1.add(Calendar.DAY_OF_MONTH, -6)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 23, 59, 59)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.LAST_THIRTY_DAYS -> {
                val c1: Calendar = GregorianCalendar(year, month, day, 0, 0, 0)
                c1.add(Calendar.DAY_OF_MONTH, -29)
                dateFrom = c1.time
                val c2: Calendar = GregorianCalendar(year, month, day, 23, 59, 59)
                dateTo = c2.time
                val range = dateFrom..dateTo
                for (ser in serviceOrderList) {
                    if (ser.callDate in range) {
                        localList.add(ser)
                    }
                }
                return localList
            }
            DateFilter.NOT_SELECTED -> {
                return serviceOrderList
            }
        }
    }

    /**
     * SortType = 1 -> Created date
     * SortType = 2 -> schedule date
     */
    fun sortServiceOrdersByDate(
        serviceOrderList: MutableList<ServiceOrder>,
        sortType: Int
    ): MutableList<ServiceOrder> {
        if (serviceOrderList.isEmpty()) return serviceOrderList
        when (sortType) {
            1 -> serviceOrderList.sortByDescending { it.callDate }
            2 -> serviceOrderList.sortBy { it.estStartDate }
            else -> {
                // do nothing
            }
        }
        return serviceOrderList
    }

    /**
     * SortType = 1 -> Created date
     * SortType = 2 -> schedule date
     */
    fun sortGroupServiceOrdersByDate(
        serviceOrderList: MutableList<GroupCallServiceOrder>,
        sortType: Int
    ): MutableList<GroupCallServiceOrder> {
        if (serviceOrderList.isEmpty()) return serviceOrderList
        when (sortType) {
            1 -> serviceOrderList.sortByDescending { it.callDate }
            2 -> serviceOrderList.sortBy { it.estStartDate }
            else -> {
                // do nothing
            }
        }
        return serviceOrderList
    }



    fun filterGroupServiceOrdersByCallType(
        filterCallType: String,
        serviceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        if (filterCallType.isEmpty()) {
            return serviceOrderList
        }
        return serviceOrderList.filter { it.callType == filterCallType }.toMutableList()
    }

    fun filterServiceOrdersByCallType(
        filterCallType: String,
        serviceOrderList: MutableList<ServiceOrder>
    ): MutableList<ServiceOrder> {
        if (filterCallType.isEmpty()) {
            return serviceOrderList
        }
        return serviceOrderList.filter { it.callType == filterCallType }.toMutableList()
    }

    fun filterGroupServiceOrderByPriority(
        filterCallPriority: String,
        serviceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        if (filterCallPriority.isEmpty()) {
            return serviceOrderList
        }
        return serviceOrderList.filter { it.callPriority == filterCallPriority }.toMutableList()
    }

    fun filterServiceOrdersByPriority(
        filterCallPriority: String,
        serviceOrderList: MutableList<ServiceOrder>
    ): MutableList<ServiceOrder> {
        if (filterCallPriority.isEmpty()) {
            return serviceOrderList
        }
        return serviceOrderList.filter { it.callPriority == filterCallPriority }.toMutableList()
    }

    fun filterGroupServiceOrderByTechnician(
        filterCallTechnicianFilter: Int,
        serviceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        if (filterCallTechnicianFilter == -1) {
            return serviceOrderList
        }
        return serviceOrderList.filter { it.technicianNumberId == filterCallTechnicianFilter }
            .toMutableList()
    }

    fun filterGroupServiceOrderByStatus(
        status: Int,
        serviceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        if (status == -1) return serviceOrderList
        return serviceOrderList.filter {
            it.statusCode_Code?.trim { it <= ' ' } == GroupCallServiceOrder.getStatusByIndex(
                status
            )
        }.toMutableList()
    }

    fun convertIntToServiceCallStatus(value: Int):ServiceCallStatus {
        return when(value){
            1 -> ServiceCallStatus.PENDING
            2 -> ServiceCallStatus.DISPATCHED
            3 -> ServiceCallStatus.ON_HOLD
            4 -> ServiceCallStatus.SCHEDULED
            else -> ServiceCallStatus.NOT_SELECTED
        }
    }

    fun filterServiceOrderByStatus(
        filterStatus: ServiceCallStatus,
        serviceOrderList: MutableList<ServiceOrder>
    ): MutableList<ServiceOrder> {
        return if (filterStatus == ServiceCallStatus.NOT_SELECTED) {
            serviceOrderList
        } else {
            val filtered = serviceOrderList.filter {
                it.getTechnicianStatusForFilter() == filterStatus
            }
            filtered.toMutableList()
        }
    }

    fun filterGroupServiceOrderByStatus(
        filterStatus: ServiceCallStatus,
        serviceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        return if (filterStatus == ServiceCallStatus.NOT_SELECTED) {
            serviceOrderList
        } else {
            val filtered = serviceOrderList.filter {
                it.getTechnicianStatusForFilter() == filterStatus
            }
            filtered.toMutableList()
        }
    }

    fun filterServiceOrderByQueryText(
        query: String,
        serviceOrderList: MutableList<ServiceOrder>
    ): MutableList<ServiceOrder> {
        var filteredServiceOrders = mutableListOf<ServiceOrder>()
        filteredServiceOrders = serviceOrderList.filter {
            it.address?.contains(query, true) ?: false ||
                    it.callNumber_Code?.contains(query, true) ?: false ||
                    it.callType?.contains(query, true) ?: false ||
                    it.city?.contains(query, true) ?: false ||
                    it.customerName?.contains(query, true) ?: false ||
                    it.customerNumber?.contains(query, true) ?: false ||
                    it.description?.contains(query, true) ?: false ||
                    it.equipmentNumber?.contains(query, true) ?: false ||
                    it.equipmentLocation?.contains(query, true) ?: false ||
                    it.equipmentRemarks?.contains(query, true) ?: false ||
                    it.ipAddress?.contains(query, true) ?: false ||
                    it.macAddress?.contains(query, true) ?: false ||
                    it.make?.contains(query, true) ?: false ||
                    it.model?.contains(query, true) ?: false ||
                    it.serialNumber?.contains(query, true) ?: false ||
                    it.serviceType?.contains(query, true) ?: false ||
                    it.state?.contains(query, true) ?: false ||
                    it.technicianName?.contains(query, true) ?: false ||
                    it.technicianNumber?.contains(query, true) ?: false ||
                    it.zip?.contains(query, true) ?: false ||
                    it.contactName?.contains(query, true) ?: false ||
                    it.contactPhone?.contains(query, true) ?: false ||
                    it.contactEmail?.contains(query, true) ?: false ||
                    it.billCode?.contains(query, true) ?: false ||
                    it.terms?.contains(query, true) ?: false ||
                    it.callPriority?.contains(query, true) ?: false ||
                    it.equipmentPrimaryNote?.contains(query, true) ?: false ||
                    it.workOrderNumber?.contains(query, true) ?: false ||
                    it.customerPhoneNumber?.contains(query, true) ?: false ||
                    it.onHoldCode?.contains(query, true) ?: false ||
                    it.onHoldDescription?.contains(query, true) ?: false
        }.toMutableList()
        return filteredServiceOrders
    }

    fun filterGroupServiceOrderByQueryText(
        query: String,
        groupCallServiceOrderList: MutableList<GroupCallServiceOrder>
    ): MutableList<GroupCallServiceOrder> {
        var filteredServiceOrders = mutableListOf<GroupCallServiceOrder>()
        filteredServiceOrders = groupCallServiceOrderList.filter {
            it.technicianNumber.contains(query, true) ?: false ||
                    it.technicianName?.contains(query, true) ?: false ||
                    it.statusCode.contains(query, true) ||
                    it.customerName?.contains(query, true) ?: false ||
                    it.equipmentNumberFilter(query) ||
                    it.callNumber_Code.contains(query, true) ||
                    it.callType?.contains(query, true) ?: false
        }.toMutableList()
        return filteredServiceOrders

    }

    fun getStatusFilterSavedForServiceOrderList(): ServiceCallStatus {
        return try {
            val valueFromPreferences = AppAuth.getInstance().statusCallFilterForServiceOrderList
            ServiceCallStatus.valueOf(valueFromPreferences)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return ServiceCallStatus.NOT_SELECTED
        }
    }

    fun getDateFilterSavedForServiceOrderList(): DateFilter {
        return try {
            val valueFromPreferences = AppAuth.getInstance().dateFilterForServiceOrderList
            DateFilter.valueOf(valueFromPreferences)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            DateFilter.NOT_SELECTED
        }

    }

    fun getDateFilterSavedForGroupServiceOrderList(): DateFilter {
        return try {
            val valueFromPreferences = AppAuth.getInstance().dateFilterForGroupCalls
            DateFilter.valueOf(valueFromPreferences)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            DateFilter.NOT_SELECTED
        }

    }

    fun filterByQuantity(serviceOrders: MutableList<ServiceOrder>): MutableList<ServiceOrder> {
        serviceOrders.sortBy { it.index }
        var numberOfOrders = AppAuth.getInstance().technicianUser.restrictCallOrderValue
        val assistantDispatch =
            serviceOrders.filter { it.isAssist() && it.shouldAssistantBeOnTop() }
        val assistantOrders =
            serviceOrders.filter { it.isAssist() && !it.shouldAssistantBeOnTop() }.toMutableList()
        val primaryOrders = serviceOrders.filter { !it.isAssist() }.toMutableList()
        if (assistantDispatch.isNotEmpty()) {
            primaryOrders.add(0, assistantDispatch[0])
            numberOfOrders += 1
        }
        val primaryCut = if (numberOfOrders < 1) {
            primaryOrders
        } else {
            primaryOrders.take(numberOfOrders)
        }
        return (primaryCut + assistantOrders).toMutableList()
    }

    fun getStringForTechniciansSort(value: Int, requireContext: Context): String {
        return when (value) {
            1 -> requireContext.getString(R.string.date_received)
            2 -> requireContext.getString(R.string.schedule_time)
            else -> ""
        }
    }

    fun getStringForTechnicianDateFilter(value: Int, requireContext: Context): String {
        return when (value) {
            1 -> requireContext.getString(R.string.filter_today)
            2 -> requireContext.getString(R.string.filter_yesterday)
            3 -> requireContext.getString(R.string.filter_last_seven_days)
            4 -> requireContext.getString(R.string.filter_last_thirty_days)
            else -> ""
        }
    }

    fun getStringFonTechnicianStatusFilter(value: Int, requireContext: Context): String {
        return when (value) {
            1 -> requireContext.getString(R.string.filter_pending)
            2 -> requireContext.getString(R.string.filter_dispatch)
            3 -> requireContext.getString(R.string.filter_on_hold)
            4 -> requireContext.getString(R.string.filter_scheduled)
            else -> ""
        }
    }

}