package eci.technician.models.attachments.ui

import java.util.*

data class AttachmentItemUI(
    var id: String = "",
    var dBFileLinkID: Int = 0,
    var createDate: String? = null,
    var description: String? = null,
    var filename: String? = null,
    var link: String? = null,
    var mimeHeader: String? = null,
    var number: Int = 0,
    var callNumberId: Int = 0,
    var localPath: String? = null,
    var isCreatedLocally: Boolean = false,
    var downloadTime: Date? = null

) {


}