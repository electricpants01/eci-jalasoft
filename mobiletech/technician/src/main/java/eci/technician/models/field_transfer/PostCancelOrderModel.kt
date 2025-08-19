package eci.technician.models.field_transfer

import com.google.gson.annotations.SerializedName

data class PostCancelOrderModel(
    @SerializedName("ActionType")
    val actionType: String = "",
    @SerializedName("iToId")
    val toId: Int = 0
) {
    companion object {
        const val ACTION_TYPE_DELETE = "delete"
        const val ACTION_TYPE_REJECT = "reject"
    }
}