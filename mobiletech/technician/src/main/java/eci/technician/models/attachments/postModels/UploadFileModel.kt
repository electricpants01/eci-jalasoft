package eci.technician.models.attachments.postModels

import com.google.gson.annotations.SerializedName

data class UploadFileModel(
    @SerializedName("FileContentBase64")
    var fileContentBase64: String,
    @SerializedName("ContentType")
    var contentType: String,
    @SerializedName("FileSize")
    var fileSize: Int,
    @SerializedName("FileName")
    var fileName: String,
    @SerializedName("CallNumber")
    var callNumber: String
)

