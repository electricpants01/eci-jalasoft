package eci.technician.models.serviceCallNotes.postModels

import com.google.gson.annotations.SerializedName
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest
import java.util.*

data class CreateNotePostModel(
    @SerializedName("CallID")
    var callId: Int = -1,
    @SerializedName("Note")
    var note: String = "",
    @SerializedName("NoteTypeID")
    var noteTypeId: Int = -1,
    @SerializedName("CreatedDate")
    var createDate: Date = Date()
) {
    companion object {
        fun instanceFromIncompleteRequest(incompleteRequest: NoteIncompleteRequest): CreateNotePostModel {
            return CreateNotePostModel(
                incompleteRequest.callId,
                incompleteRequest.note ?: "",
                incompleteRequest.noteTypeId ?: 1,
                incompleteRequest.createDate ?: Date()
            )
        }
    }
}