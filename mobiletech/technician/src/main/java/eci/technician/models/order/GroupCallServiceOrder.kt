package eci.technician.models.order

import android.content.Context
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.DateTimeHelper.formatTimeDate
import eci.technician.helpers.FilterHelper
import eci.technician.helpers.PhoneHelper.formatNumberInternationally
import eci.technician.models.data.UsedPart
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.*

open class GroupCallServiceOrder(
    @PrimaryKey
    @SerializedName("CallNumber_ID")
    var callNumber_ID: Int = 0,

    @SerializedName("Id")
    var id: String? = null,

    @SerializedName("Address")
    var address: String? = null,

    @SerializedName("CallDate")
    var callDate: Date = Date(),

    @SerializedName("CallNumber_Code")
    var callNumber_Code: String = "",

    @SerializedName("CallType")
    var callType: String? = null,

    @SerializedName("Caller")
    var caller: String? = null,

    @SerializedName("ChatIdent")
    var chatIdent: String? = null,

    @SerializedName("City")
    var city: String? = null,

    @SerializedName("CustomerName")
    var customerName: String? = null,

    @SerializedName("CustomerNumber")
    var customerNumber: String? = null,

    @SerializedName("Description")
    var description: String? = null,

    @SerializedName("Duetime")
    var dueDate: Date? = null,

    @SerializedName("EquipmentNumber")
    var equipmentNumber: String? = "",

    @SerializedName("EquipmentId")
    var equipmentId: Int = 0,

    @SerializedName("Equipmentlocation")
    var equipmentLocation: String? = null,

    @SerializedName("Equipmentremarks")
    var equipmentRemarks: String? = null,

    @SerializedName("IPAddress")
    var ipAddress: String? = null,

    @SerializedName("IsSupplyOrder")
    var isSupplyOrder: Boolean = false,

    @SerializedName("MACAddress")
    var macAddress: String? = null,

    @SerializedName("Make")
    var make: String? = null,

    @SerializedName("Model")
    var model: String? = null,

    @SerializedName("SerialNumber")
    var serialNumber: String? = null,

    @SerializedName("ServiceType")
    var serviceType: String? = null,

    @SerializedName("State")
    var state: String? = null,

    @SerializedName("StatusCode")
    var statusCode: String = "",

    @SerializedName("StatusCode_Code")
    var statusCode_Code: String? = null,
    var technicianStatusCode: String? = null,

    @SerializedName("TechnicianId")
    var technicianId: String? = null,

    @SerializedName("TechnicianName")
    var technicianName: String? = null,

    @SerializedName("TechnicianNumber")
    var technicianNumber: String = "",

    @SerializedName("TechnicianNumberId")
    var technicianNumberId: Int = 0,

    @SerializedName("TechnicianRate")
    var technicianRate: Int? = null,

    @SerializedName("TechnicianRateComment")
    var technicianRateComment: String? = null,

    @SerializedName("Zip")
    var zip: String? = null,

    @SerializedName("ContactName")
    var contactName: String? = null,

    @SerializedName("ContactPhone")
    var contactPhone: String? = null,

    @SerializedName("ContactEmail")
    var contactEmail: String? = null,

    @SerializedName("BillCode")
    var billCode: String? = null,

    @SerializedName("Terms")
    var terms: String? = null,

    @SerializedName("CallPriority")
    var callPriority: String? = null,

    @SerializedName("EquipmentInstallDate")
    var equipmentInstallDate: String? = null,

    @SerializedName("EquipmentPrimaryNote")
    var equipmentPrimaryNote: String? = null,

    @SerializedName("WorkOrderNumber")
    var workOrderNumber: String? = null,

    @SerializedName("CustomerPhoneNumber")
    var customerPhoneNumber: String? = null,

    @SerializedName("ArriveTime")
    var arriveTime: Date? = null,

    @SerializedName("DepartTime")
    var departTime: Date? = null,

    @SerializedName("DispatchTime")
    var dispatchTime: Date? = null,

    @SerializedName("MasterCallId")
    var masterCallId: Int = 0,

    @SerializedName("OnHoldCode")
    var onHoldCode: String? = null,

    @SerializedName("OnHoldDescription")
    var onHoldDescription: String? = null,

    @SerializedName("RescheduledCallId")
    var rescheduledCallId: Int = 0,

    @SerializedName("PoNumber")
    var poNumber: String? = null,

    @SerializedName("DefaultActivityCodeId")
    var defaultActivityCodeId: String? = null,

    @SerializedName("ContractNumber")
    var contractNumber: String? = null,

    @SerializedName("EstStartDate")
    var estStartDate: Date? = null,

    @SerializedName("PMDueDate")
    var pmDueDate: String? = null,

    @SerializedName("PMDueDateString")
    var pmDueDateString: String? = null,

    @SerializedName("PMDueDisplay")
    var pmDueDisplay: String? = null,

    @SerializedName("PMLastDate")
    var pmLastDate: String? = null,

    @SerializedName("PMLastDateString")
    var pmLastDateString: String? = null,

    @SerializedName("PMLastDisplay")
    var pmLastDisplay: String? = null,

    @SerializedName("PMUseDate")
    var isPmUseDate: Boolean = false,

    @SerializedName("PMUseMeter")
    var isPmUseMeter: Boolean = false,
    @Ignore
    @SerializedName("Parts")
    var parts: List<UsedPart> = ArrayList(),
    @Ignore
    @SerializedName("Labors")
    var labors: List<ServiceCallLabor> = ArrayList(),

    @SerializedName("SignatureDateTimeString")
    var signatureDateTimeString: Date? = null,

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null,
) : RealmObject() {

    fun equipmentNumberFilter(query: String): Boolean {
        return try {
            equipmentNumber?.toLowerCase()?.contains(query.toLowerCase()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    val customerFullAddress: String
        get() = String.format("%s, %s, %s, %s", address, city, state, zip)
    val header: String
        get() = try {
            String.format(
                "%s (%s) %s%s",
                formatTimeDate((callDate)),
                technicianStatus,
                if (isOnHold) " - " + (if (onHoldCode == null
                ) "" else onHoldCode) else "",
                if (isAssist) " - Assist" else ""
            )
        } catch (e: Exception) {
            ""
        }
    val isAssist: Boolean
        get() = try {
            if (!isValid) {
                false
            } else AppAuth.getInstance().technicianUser.technicianCode != technicianNumber &&
                    Realm.getDefaultInstance().where(ServiceCallLabor::class.java)
                        .equalTo("callId", callNumber_ID)
                        .equalTo(
                            "technicianId",
                            AppAuth.getInstance().technicianUser.technicianNumber
                        )
                        .count() > 0
        } catch (e: Exception) {
            false
        }
    private val technicianStatus: String?
        get() = try {
            if (isAssist) {
                if (isOnHold) {
                    statusCode
                } else {
                    val labors = Realm.getDefaultInstance()
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
                statusCode
            }
        } catch (e: Exception) {
            ""
        }
    val isOnHold: Boolean
        get() = statusCode_Code?.trim { it <= ' ' } == "H"
    val makeModel: String?
        get() = if (TextUtils.isEmpty(make) || TextUtils.isEmpty(model)) {
            null
        } else String.format("%s/%s", make, model)

    fun getFormattedCustomerPhoneNumber(phone: String?): String? {
        return try {
            if (phone != null) {
                val tm =
                    AppAuth.getInstance().context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val countryCodeValue = tm.networkCountryIso
                formatNumberInternationally(phone, countryCodeValue.toUpperCase())
            } else {
                phone
            }
        } catch (e: Exception) {
            phone
        }
    }

    fun getFormattedScheduleTime(): String {
        return try {
            estStartDate?.let {
                formatTimeDate(it)
            } ?: kotlin.run {
                "N/A"
            }
        } catch (e: java.lang.Exception) {
            "N/A"
        }
    }

    fun canReassignChecker(): Boolean {
        return try {
            val technicianUser = AppAuth.getInstance().technicianUser
            if (technicianUser != null) {
                var res = false
                if ((statusCode_Code?.trim { it <= ' ' } == "P" && technicianUser.technicianCode != technicianNumber
                            && technicianUser.isAllowReassignment) || statusCode_Code?.trim { it <= ' ' } == "S" &&
                    technicianUser.technicianCode != technicianNumber && technicianUser.isAllowReassignment) {
                    res = true
                }
                res
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getTechnicianStatusForFilter(): FilterHelper.ServiceCallStatus {
        return when {
            statusCode_Code?.trim { it <= ' ' } == "D" -> {
                FilterHelper.ServiceCallStatus.DISPATCHED
            }
            statusCode_Code?.trim { it <= ' ' } == "P" -> {
                FilterHelper.ServiceCallStatus.PENDING
            }
            statusCode_Code?.trim { it <= ' ' } == "S" -> {
                FilterHelper.ServiceCallStatus.SCHEDULED
            }
            statusCode_Code?.trim { it <= ' ' } == "H" -> {
                FilterHelper.ServiceCallStatus.ON_HOLD
            }
            else -> {
                FilterHelper.ServiceCallStatus.PENDING
            }
        }
    }

    companion object {
        const val CALL_NUMBER_ID = "callNumber_ID"
        const val CALL_DATE = "callDate"
        const val TECHNICIAN_STATUS_CODE = "technicianStatusCode"
        const val STATUS_ORDER = "statusOrder"
        const val TAG = "ServiceOrder"
        const val EXCEPTION = "Exception"
        const val COMPLETED = "completedCall"

        fun getStatusCallByIndex(index: Int): String{
            return when(index){
                0 -> "Pending"
                1 -> "Dispatch"
                2 -> "On Hold"
                3 -> "Scheduled"
                else -> "Pending"
            }
        }

        fun getStatusByIndex(index: Int): String{
            return when(index){
                0 -> "P"
                1 -> "D"
                2 -> "H"
                3 -> "S"
                else -> "P"
            }
        }
    }
}