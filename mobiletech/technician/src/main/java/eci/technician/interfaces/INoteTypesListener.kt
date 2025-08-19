package eci.technician.interfaces

import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity

interface INoteTypesListener {
    fun onTapNoteType( noteType: ServiceCallNoteTypeEntity)

}