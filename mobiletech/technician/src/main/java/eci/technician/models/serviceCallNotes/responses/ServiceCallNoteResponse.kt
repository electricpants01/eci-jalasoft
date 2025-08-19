package eci.technician.models.serviceCallNotes.responses

import com.google.gson.annotations.SerializedName
import java.util.*

data class ServiceCallNoteResponse(
    @SerializedName("CallID")
    var callId: Int? = 0,
    @SerializedName("NoteID")
    var noteId: Int? = 0,
    @SerializedName("NoteDetailID")
    var noteDetailId: Long? = 0,
    @SerializedName("Note")
    var note: String? = "",
    @SerializedName("NoteTypeID")
    var noteTypeId: Int? = 0,
    @SerializedName("CreatedDate")
    var createDate: Date? = Date(),
    @SerializedName("LastUpdate")
    var lastUpdate: Date? = Date()
)
