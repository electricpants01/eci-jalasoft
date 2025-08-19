package eci.technician.models

import com.google.gson.annotations.SerializedName

data class LoginPostModel(
    @SerializedName("EAutomateId")
    val eAutomateId: String,
    @SerializedName("Email")
    val email: String,
    @SerializedName("Password")
    val password: String
) {
}