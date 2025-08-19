package eci.technician.interfaces

import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest
import eci.technician.models.order.IncompleteRequests

interface OnDeleteItemListener {
    fun onDeletedAction(incompleteRequests: MutableList<IncompleteRequests>)

    fun onDeletedAttachment(incompleteRequest: AttachmentIncompleteRequest)

    fun onDeletedNote(customNoteId:String)
}