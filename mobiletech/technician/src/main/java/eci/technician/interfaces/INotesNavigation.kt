package eci.technician.interfaces

interface INotesNavigation {
    fun goToAllNotesFragment(showLoadingError: Boolean)
    fun goToCreateNoteFragment()
    fun goToEditNoteFragment()

}