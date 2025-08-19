package eci.technician.models.serviceCallNotes.responses

import com.google.gson.annotations.SerializedName

data class ServiceCallNoteTypeResponse(
    @SerializedName("NoteTypeID")
    var noteTypeId: Int = 0,
    @SerializedName("NoteType")
    var noteType: String? = "",
    @SerializedName("Description")
    var description: String? = "",
    @SerializedName("IsEditable")
    var isEditable: Boolean? = false,
    @SerializedName("Id")
    var id: String? = ""
)
