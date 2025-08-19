package eci.technician.models.attachments.persistModels

import eci.technician.models.attachments.responses.AttachmentItem
import eci.technician.repository.AttachmentRepository
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class AttachmentItemEntity : RealmObject() {

    @PrimaryKey
    var id: String = ""
    var dBFileLinkID = 0
    var createDate: String? = null
    var description: String? = null
    var filename: String? = null
    var link: String? = null
    var mimeHeader: String? = null
    var number: Int = 0

    var callNumberId: Int = 0
    var localPath: String? = null
    var isCreatedLocally: Boolean = false
    var downloadTime: Date? = null

    companion object {
        const val ID = "id"
        const val CALL_NUMBER_ID = "callNumberId"
        const val FILE_NAME = "filename"
        const val CREATED_LOCALLY = "isCreatedLocally"
        const val CUSTOM_CREATE_DATE = "customCreateDate"
        const val NUMBER = "number"
        const val LOCAL_PATH = "localPath"
        const val DB_FILE_LINK_ID = "dBFileLinkID"


        suspend fun convertToAttachmentItemEntity(
            attachmentItem: AttachmentItem,
            callNumberId: Int,
            localPath: String
        ): AttachmentItemEntity {
            val attachmentItemEntity = AttachmentItemEntity()
            val dbAttachment = AttachmentRepository.getAttachmentFromDBBy(
                fileName = attachmentItem.filename ?: "",
                callNumberId = callNumberId
            )
            attachmentItemEntity.id = dbAttachment?.id ?: UUID.randomUUID().toString()
            attachmentItemEntity.dBFileLinkID = attachmentItem.dBFileLinkID
            attachmentItemEntity.createDate = attachmentItem.createDate
            attachmentItemEntity.description = attachmentItem.description
            attachmentItemEntity.filename = attachmentItem.filename
            attachmentItemEntity.link = attachmentItem.link
            attachmentItemEntity.mimeHeader = attachmentItem.mimeHeader
            attachmentItemEntity.number = attachmentItem.number

            attachmentItemEntity.callNumberId = callNumberId
            attachmentItemEntity.localPath = localPath
            attachmentItemEntity.isCreatedLocally = false

            return attachmentItemEntity
        }
    }
}