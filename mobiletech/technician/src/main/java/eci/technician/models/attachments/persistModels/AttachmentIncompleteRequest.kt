package eci.technician.models.attachments.persistModels

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class AttachmentIncompleteRequest():RealmObject() {
    @PrimaryKey
    var id:String = ""
    var contentType: String = ""
    var fileSize: Int = 0
    var fileName: String = ""
    var callNumber: String = ""
    var callNumberId:Int = 0
    var dateAdded: Date? = null
    var status: Int? = null
    var requestErrors: String = ""
    var requestErrorCode: Int = 0
    var requestCategory: Int? = null
    var attachmentItemId: Int? = null
    var attachmentItemEntityId:String = ""
    var localPath: String = ""

    object COLUMNS{
        const val ID = "id"
        const val STATUS = "status"
        const val DATE_ADDED = "dateAdded"
        const val CALL_NUMBER_ID = "callNumberId"
        const val FILE_NAME = "fileName"
    }
}