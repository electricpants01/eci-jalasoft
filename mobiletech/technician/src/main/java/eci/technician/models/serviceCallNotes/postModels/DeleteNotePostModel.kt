package eci.technician.models.serviceCallNotes.postModels

import com.google.gson.annotations.SerializedName

data class DeleteNotePostModel
    (
    @SerializedName("NoteDetailID")
    var noteDetailId: Long
) {
}