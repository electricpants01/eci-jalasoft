package eci.technician.activities.addParts

import eci.technician.models.order.Bin

interface IBinTapListener {
    fun onBinSelected(bin: Bin, binAvailableQuantityUI:Double)
}