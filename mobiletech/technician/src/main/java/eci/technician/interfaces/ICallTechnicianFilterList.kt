package eci.technician.interfaces

import eci.technician.models.filters.CallTechnicianFilter

interface ICallTechnicianFilterList {
    fun onTapTechnicianFilter(technicianFilter: CallTechnicianFilter)
}