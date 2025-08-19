package eci.technician.interfaces


interface INotesListener {
    fun onTapEditNote(noteId: Long)
    fun onTapDeleteNote(noteDetailId: Long, customUUID: String)
}