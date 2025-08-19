package eci.technician.models

import com.google.gson.annotations.SerializedName

data class TechnicianGroup(
        @SerializedName("GroupName")
        val groupName: String = "",
        @SerializedName("Description")
        val description: String = "",
        var checked: Boolean = false) {
}