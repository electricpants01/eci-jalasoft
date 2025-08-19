package eci.technician.models.attachments.responses

import com.google.gson.annotations.SerializedName

data class AttachmentItem(
    @SerializedName("DBFileLinkID")
    var dBFileLinkID: Int = 0,

    @SerializedName("CreateDate")
    var createDate: String? = null,

    @SerializedName("Description")
    var description: String? = null,

    @SerializedName("Filename")
    var filename: String? = null,

    @SerializedName("Link")
    var link: String? = null,

    @SerializedName("MimeHeader")
    var mimeHeader: String? = null,

    @SerializedName("Number")
    var number: Int = 0

)