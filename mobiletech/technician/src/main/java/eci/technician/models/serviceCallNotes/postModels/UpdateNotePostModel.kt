package eci.technician.models.serviceCallNotes.postModels

import com.google.gson.annotations.SerializedName
import java.util.*

data class UpdateNotePostModel(
    @SerializedName("CallID")
    var callId: Int = -1,
    @SerializedName("Note")
    var note: String = "",
    @SerializedName("NoteDetailID")
    var noteDetailId: Long = -1,
    @SerializedName("NoteTypeID")
    var noteTypeId: Int = -1,
    @SerializedName("CreatedDate")
    var createDate: Date = Date()
)
