package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.interfaces.INotesListener
import eci.technician.databinding.ContainerNoteBinding
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.ui.ServiceCallNoteUIModel

class NotesAdapter(
    var listOfNotes: List<ServiceCallNoteUIModel>,
    val listenerI: INotesListener,
    private val hasCreateNotesPermission: Boolean
) :
    RecyclerView.Adapter<NotesAdapter.NotesViewHolder>() {

    inner class NotesViewHolder(val binding: ContainerNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val view = ContainerNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotesViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        val item = listOfNotes[position]
        holder.binding.mainText.text = item.note
        var date = ServiceCallNoteEntity.parseNoteDate(item.lastUpdate)
        val status = holder.binding.root.context.getString(R.string.upload_pending)
        if (item.isAddedLocally) {
            date = "$date ($status)"
        }
        holder.binding.dateText.text = date
        holder.binding.typeText.text = item.noteTypeString
        holder.binding.rowContainer.setOnClickListener {
            listenerI.onTapEditNote(item.noteDetailId)
        }
        holder.binding.deleteContainer.visibility =
            if (hasCreateNotesPermission && item.isEditable) View.VISIBLE else View.GONE
        holder.binding.deleteContainer.setOnClickListener {
            listenerI.onTapDeleteNote(item.noteDetailId, item.customUUID)
        }
    }

    override fun getItemCount(): Int {
        return listOfNotes.size
    }
}

