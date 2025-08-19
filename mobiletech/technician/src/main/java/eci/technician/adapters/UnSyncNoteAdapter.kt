package eci.technician.adapters

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.ContainerUnsyncItemBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.ApiErrorHelper
import eci.technician.interfaces.OnDeleteItemListener
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest
import eci.technician.tools.Constants
import java.text.SimpleDateFormat
import java.util.*

class UnSyncNoteAdapter(
    val unSyncList: List<NoteIncompleteRequest>,
    val listener: OnDeleteItemListener
) : RecyclerView.Adapter<UnSyncNoteAdapter.UnSyncNoteViewHolder>() {

    lateinit var context: Context

    inner class UnSyncNoteViewHolder(val binding: ContainerUnsyncItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnSyncNoteViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        return UnSyncNoteViewHolder(ContainerUnsyncItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: UnSyncNoteViewHolder, position: Int) {
        val item = unSyncList[position]
        val pattern = "hh:mm a"
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        holder.binding.primaryUnSyncTextView.maxLines = 1
        holder.binding.primaryUnSyncTextView.ellipsize = TextUtils.TruncateAt.END
        holder.binding.primaryUnSyncTextView.text = context.getString(
            R.string.un_sync_note_title,
            item.callNumberCode,
            item.note
        )
        when (item.status) {
            Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value,
            Constants.INCOMPLETE_REQUEST_STATUS.SUCCESS.value -> {
                holder.binding.secondaryUnSyncTextView.text = ""
                holder.binding.secondaryUnSyncTextView.visibility = View.GONE
                holder.binding.progressBarContainer.visibility = View.GONE
                holder.binding.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value -> {
                holder.binding.secondaryUnSyncTextView.text =
                    context.getString(R.string.in_progress)
                holder.binding.progressBarContainer.visibility = View.VISIBLE
                holder.binding.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value -> {
                holder.binding.secondaryUnSyncTextView.text =
                    ApiErrorHelper.getFormattedError(
                        item.requestErrorCode,
                        item.requestErrors ?: ""
                    )
                holder.binding.progressBarContainer.visibility = View.GONE
                holder.binding.warningContainer.visibility = View.VISIBLE
            }
        }
        if (holder.binding.warningContainer.visibility == View.VISIBLE && AppAuth.getInstance().isConnected) {
            holder.binding.btnDelete.visibility = View.VISIBLE
        } else {
            holder.binding.btnDelete.visibility = View.GONE
        }

        holder.binding.btnDelete.setOnClickListener {
            listener.onDeletedNote(item.customUUID)
        }
    }

    override fun getItemCount(): Int {
        return unSyncList.size
    }
}