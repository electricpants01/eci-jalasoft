package eci.technician.activities.allparts

import eci.technician.models.data.UsedPart

interface IPartsUpdate {

    fun markAsPending(partCustomId:String)
    fun markAsUsed(partCustomId:String)
    fun deletePendingPart(partCustomId:String)
    fun deleteUsedPart(partCustomId: String)
    fun deleteNeededPart(partCustomId: String)
    fun onLongPress(partCustomId: String)
}