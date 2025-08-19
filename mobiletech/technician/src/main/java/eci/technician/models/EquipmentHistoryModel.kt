package eci.technician.models

import android.content.Context
import com.google.gson.annotations.SerializedName
import eci.technician.R
import eci.technician.helpers.DateTimeHelper
import eci.technician.helpers.DecimalsHelper
import eci.technician.models.order.EquipmentMeter
import eci.technician.models.order.ProblemCode
import eci.technician.models.order.RepairCode
import java.util.*

data class EquipmentHistoryModel(
    @SerializedName("CallDate")
    var callDate: Date? = Date(),

    @SerializedName("CallNotes")
    var callNotes: String? = "",

    @SerializedName("DepartureDate")
    var departureDate: Date? = Date(),

    @SerializedName("EquipmentId")
    var equipmentID: Int? = 0,

    @SerializedName("Meters")
    val meters: MutableList<EquipmentMeter>? = mutableListOf(),

    @SerializedName("Parts")
    val parts: List<EquipmentHistoryPart>,

    @SerializedName("ProblemCodes")
    val problemCodes: MutableList<ProblemCode>? = mutableListOf(),

    @SerializedName("ProblemDescription")
    val problemDescription: String? = "",

    @SerializedName("RepairCodes")
    val repairCodes: MutableList<RepairCode>? = mutableListOf(),

    @SerializedName("ServiceCallId")
    val serviceCallID: Long? = 0,

    @SerializedName("ServiceCallNumber")
    val serviceCallNumber: String? = "",

    @SerializedName("TechnicianName")
    val technicianName: String? = "",

    @SerializedName("ServiceCaller")
    val caller: String? = ""
) {

    data class EquipmentHistoryPart(
        @SerializedName("Id")
        val id: String? = "",

        @SerializedName("Amount")
        val amount: Long? = 0,

        @SerializedName("CallId")
        val callID: Long? = 0,

        @SerializedName("Canceled")
        val canceled: Long? = 0,

        @SerializedName("Cost")
        val cost: Double? = 0.0,

        @SerializedName("CreatorID")
        val creatorID: String? = "",

        @SerializedName("Description")
        val description: String? = "",

        @SerializedName("DetailID")
        val detailID: Long? = 0,

        @SerializedName("Discount")
        val discount: Long? = 0,

        @SerializedName("Item")
        val item: String? = "",

        @SerializedName("ItemId")
        val itemID: Long? = 0,

        @SerializedName("Price")
        val price: Double? = 0.0,

        @SerializedName("Quantity")
        val quantity: Double? = 0.0,

        @SerializedName("UpdatorID")
        val updatorID: String? = "",

        @SerializedName("UsageStatusId")
        val usageStatusID: Long? = 0,

        @SerializedName("VoidCost")
        val voidCost: Double? = 0.0,

        @SerializedName("WareHouseName")
        val wareHouseName: String? = "",

        @SerializedName("WarehouseId")
        val warehouseID: Long? = 0
    ) {
    }

    fun getFormattedCallDate(): String {
        return callDate?.let {
            DateTimeHelper.formatTimeDate(it)
        } ?: ""
    }

    fun getFormattedDepartureDate(): String {
        return departureDate?.let {
            DateTimeHelper.formatTimeDate(it)
        } ?: ""
    }

    fun getEquipmentMeters(context: Context): String {
        if (meters.isNullOrEmpty()) {
            return context.getString(R.string.no_meter_data)
        } else {
            val res = StringBuilder()
            meters.let {
                for (equipmentMeter in it) {
                    var display  = if(equipmentMeter.display == null) "-" else wrapDecimalValues(equipmentMeter.display, true)
                    var actual  = if(equipmentMeter.actual == null) "-" else wrapDecimalValues(equipmentMeter.actual, true)
                    res.append(equipmentMeter.meterType ?: "").append("\n")
                    res.append("${context.getString(R.string.display_meter)}: ").append(display).append("\n")
                    res.append("${context.getString(R.string.actual_meter)}: ").append(actual).append("\n")
                    res.append("\n")
                }
            }
            if (res.isNotEmpty()) res.setLength(res.length - 2);
            return res.toString()
        }
    }

    private fun wrapDecimalValues(value: Double?, isEquipmentMeter: Boolean): String {
        if (value == null) return ""
        val valueStr = if (isEquipmentMeter) DecimalsHelper.getValueFromDecimal(value) else value.toString()
        return if (valueStr.contains(".00")) {
            valueStr.substring(0, valueStr.length - 3)
        } else {
            valueStr
        }
    }

    fun getEquipmentParts(context: Context): String {
        return if (parts.isNullOrEmpty()) {
            context.getString(R.string.no_used_parts_data)
        } else {
            val res = StringBuilder()
            var firstTime = true
            for (part in parts) {
                if (!firstTime) res.append("\n")
                firstTime = false
                res.append("${part.item ?: ""} - ${part.description ?: ""} \n")
                res.append("${context.getString(R.string.quantity)}: ${part.quantity?.toInt()?.toString() ?: 0.toString()}\n")
                res.append("${context.getString(R.string.cost)}: ${wrapDecimalValues(part.cost, false)}\n")
                res.append("${context.getString(R.string.void_cost)}: ${wrapDecimalValues(part.voidCost, false)}\n")
            }
            res.toString()
        }
    }

    fun getEquipmentProblemCodes(context: Context): String {
        return if (problemCodes.isNullOrEmpty()) {
            context.getString(R.string.no_problem_codes_data)
        } else {
            val res = StringBuilder()
            var firstTime = true
            for (problemCode in problemCodes) {
                if (!firstTime) res.append("\n")
                firstTime = false
                res.append("${problemCode.problemCodeName ?: ""} - ${problemCode.description ?: ""}\n")
            }
            res.toString()
        }
    }

    fun getEquipmentRepairCodes(context: Context): String {
        return if (repairCodes.isNullOrEmpty()) {
            context.getString(R.string.no_repairs_code_data)
        } else {
            val res = StringBuilder()
            var firstTime = true
            for (repairCode in repairCodes) {
                if (!firstTime) res.append("\n")
                firstTime = false
                res.append("${repairCode.repairCodeName ?: ""} - ${repairCode.description ?: ""}\n")
            }
            res.toString()
        }
    }
}


