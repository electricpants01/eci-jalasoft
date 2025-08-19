package eci.technician.models.time_cards

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DateTimeHelper.getDateFromStringWithoutTimeZone
import eci.technician.helpers.DateTimeHelper.getShortFormattedDate
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.Date


open class PayPeriod : RealmObject(), Serializable {

    companion object {
        const val PAY_PERIOD_ID = "payPeriodId"
    }

    @PrimaryKey
    var payPeriodId: String = ""

    @SerializedName("DateFrom")
    var dateFrom: String? = null

    @SerializedName("DateTo")
    var dateTo: String? = null

    @SerializedName("DateValue")
    var dateValue: Date? = null

    val formattedDate: String
        get() {
            val dFrom = getDateFromStringWithoutTimeZone(dateFrom!!)
            val dTo = getDateFromStringWithoutTimeZone(dateTo!!)
            var res = ""
            if (dFrom != null && dTo != null) {
                res = String.format(
                    "%s - %s",
                    getShortFormattedDate(dFrom),
                    getShortFormattedDate(dTo)
                )
            }
            return res
        }
}
