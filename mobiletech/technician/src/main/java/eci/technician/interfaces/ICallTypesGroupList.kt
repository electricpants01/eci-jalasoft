package eci.technician.interfaces

import eci.technician.models.filters.GroupCallType

interface ICallTypesGroupList {
    fun onTapCallType(item: GroupCallType)
}