package eci.technician.interfaces

import eci.technician.models.data.UsedRepairCode

interface IUsedRepairCodeListener {
    fun onUsedRepairCodePressed(item: UsedRepairCode)
}