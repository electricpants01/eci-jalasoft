package eci.technician.helpers.sortList

import eci.technician.models.equipment.EquipmentSearchModel
import kotlin.Comparator

class EquipmentHistorySortByCode :Comparator<EquipmentSearchModel>{
    override fun compare(o1: EquipmentSearchModel, o2: EquipmentSearchModel): Int {
        var comparation = -1
        if(o1.makeCode != null && o2.makeCode != null){
            comparation = o1.makeCode.compareTo(o2.makeCode)
        }
        return comparation

    }
}