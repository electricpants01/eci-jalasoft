package eci.technician.models.order

import android.location.Location
import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DateTimeHelper.getTimeBetweenCurrentTimeAndDate
import eci.technician.helpers.DecimalsHelper
import eci.technician.tools.Constants
import java.util.*


data class RequestPartTransferItem(
        @SerializedName("Id")
        var id: String? = null,
        @SerializedName("AvailableQty")
        var availableQty: Double = 0.0,
        @SerializedName("BinID")
        var binID: Int = 0,
        @SerializedName("CurrentCallAddress")
        var currentCallAddress: String? = null,
        @SerializedName("CallNumber")
        var callNumber: String? = null,
        @SerializedName("DefaultPrice")
        var defaultPrice: Double = 0.0,
        @SerializedName("Description")
        var description: String? = null,
        @SerializedName("GPSLatitude")
        var gPSLatitude: Double = 0.0,
        @SerializedName("GPSLongitude")
        var gPSLongitude: Double = 0.0,
        @SerializedName("GpsUpdateTime")
        var gpsUpdateTime: Date? = null,
        @SerializedName("Item")
        var item: String? = null,
        @SerializedName("ItemId")
        var itemId: Int = 0,
        @SerializedName("TechnicianID")
        var technicianID: Int = 0,
        @SerializedName("TechnicianName")
        var technicianName: String? = null,
        @SerializedName("TechnicianNumber")
        var technicianNumber: String? = null,
        @SerializedName("TechnicianPhone")
        var technicianPhone: String? = null,
        @SerializedName("Warehouse")
        var warehouse: String? = null,
        @SerializedName("WarehouseId")
        var warehouseId: Int = 0,
        @SerializedName("ChatIdent")
        var chatIdent: String? = null,
        @SerializedName("IsTechnicianActive")
        var isTechnicianActive: Boolean = false,
        @SerializedName("IsTechnicianInGroup")
        var isTechnicianInGroup: Boolean = false,
        @SerializedName("IsTechnicianRegistered")
        var isTechnicianRegistered: Boolean = false,
        @SerializedName("City")
        var city: String? = null,
        @SerializedName("State")
        var state: String? = null,
        @SerializedName("Zip")
        var zip: String? = null,

        var distance: Float
) {

    fun getTechnicianPhoneFixed(): String? {
        if (technicianPhone != null) {
            technicianPhone = technicianPhone!!.replace("[^0-9]".toRegex(), "")
            if (technicianPhone!!.length >= 7 && technicianPhone!!.length <= 8) {
                technicianPhone = technicianPhone!!.substring(0, 3) + "-" + technicianPhone!!.substring(3)
            }
            if (technicianPhone!!.length >= 9) {
                technicianPhone = "(" + technicianPhone!!.substring(0, 3) + ") " + technicianPhone!!.substring(3, 6) + "-" + technicianPhone!!.substring(6)
            }
        }
        return technicianPhone
    }


    fun computeDistance(lastLocation: Location?): Float {
        if (gPSLatitude != 0.0 && gPSLongitude != 0.0 && lastLocation != null) {
            val location = Location("")
            location.latitude = gPSLatitude
            location.longitude = gPSLongitude
            return lastLocation.distanceTo(location) * 0.0006213712f
        }
        return Float.MAX_VALUE
    }

    fun distanceFormatted(): String {
        return if (gpsUpdateTime != null) {
            val timeDifferenceInMinutes = getTimeBetweenCurrentTimeAndDate(gpsUpdateTime!!)
            if (timeDifferenceInMinutes > 30) {
                return Constants.UNKNOWN_LOCATION
            }
            var distance = distance
            if (distance == Float.MAX_VALUE) {
                distance = 0f
                return Constants.UNKNOWN_LOCATION
            }
            String.format(Locale.US, "%.2f", distance) + " miles"
        } else {
            Constants.UNKNOWN_LOCATION
        }
    }

    fun isValidLocation(): Boolean {
        return if (gpsUpdateTime != null) {
            val timeDifferenceInMinutes = getTimeBetweenCurrentTimeAndDate(gpsUpdateTime!!)
            if (timeDifferenceInMinutes > 30) return false
            distance != Float.MAX_VALUE
        } else {
            false
        }
    }

    fun getQuantityFormatted():String{
        return DecimalsHelper.getValueFromDecimal(this.availableQty)
    }
}