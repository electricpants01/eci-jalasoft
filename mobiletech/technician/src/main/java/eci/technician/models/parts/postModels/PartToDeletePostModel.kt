package eci.technician.models.parts.postModels

import com.google.gson.annotations.SerializedName
import eci.technician.models.data.UsedPart

data class PartToDeletePostModel(
    @SerializedName("CallId")
    val callId:Int,
    @SerializedName("ItemId")
    val itemId:Int,
    @SerializedName("DetailID")
    val detailId:Int
) {
    companion object {
        fun createInstanceWith(part:UsedPart):PartToDeletePostModel{
            return PartToDeletePostModel(
                callId = part.callId,
                itemId = part.itemId,
                detailId = part.detailId
            )
        }
    }
}