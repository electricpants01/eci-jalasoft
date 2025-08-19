package eci.technician.models.serviceCallNotes.ui

import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteTypeResponse
import eci.technician.repository.ServiceCallNotesRepository
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*


data class ServiceCallNoteUIModel(
    var noteDetailId: Long = 0,
    var callId: Int = 0,
    var noteId: Int = 0,
    var note: String = "",
    var noteTypeId: Int = 0,
    var createDate: Date = Date(),
    var lastUpdate: Date = Date(),
    var isAddedLocally: Boolean = false,
    var customUUID: String = "",
    var noteTypeString: String = "",
    var isEditable: Boolean = false


) {
    companion object {
        suspend fun createUIModelFromEntity(entity: ServiceCallNoteEntity): ServiceCallNoteUIModel {
            return ServiceCallNoteUIModel(
                noteDetailId = entity.noteDetailId,
                callId = entity.callId,
                noteId = entity.noteId,
                note = entity.note,
                noteTypeId = entity.noteTypeId,
                createDate = entity.createDate,
                lastUpdate = entity.lastUpdate,
                isAddedLocally = entity.isAddedLocally,
                customUUID = entity.customUUID,
                noteTypeString = ""
            )
        }
    }

}