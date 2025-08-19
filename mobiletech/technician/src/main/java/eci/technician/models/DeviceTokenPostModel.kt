package eci.technician.models

import com.google.gson.annotations.SerializedName

data class DeviceTokenPostModel(
    @SerializedName("UserId")
    val userId:String,
    @SerializedName("DeviceToken")
    val deviceToken:String,
    @SerializedName("DeviceType")
    val deviceType:String

) {
}