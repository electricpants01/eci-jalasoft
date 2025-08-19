package eci.technician.helpers.sortList

import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.models.order.ServiceOrder

class GroupCallsSortByTechCode : Comparator<GroupCallServiceOrder> {
    override fun compare(o1: GroupCallServiceOrder?, o2: GroupCallServiceOrder?): Int {
        var res = 0
        o1?.let { o1 ->
            o2?.let { o2 ->
                res = o1.technicianNumber.compareTo(o2.technicianNumber)
            }
        }
        return res
    }
}