package eci.technician.models.attachments.responses

import com.google.gson.annotations.SerializedName

data class AttachmentFile(

    @SerializedName("Base64Text")
    var base64text: String? = null,

    @SerializedName("Filename")
    var fileName: String? = null,

    @SerializedName("Id")
    val id: Int? = null
)