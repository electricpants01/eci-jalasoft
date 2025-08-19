package eci.technician.models.order

import android.util.Log
import com.google.gson.annotations.SerializedName
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.DateTimeHelper.formatDate
import eci.technician.helpers.DateTimeHelper.formatDateYear
import eci.technician.helpers.DateTimeHelper.formatFullDateYear
import eci.technician.helpers.DateTimeHelper.formatTimeDate
import eci.technician.helpers.DateTimeHelper.getDateFromStringWithoutTimeZone
import eci.technician.helpers.DateTimeHelper.getPMDateFromString
import eci.technician.helpers.FilterHelper.ServiceCallStatus
import eci.technician.models.data.UsedPart
import eci.technician.models.sort.ServiceOrderSort
import eci.technician.repository.ServiceOrderRepository.findIndex
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.*
import io.realm.Sort





open class ServiceOrder : RealmObject {
    @PrimaryKey
    @SerializedName("CallNumber_ID")
    var callNumber_ID: Int = 0

    @SerializedName("Id")
    var id: String? = null

    @SerializedName("Address")
    var address: String? = null

    @SerializedName("CallDate")
    var callDate: Date? = null

    @SerializedName("CallNumber_Code")
    var callNumber_Code: String? = null

    @SerializedName("CallType")
    var callType: String? = null

    @SerializedName("Caller")
    var caller: String? = null

    @SerializedName("ChatIdent")
    var chatIdent: String? = null

    @SerializedName("City")
    var city: String? = null

    @SerializedName("CustomerName")
    var customerName: String? = null

    @SerializedName("CustomerNumber")
    var customerNumber: String? = null

    @SerializedName("CustomerWarehouseId")
    var customerWarehouseId:Int = 0

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("Duetime")
    var dueDate: Date? = null

    @SerializedName("EquipmentNumber")
    var equipmentNumber: String? = null

    @SerializedName("EquipmentId")
    var equipmentId: Int = 0

    @SerializedName("Equipmentlocation")
    var equipmentLocation: String? = null

    @SerializedName("Equipmentremarks")
    var equipmentRemarks: String? = null

    @SerializedName("IPAddress")
    var ipAddress: String? = null

    @SerializedName("IsSupplyOrder")
    var isSupplyOrder: Boolean = false

    @SerializedName("MACAddress")
    var macAddress: String? = null

    @SerializedName("Make")
    var make: String? = null

    @SerializedName("Model")
    var model: String? = null

    @SerializedName("SerialNumber")
    var serialNumber: String? = null

    @SerializedName("ServiceType")
    var serviceType: String? = null

    @SerializedName("State")
    var state: String? = null

    @SerializedName("StatusCode")
    var statusCode: String? = null

    @SerializedName("StatusCode_Code")
    var statusCode_Code: String? = null
    var technicianStatusCode: String? = null

    @SerializedName("TechnicianId")
    var technicianId: String? = null

    @SerializedName("TechnicianName")
    var technicianName: String? = null

    @SerializedName("TechnicianNumber")
    var technicianNumber: String? = null

    @SerializedName("TechnicianNumberId")
    var technicianNumberId: Int = 0

    @SerializedName("TechnicianRate")
    var technicianRate: Int? = null

    @SerializedName("TechnicianRateComment")
    var technicianRateComment: String? = null

    @SerializedName("Zip")
    var zip: String? = null

    @SerializedName("ContactName")
    var contactName: String? = null

    @SerializedName("ContactPhone")
    var contactPhone: String? = null

    @SerializedName("ContactEmail")
    var contactEmail: String? = null

    @SerializedName("BillCode")
    var billCode: String? = null

    @SerializedName("Terms")
    var terms: String? = null

    @SerializedName("CallPriority")
    var callPriority: String? = null

    @SerializedName("EquipmentInstallDate")
    var equipmentInstallDate: String? = null

    @SerializedName("EquipmentPrimaryNote")
    var equipmentPrimaryNote: String? = null

    @SerializedName("WorkOrderNumber")
    var workOrderNumber: String? = null

    @SerializedName("CustomerPhoneNumber")
    var customerPhoneNumber: String? = null

    @SerializedName("ArriveTime")
    var arriveTime: Date? = null

    @SerializedName("DepartTime")
    var departTime: Date? = null

    @SerializedName("DispatchTime")
    var dispatchTime: Date? = null

    @SerializedName("MasterCallId")
    var masterCallId: Int = 0

    @SerializedName("OnHoldCode")
    var onHoldCode: String? = null

    @SerializedName("OnHoldDescription")
    var onHoldDescription: String? = null

    @SerializedName("RescheduledCallId")
    var rescheduledCallId: Int = 0

    @SerializedName("PoNumber")
    var poNumber: String? = null

    @SerializedName("DefaultActivityCodeId")
    var defaultActivityCodeId: String? = null

    @SerializedName("ContractNumber")
    var contractNumber: String? = null

    @SerializedName("EstStartDate")
    var estStartDate: Date? = null

    @SerializedName("PMDueDate")
    var pmDueDate: String? = null

    @SerializedName("PMDueDateString")
    var pmDueDateString: String? = null

    @SerializedName("PMDueDisplay")
    private var pmDueDisplay: String? = null

    @SerializedName("PMLastDate")
    var pmLastDate: String? = null

    @SerializedName("PMLastDateString")
    var pmLastDateString: String? = null

    @SerializedName("PMLastDisplay")
    var pmLastDisplay: String? = null

    @SerializedName("PMUseDate")
    var isPmUseDate: Boolean = false

    @SerializedName("PMUseMeter")
    var isPmUseMeter: Boolean = false

    @Ignore
    @SerializedName("Parts")
    var parts: List<UsedPart> = ArrayList()

    @Ignore
    @SerializedName("Labors")
    var labors: List<ServiceCallLabor> = ArrayList()

    @SerializedName("SignatureDateTimeString")
    var signatureDateTimeString: Date? = null

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null

    @SerializedName("PreventativeMaintenance")
    var isPreventativeMaintenance: Boolean = false

    @SerializedName("WarrantyDate")
    var warrantyDate: String? = null

    var index: Int = -1
    var showDetails: Boolean = false
    var statusOrder: Int = 0
    var completedCall: Boolean = false

    constructor() {}

    fun canReassignChecker(): Boolean {
        try {
            val technicianUser = AppAuth.getInstance().technicianUser
            if (technicianUser != null) {
                val res = false
                if (technicianUser.technicianCode == technicianNumber) return false
                if (!technicianUser.isAllowReassignment) return false
                if (statusCode_Code?.trim { it <= ' ' } == "P" || statusCode_Code?.trim { it <= ' ' } == "S") {
                    return true
                }
            } else {
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e.fillInStackTrace())
        }
        return false
    }


    fun getTechnicianDispatchTime(): Date? {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labors = realm
                    .where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                    .equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    )
                    .sort(ServiceCallLabor.DISPATCH_TIME)
                    .findAll()
                if (!labors.isEmpty()) {
                    val item = labors.first()
                    item?.dispatchTime
                } else {
                    null
                }
            } else {
                dispatchTime
            }
        } catch (e: java.lang.Exception) {
            null
        } finally {
            realm.close()
        }
    }

    fun getTechnicianArriveTime(): Date? {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labors = realm
                    .where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                    .equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    )
                    .sort(ServiceCallLabor.ARRIVE_TIME)
                    .findAll()
                if (!labors.isEmpty()) {
                    val item = labors.first()
                    item?.arriveTime
                } else {
                    null
                }
            } else {
                arriveTime
            }
        } catch (e: java.lang.Exception) {
            null
        } finally {
            realm.close()
        }
    }


    constructor(id: String?) {
        this.id = id
    }


    fun isAssist(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (!isValid) {
                false
            } else AppAuth.getInstance().technicianUser.technicianCode != technicianNumber &&
                    realm.where(ServiceCallLabor::class.java)
                        .equalTo("callId", callNumber_ID)
                        .equalTo(
                            "technicianId",
                            AppAuth.getInstance().technicianUser.technicianNumber
                        )
                        .count() > 0
        } catch (e: java.lang.Exception) {
            false
        } finally {
            realm.close()
        }
    }


    fun getHeader(): String {
        return try {
            java.lang.String.format(
                "%s (%s) %s%s",
                formatTimeDate(callDate ?: Date()),
                getTechnicianStatus(),
                if (isOnHold()) " - " + (onHoldCode ?: "") else "",
                if (isAssist()) " - Assist" else ""
            )
        } catch (e: Exception) {
            ""
        }
    }

    fun getTechnicianStatus(): String {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                if (isOnHold()) {
                    statusCode ?: ""
                } else {
                    val labors = realm
                        .where(ServiceCallLabor::class.java)
                        .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                        .equalTo(
                            ServiceCallLabor.TECHNICIAN_ID,
                            AppAuth.getInstance().technicianUser.technicianNumber
                        )
                        .sort(ServiceCallLabor.DISPATCH_TIME)
                        .findAll()
                    if (!labors.isEmpty()) {
                        val item = labors.first()
                        if (item != null) {
                            if (item.departureTime != null) AppAuth.getInstance().context.getString(
                                R.string.callStatusCompleted
                            ) else if (item.arriveTime != null) AppAuth.getInstance().context.getString(
                                R.string.callStatusArrived
                            ) else if (item.dispatchTime != null) AppAuth.getInstance().context.getString(
                                R.string.callStatusDispatched
                            ) else AppAuth.getInstance().context.getString(R.string.callStatusPending)
                        } else {
                            AppAuth.getInstance().context.getString(R.string.callStatusPending)
                        }
                    } else {
                        AppAuth.getInstance().context.getString(R.string.callStatusPending)
                    }
                }
            } else {
                statusCode ?: ""
            }
        } catch (e: java.lang.Exception) {
            ""
        } finally {
            realm.close()
        }
    }


    fun shouldAssistantBeOnTop(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                if (isOnHold()) {
                    false
                } else {
                    val fieldNames = arrayOf(ServiceCallLabor.DISPATCH_TIME, ServiceCallLabor.ARRIVE_TIME)
                    val sort = arrayOf(Sort.ASCENDING, Sort.ASCENDING)
                    val labors = realm
                        .where(ServiceCallLabor::class.java)
                        .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                        .equalTo(
                            ServiceCallLabor.TECHNICIAN_ID,
                            AppAuth.getInstance().technicianUser.technicianNumber
                        )
                        .sort(fieldNames,sort)
                        .findAll()
                    if (!labors.isEmpty()) {
                        val item = labors.first()
                        if (item != null) {
                            if (item.departureTime != null) false else if (item.arriveTime != null) true else if (item.dispatchTime != null) true else false
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

    open fun getTechnicianStatusForFilter(): ServiceCallStatus? {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                if (isOnHold()) {
                    ServiceCallStatus.ON_HOLD
                } else {
                    val labors = realm
                        .where(ServiceCallLabor::class.java)
                        .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                        .equalTo(
                            ServiceCallLabor.TECHNICIAN_ID,
                            AppAuth.getInstance().technicianUser.technicianNumber
                        )
                        .sort(ServiceCallLabor.DISPATCH_TIME)
                        .findAll()
                    if (!labors.isEmpty()) {
                        val item = labors.first()
                        if (item != null) {
                            if (item.departureTime != null) ServiceCallStatus.DISPATCHED else if (item.arriveTime != null) ServiceCallStatus.DISPATCHED else if (item.dispatchTime != null) ServiceCallStatus.DISPATCHED else ServiceCallStatus.PENDING
                        } else {
                            ServiceCallStatus.PENDING
                        }
                    } else {
                        ServiceCallStatus.PENDING
                    }
                }
            } else {
                if (statusCode_Code?.trim { it <= ' ' } == "D") {
                    ServiceCallStatus.DISPATCHED
                } else if (statusCode_Code?.trim { it <= ' ' } == "P") {
                    ServiceCallStatus.PENDING
                } else if (statusCode_Code?.trim { it <= ' ' } == "S") {
                    ServiceCallStatus.SCHEDULED
                } else if (statusCode_Code?.trim { it <= ' ' } == "H") {
                    ServiceCallStatus.ON_HOLD
                } else {
                    ServiceCallStatus.PENDING
                }
            }
        } catch (e: Exception) {
            ServiceCallStatus.PENDING
        } finally {
            realm.close()
        }
    }

    open fun getStatusOrderForSorting(): Int {
        val realm = Realm.getDefaultInstance()
        return try {
            var orderByStatus = 1
            orderByStatus = if (isAssist()) {
                val labors = realm
                    .where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                    .equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    )
                    .sort(ServiceCallLabor.DISPATCH_TIME)
                    .findAll()
                if (!labors.isEmpty()) {
                    val item = labors.first()
                    if (item != null) {
                        if (item.departureTime != null) 2 else if (item.arriveTime != null) 0 else if (item.dispatchTime != null) 0 else 2
                    } else {
                        2
                    }
                } else {
                    2
                }
            } else {
                if (statusCode_Code?.trim { it <= ' ' } == "D") {
                    0
                } else {
                    1
                }
            }
            if (AppAuth.getInstance().technicianUser.isRestrictCallOrder && orderByStatus != 0) {
                if (index == ServiceOrderSort.HAS_OFFLINE_CHANGES) {
                    index = findIndex(callNumber_Code ?: "").index ?: 0
                }
                orderByStatus = index
            }
            orderByStatus
        } catch (e: java.lang.Exception) {
            1
        } finally {
            realm.close()
        }
    }

    fun setTechStatusByCode() {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction { realm ->
                technicianStatusCode = if (isAssist()) {
                    val labors = realm
                        .where(ServiceCallLabor::class.java)
                        .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                        .equalTo(
                            ServiceCallLabor.TECHNICIAN_ID,
                            AppAuth.getInstance().technicianUser.technicianNumber
                        )
                        .sort(ServiceCallLabor.DISPATCH_TIME)
                        .findAll()
                    if (!labors.isEmpty()) {
                        val item = labors.first()
                        if (item != null) {
                            if (item.departureTime != null) AppAuth.getInstance().context.getString(
                                R.string.callStatusCompleted
                            ) else if (item.arriveTime != null) AppAuth.getInstance().context.getString(
                                R.string.callStatusArrived
                            ) else if (item.dispatchTime != null) AppAuth.getInstance().context.getString(
                                R.string.callStatusDispatched
                            ) else AppAuth.getInstance().context.getString(R.string.callStatusPending)
                        } else {
                            AppAuth.getInstance().context.getString(R.string.callStatusPending)
                        }
                    } else {
                        AppAuth.getInstance().context.getString(R.string.callStatusPending)
                    }
                } else {
                    statusCode
                }
            }
        } catch (e: Exception) {
        } finally {
            realm.close()
        }
    }

    open fun getFormattedWarrantyDate():String{
        return try {
            if(warrantyDate == null)
                "N/A"
            warrantyDate?.let {
                val stringDate = getDateFromStringWithoutTimeZone(it)
                stringDate?.let { it1 -> formatFullDateYear(it1) }
            } ?: kotlin.run {
                "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    open fun getFormattedDueDate(): String {
        return try {
            dueDate?.let {
                formatDate(it)
            } ?: kotlin.run {
                "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun getFormattedArriveTime(): String {
        return try {
            val techArriveTime = getTechnicianArriveTime()
            if (techArriveTime == null || getTechnicianStatus() == AppAuth.getInstance().context.getString(
                    R.string.callStatusPending
                ) || statusCode == "Scheduled" || statusCode == "On Hold"
            ) {
                "N/A"
            } else {
                formatTimeDate(techArriveTime)
            }
        } catch (e: java.lang.Exception) {
            "N/A"
        }
    }

    open fun getFormattedScheduleTime(): String {
        return try {
            estStartDate?.let {
                formatTimeDate(it)
            } ?: kotlin.run {
                "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun getFormattedDispatchTime(): String {
        return try {
            val techDispatchTime = getTechnicianDispatchTime()
            if (techDispatchTime == null || getTechnicianStatus() == AppAuth.getInstance().context.getString(
                    R.string.callStatusPending
                ) || statusCode == "Scheduled" || statusCode == "On Hold"
            ) {
                "N/A"
            } else {
                formatTimeDate(techDispatchTime)
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    open fun getFormattedDepartTime(): String? {
        return try {
            departTime?.let {
                formatTimeDate(it)
            }
            if (departTime == null) {
                "N/A"
            } else {
                formatTimeDate(departTime!!)
            }
        } catch (e: java.lang.Exception) {
            "N/A"
        }
    }

    open fun getFormattedPMDueDate(): String {
        try {
            val pmDueDateStr = pmDueDateString ?: return "N/A"
            val convertedDate = getPMDateFromString(pmDueDateStr) ?: return "N/A"
            return formatDateYear(convertedDate)
        } catch (e: Exception) {
            return "N/A"
        }

    }

    open fun getFormattedPMLastDate(): String {
        try {
            val pmLastDateStr = pmLastDateString ?: return "N/A"
            val convertedDate = getPMDateFromString(pmLastDateStr) ?: return "N/A"
            return formatDateYear(convertedDate)
        } catch (e: Exception) {
            return "N/A"
        }

    }

    open fun getFormattedLastDisplay(): String {
        try {
            val pmLastDisplay1 = pmLastDisplay ?: return "N/A"
            return pmLastDisplay1.toDouble().toInt().toString()
        } catch (e: Exception) {
            return "N/A"
        }
    }

    open fun getFormattedDueDisplay(): String {
        try {
            val pmDueDisplay1 = pmDueDisplay ?: return "N/A"
            return pmDueDisplay1.toDouble().toInt().toString()
        } catch (e: Exception) {
            return "N/A"
        }
    }

    open fun getFormattedCurrentTime(): String? {
        return formatTimeDate(Date())
    }

    open fun getFormattedEquipmentInstallDate(): String? {
        try {
            val equipmentInstallDate1 = equipmentInstallDate ?: return "N/A"
            val dateFromString =
                getDateFromStringWithoutTimeZone(equipmentInstallDate1) ?: return "N/A"
            return formatDateYear(dateFromString)
        } catch (e: Exception) {
            return "N/A"
        }

    }

    open fun getFullAddress(): String {
        return String.format("%s, %s", address ?: "", city ?: "")
    }

    open fun getCustomerFullAddress(): String? {
        return String.format("%s, %s, %s, %s", address ?: "", city ?: "", state ?: "", zip ?: "")
    }

    open fun getMakeModel(): String? {
        if (make.isNullOrEmpty() || model.isNullOrEmpty()) {
            return null
        } else {
            return String.format("%s/%s", make, model)
        }
    }

    fun canRelease(): Boolean {
        return statusCode_Code?.trim { it <= ' ' } == "H" && !isAssist()
    }

    fun canReAssign(): Boolean {
        return statusCode_Code?.trim { it <= ' ' } == "P" && !isAssist()
    }

    fun canArrive(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labor = realm.where(ServiceCallLabor::class.java)
                    .equalTo("callId", callNumber_ID)
                    .equalTo("technicianId", AppAuth.getInstance().technicianUser.technicianNumber)
                    .findFirst()
                if (labor != null) {
                    labor.arriveTime == null && labor.dispatchTime != null
                } else {
                    false
                }
            } else {
                val code = statusCode_Code?.trim { it <= ' ' }
                code == "D" && arriveTime == null
            }
        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

    fun canCancel(): Boolean {
        return try {
            val code = statusCode_Code?.trim { it <= ' ' }
            AppAuth.getInstance().technicianUser.isAllowCancelCall && (code == "P" || code == "S" || code == "D" || code == "H") && !isAssist()
        } catch (e: Exception) {
            false
        }
    }

    open fun isPending(): Boolean {
        return statusCode_Code?.trim { it <= ' ' } == "P"
    }

    open fun isOnHold(): Boolean {
        return statusCode_Code?.trim { it <= ' ' } == "H"
    }

    fun canOnHold(): Boolean {
        return try {
            val code = statusCode_Code?.trim { it <= ' ' }
            (code == "P" || code == "S") && AppAuth.getInstance().technicianUser.isAllowOnHold && !isAssist()
        } catch (e: Exception) {
            false
        }
    }

    fun canDispatch(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labor = realm.where(ServiceCallLabor::class.java)
                    .equalTo("callId", callNumber_ID)
                    .equalTo("technicianId", AppAuth.getInstance().technicianUser.technicianNumber)
                    .findFirst()
                if (labor != null) {
                    labor.dispatchTime == null && !isOnHold()
                } else {
                    false
                }
            } else {
                val code = statusCode_Code?.trim { it <= ' ' }
                code == "P" || code == "S"
            }
        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

    fun canSchedule(): Boolean {
        return try {
            val code = statusCode_Code?.trim { it <= ' ' }
            code == "P" && AppAuth.getInstance().technicianUser.isAllowScheduleCall && !isAssist()
        } catch (e: Exception) {
            false
        }
    }

    fun canUnDispatch(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labor = realm.where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                    .equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    )
                    .findFirst()
                if (labor != null && labor.isValid) {
                    labor.dispatchTime != null && labor.arriveTime == null && labor.departureTime == null ||
                            labor.arriveTime != null && AppAuth.getInstance().technicianUser.isAllowUndispatchCallAfterArrive && labor.departureTime == null
                } else {
                    false
                }
            } else {
                try {
                    statusCode_Code?.trim { it <= ' ' } == "D" && canArrive() || statusCode_Code?.trim { it <= ' ' } == "D" && AppAuth.getInstance().technicianUser.isAllowUndispatchCallAfterArrive
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

    fun canIncomplete(): Boolean {
        return try {
            !isAssist() && canComplete()
        } catch (e: Exception) {
            false
        }
    }

    fun canShowIncompleteButton(): Boolean {
        return try {
            !isAssist() && canShowCompleteButton()
        } catch (e: Exception) {
            false
        }
    }

    fun canShowCompleteButton(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labor = realm.where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                    .equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    )
                    .findFirst()
                if (labor != null) {
                    labor.departureTime == null && labor.arriveTime != null
                } else {
                    false
                }
            } else {
                val code = statusCode_Code?.trim { it <= ' ' }
                code == "D" && arriveTime != null
            }
        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

    fun canComplete(): Boolean {
        val realm = Realm.getDefaultInstance()
        return try {
            if (isAssist()) {
                val labor = realm.where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, callNumber_ID)
                    .equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    )
                    .findFirst()
                if (labor != null) {
                    labor.departureTime == null && labor.arriveTime != null
                } else {
                    false
                }
            } else {
                val code = statusCode_Code?.trim { it <= ' ' }
                code == "D" && arriveTime != null && realm.where(
                    ServiceCallLabor::class.java
                )
                    .equalTo("callId", callNumber_ID)
                    .isNull("departureTime").count() == 0L
            }
        } catch (e: Exception) {
            false
        } finally {
            realm.close()
        }
    }

    fun showNeededParts(): Boolean {
        return try {
            isAssist() && canComplete()
        } catch (e: Exception) {
            false
        }
    }

    open fun isArrived(): Boolean {
        return statusCode_Code?.trim { it <= ' ' } == "D" && arriveTime != null
    }

    fun canAddParts(): Boolean {
        return try {
            if (isAssist()) {
                false
            } else {
                dispatchTime != null && arriveTime != null && statusCode_Code?.trim { it <= ' ' } == "D" || isOnHold()
            }
        } catch (e: Exception) {
            false
        }
    }

    open fun getPrimaryTechName(): String {

        return technicianName ?: ""
    }

    fun canShowPrimaryTechName(): Boolean {
        return try {
            val canShow = false
            isAssist()
        } catch (e: Exception) {
            false
        }
    }

    fun getPmDueDisplay(): String {
        pmDueDisplay?.let {
            if (it.isEmpty()) {
                return "N/A"
            }
            return it
        } ?: kotlin.run {
            return "N/A"
        }
    }

    fun setPmDueDisplay(pmDueDisplay: String?) {
        this.pmDueDisplay = pmDueDisplay
    }

    companion object {
        const val CALL_NUMBER_ID = "callNumber_ID"
        const val CALL_DATE = "callDate"
        const val TECHNICIAN_STATUS_CODE = "technicianStatusCode"
        const val STATUS_ORDER = "statusOrder"
        const val INDEX = "index"
        const val TAG = "ServiceOrder"
        const val EXCEPTION = "Exception"
        const val COMPLETED = "completedCall"
    }
}