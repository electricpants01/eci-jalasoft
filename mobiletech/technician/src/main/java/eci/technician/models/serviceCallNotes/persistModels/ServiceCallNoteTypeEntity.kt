package eci.technician.models.serviceCallNotes.persistModels

import com.google.gson.annotations.SerializedName
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteTypeResponse
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class ServiceCallNoteTypeEntity() : RealmObject() {
    @PrimaryKey
    var noteTypeId: Int = 0
    var noteType: String? = ""
    var description: String? = ""
    var isEditable: Boolean? = false
    var id: String? = ""

    companion object {
        const val NOTE_TYPE_ID = "noteTypeId"
        const val NOTE_TYPE = "noteType"
        fun convertToNoteTypeEntity(serviceCallNoteTypeResponse: ServiceCallNoteTypeResponse): ServiceCallNoteTypeEntity {
            val noteTypeEntity = ServiceCallNoteTypeEntity()
            noteTypeEntity.description = serviceCallNoteTypeResponse.description
            noteTypeEntity.id = serviceCallNoteTypeResponse.id
            noteTypeEntity.isEditable = serviceCallNoteTypeResponse.isEditable
            noteTypeEntity.noteTypeId = serviceCallNoteTypeResponse.noteTypeId
            noteTypeEntity.noteType = serviceCallNoteTypeResponse.noteType
            return noteTypeEntity
        }
    }
}