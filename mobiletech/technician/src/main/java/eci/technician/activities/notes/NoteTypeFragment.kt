package eci.technician.activities.notes

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.interfaces.INoteTypesListener
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity


class NoteTypeFragment(
        private val typesList: List<ServiceCallNoteTypeEntity>,
        private val listener: INoteTypesListener,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
        builder.setTitle(getString(R.string.note_types))
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.container_note_types, null)
        builder.setView(dialogView)
        val adapter: RecyclerView = dialogView.findViewById(R.id.recNoteTypes)
        adapter.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        adapter.adapter = NoteTypeAdapter(typesList, listener, this)
        builder.setNegativeButton(
            context?.resources?.getString(R.string.cancel)
        ) { dialog, _ -> dialog.dismiss() }
        return builder.create()
    }


    companion object {
        const val TAG = "NoteTypeConfirmationDialog"
    }
}
