package eci.technician.interfaces

import eci.technician.models.filters.GroupCallType
import eci.technician.models.filters.TechnicianCallType

interface ICallTypesTechnicianList {
    fun onTapCallType(item: TechnicianCallType)

}