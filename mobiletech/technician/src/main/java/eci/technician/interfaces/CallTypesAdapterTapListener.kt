package eci.technician.interfaces

import eci.technician.models.create_call.CallType
import eci.technician.models.filters.GroupCallType

interface CallTypesAdapterTapListener {
    fun onTapCallType(item: CallType)
}