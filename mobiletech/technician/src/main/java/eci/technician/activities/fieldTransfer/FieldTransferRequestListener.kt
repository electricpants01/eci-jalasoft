package eci.technician.activities.fieldTransfer

import com.google.gson.JsonElement
import eci.technician.models.field_transfer.PartRequestTransfer

interface FieldTransferRequestListener {
    fun onAcceptClick(item: PartRequestTransfer)
    fun onRejectClick(item: PartRequestTransfer)
    fun onCancelClick(item: PartRequestTransfer)
}