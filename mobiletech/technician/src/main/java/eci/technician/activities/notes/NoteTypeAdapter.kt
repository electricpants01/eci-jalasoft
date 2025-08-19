package eci.technician.activities.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.notes.NoteTypeAdapter.NoteTypeViewHolder
import eci.technician.databinding.ContainerNoteTypeBinding
import eci.technician.interfaces.INoteTypesListener
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity

class NoteTypeAdapter(
        private val noteTypesList: List<ServiceCallNoteTypeEntity>,
        private val listener: INoteTypesListener,
        private val dialog: DialogFragment,
) : RecyclerView.Adapter<NoteTypeViewHolder>() {

    class NoteTypeViewHolder(v: ContainerNoteTypeBinding) : RecyclerView.ViewHolder(v.root) {
        val binding: ContainerNoteTypeBinding = v
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerNoteTypeBinding.inflate(inflater, parent, false)
        return NoteTypeViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: NoteTypeViewHolder, position: Int) {
        val item = noteTypesList[position]
        holder.binding.mainText.text = item.noteType
        holder.binding.secondaryText.text = item.description
        holder.binding.rowContainer.setOnClickListener {
            listener.onTapNoteType(item)
            dialog.dismiss()
        }
    }

    override fun getItemCount(): Int {
        return noteTypesList.size
    }
}