package eci.technician.interfaces

import eci.technician.models.data.UsedProblemCode

interface IUsedProblemCodeListener {
    fun onUsedProblemCodePressed(item: UsedProblemCode)
}