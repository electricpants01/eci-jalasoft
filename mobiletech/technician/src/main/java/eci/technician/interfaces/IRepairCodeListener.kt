package eci.technician.interfaces

import eci.technician.models.order.RepairCode

interface IRepairCodeListener {
    fun onProblemCodePressed(item: RepairCode);
}