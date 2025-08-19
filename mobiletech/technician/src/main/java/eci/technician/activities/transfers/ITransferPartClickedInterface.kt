package eci.technician.activities.transfers

import eci.technician.models.transfers.Part

interface ITransferPartClickedInterface {
    fun onTapPart(part: Part)
    fun onFilteredAction();
}