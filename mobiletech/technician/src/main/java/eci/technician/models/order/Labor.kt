package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import java.util.*

class Labor {
    @SerializedName("ArriveTime")
    var arriveTime: String? = null

    @SerializedName("DispatchTime")
    var dispatchTime: String? = null

    @SerializedName("DispatchTimeString")
    var dispatchTimeString: Date? = null

    @SerializedName("ArriveTimeString")
    var arriveTimeString: Date? = null
}