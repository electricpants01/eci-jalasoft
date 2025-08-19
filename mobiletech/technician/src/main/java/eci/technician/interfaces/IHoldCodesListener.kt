package eci.technician.interfaces

import eci.technician.models.order.HoldCode

interface IHoldCodesListener {
    fun onHoldCodePressed(item: HoldCode);
}