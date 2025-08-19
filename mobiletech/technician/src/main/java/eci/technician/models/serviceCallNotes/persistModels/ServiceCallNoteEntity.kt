package eci.technician.models.serviceCallNotes.persistModels

import com.google.gson.annotations.SerializedName
import eci.technician.helpers.DateTimeHelper
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteResponse
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@RealmClass
open class ServiceCallNoteEntity : RealmObject() {
    var noteDetailId: Long = 0
    var callId: Int = 0
    var noteId: Int = 0
    var note: String = ""
    var noteTypeId: Int = 0
    var createDate: Date = Date()
    var lastUpdate: Date = Date()
    var isAddedLocally:Boolean = false
    @PrimaryKey
    var customUUID:String = ""

    companion object {
        const val CALL_ID = "callId"
        const val NOTE_DETAIL_ID = "noteDetailId"
        const val IS_ADDED_LOCALLY = "isAddedLocally"
        const val LAST_UPDATE = "lastUpdate"
        const val CUSTOM_UUID = "customUUID"

        fun convertToNoteEntity(note: ServiceCallNoteResponse): ServiceCallNoteEntity {
            val noteEntity = ServiceCallNoteEntity()
            noteEntity.noteDetailId = note.noteDetailId ?: 0
            noteEntity.callId = note.callId ?: 0
            noteEntity.note = note.note ?: ""
            noteEntity.noteTypeId = note.noteTypeId ?: 0
            noteEntity.createDate = note.createDate ?: Date()
            noteEntity.lastUpdate = note.lastUpdate ?: Date()
            return noteEntity
        }

        fun parseNoteDate(date:Date):String {
            return DateTimeHelper.formatTimeDate(date)
        }
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}