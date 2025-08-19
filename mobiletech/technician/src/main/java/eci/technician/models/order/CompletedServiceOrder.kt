package eci.technician.models.order

import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DateTimeHelper.formatTimeDate
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.*


open class CompletedServiceOrder : RealmObject() {

    @SerializedName("CallNumber_ID")
    var callNumber_ID: Int = 0

    @PrimaryKey
    @SerializedName("Id")
    var id: String? = ""

    @SerializedName("Address")
    var address: String? = ""

    @SerializedName("CallDate")
    var callDate: Date? = Date()

    @SerializedName("CallNumber_Code")
    var callNumber_Code: String? = ""

    @SerializedName("CallType")
    var callType: String? = ""

    @SerializedName("Caller")
    var caller: String? = ""

    @SerializedName("ChatIdent")
    var chatIdent: String? = ""

    @SerializedName("City")
    var city: String? = ""

    @SerializedName("CustomerName")
    var customerName: String? = ""

    @SerializedName("CustomerNumber")
    var customerNumber: String? = ""

    @SerializedName("Description")
    var description: String? = ""

    @SerializedName("Duetime")
    var dueDate: Date? = Date()

    @SerializedName("EquipmentNumber")
    var equipmentNumber: String? = ""

    @SerializedName("EquipmentId")
    var equipmentId = 0

    @SerializedName("Equipmentlocation")
    var equipmentLocation: String? = ""

    @SerializedName("Equipmentremarks")
    var equipmentRemarks: String? = ""

    @SerializedName("IPAddress")
    var ipAddress: String? = ""

    @SerializedName("IsSupplyOrder")
    var isSupplyOrder = false

    @SerializedName("MACAddress")
    var macAddress: String? = ""

    @SerializedName("Make")
    var make: String? = ""

    @SerializedName("Model")
    var model: String? = ""

    @SerializedName("SerialNumber")
    var serialNumber: String? = ""

    @SerializedName("ServiceType")
    var serviceType: String? = ""

    @SerializedName("State")
    var state: String? = ""

    @SerializedName("StatusCode")
    var statusCode: String? = ""

    @SerializedName("StatusCode_Code")
    var statusCode_Code: String? = ""

    @SerializedName("TechnicianId")
    var technicianId: String? = ""

    @SerializedName("TechnicianName")
    var technicianName: String? = ""

    @SerializedName("TechnicianNumber")
    var technicianNumber: String? = ""

    @SerializedName("TechnicianNumberId")
    var technicianNumberId: Int = 0

    @SerializedName("TechnicianRate")
    var technicianRate: Int = 0

    @SerializedName("TechnicianRateComment")
    var technicianRateComment: String? = ""

    @SerializedName("Zip")
    var zip: String? = ""

    @SerializedName("ContactName")
    var contactName: String? = ""

    @SerializedName("ContactPhone")
    var contactPhone: String? = ""

    @SerializedName("ContactEmail")
    var contactEmail: String? = ""

    @SerializedName("BillCode")
    var billCode: String? = ""

    @SerializedName("Terms")
    var terms: String? = ""

    @SerializedName("CallPriority")
    var callPriority: String? = ""

    @SerializedName("EquipmentInstallDate")
    var equipmentInstallDate: String? = ""

    @SerializedName("EquipmentPrimaryNote")
    var equipmentPrimaryNote: String? = ""

    @SerializedName("WorkOrderNumber")
    var workOrderNumber: String? = ""

    @SerializedName("CallNotes")
    var callNotes: String? = ""

    @SerializedName("CustomerPhoneNumber")
    var customerPhoneNumber: String? = ""

    @SerializedName("ArriveTime")
    var arriveTime: Date? = Date()

    @SerializedName("DepartTime")
    var departTime: Date? = Date()

    @SerializedName("DispatchTime")
    var dispatchTime: Date? = Date()

    @SerializedName("MasterCallId")
    var masterCallId: Int = 0

    @SerializedName("OnHoldCode")
    var onHoldCode: String? = ""

    @SerializedName("OnHoldDescription")
    var onHoldDescription: String? = ""

    @SerializedName("RescheduledCallId")
    var rescheduledCallId: Int = 0

    @SerializedName("SignatureDateTimeString")
    var signatureDateTimeString: Date? = Date()

    @SerializedName("DefaultActivityCodeId")
    var defaultActivityCodeId: String? = ""

    @SerializedName("PoNumber")
    var poNumber: String? = ""

    @Ignore
    @SerializedName("Labors")
    var labors: List<ServiceCallLabor> = ArrayList()

    fun getFormattedDepartTime(): String {
        return departTime?.let {
            formatTimeDate(it)
        } ?: "N/A"
    }

    fun getMakeModel(): String {
        return if (TextUtils.isEmpty(make) || TextUtils.isEmpty(model)) {
            ""
        } else String.format("%s/%s", make, model)
    }

    fun getCustomerFullAddress(): String {
        return String.format("%s, %s, %s, %s", address ?: "", city ?: "", state ?: "", zip ?: "")
    }

    fun getFormattedArriveTime(): String {
        return arriveTime?.let {
            formatTimeDate(it)
        } ?: "N/A"
    }

    fun getFormattedDispatchTime(): String {
        return dispatchTime?.let {
            formatTimeDate(it)
        } ?: "N/A"
    }

    fun getHeader(): String {
        return String.format("%s (%s) %s%s",
                callDate?.let {
                    formatTimeDate(it)
                } ?: "N/A" ,
                statusCode,
                "",
                "")
    }

    companion object {
        const val CALL_NUMBER_CODE = "callNumber_Code"
        const val CALL_NUMBER_ID = "callNumber_ID"
        const val ID = "id"
    }
}