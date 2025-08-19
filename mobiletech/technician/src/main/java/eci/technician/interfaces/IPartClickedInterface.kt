package eci.technician.interfaces

import eci.technician.models.order.TechnicianWarehousePart

interface IPartClickedInterface {
    fun onTapPart(part: TechnicianWarehousePart)
    fun onTapDisabledItem()
}