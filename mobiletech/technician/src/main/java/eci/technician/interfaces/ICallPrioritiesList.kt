package eci.technician.interfaces

import eci.technician.models.filters.CallPriorityFilter

interface ICallPrioritiesList {

    fun onTapPriority( priority: CallPriorityFilter)
}