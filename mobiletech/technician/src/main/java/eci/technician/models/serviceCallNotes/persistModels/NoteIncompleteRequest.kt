package eci.technician.models.serviceCallNotes.persistModels

import io.realm.RealmObject
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class NoteIncompleteRequest : RealmObject() {
    var callId: Int = 0
    var note: String? = ""
    var noteDetailId: Long = 0
    var noteTypeId: Int = 0
    var createDate: Date? = Date()
    var status: Int = 0
    var requestErrors: String? = ""
    var requestErrorCode: Int = 0
    var requestCategory: Int = 0
    var dateAdded: Date? = Date()
    var isCreatedLocally: Boolean = false
    var customUUID: String = ""
    var callNumberCode:String = ""

    companion object {
        const val STATUS = "status"
        const val DATE_ADDED = "dateAdded"
        const val CUSTOM_UUID = "customUUID"

        const val REQUEST_TYPE_CREATE = 1
        const val REQUEST_TYPE_UPDATE = 2
        const val REQUEST_TYPE_DELETE = 3
    }

    /**
     * Request for create = 1 (CreateServiceCallNotes)
     * Request for update = 2 (UpdateServiceCallNotes)
     */
}