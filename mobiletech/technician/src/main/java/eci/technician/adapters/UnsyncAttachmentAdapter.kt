package eci.technician.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.ApiErrorHelper
import eci.technician.interfaces.OnDeleteItemListener
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.container_unsync_item.view.*

class UnsyncAttachmentAdapter(val incompleteAttachmentRequestList: MutableList<AttachmentIncompleteRequest>, val listener: OnDeleteItemListener):RecyclerView.Adapter<UnsyncAttachmentAdapter.UnsyncAttachmentViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnsyncAttachmentViewHolder {
        context = parent.context
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.container_unsync_item, parent, false)
        return UnsyncAttachmentViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: UnsyncAttachmentViewHolder, position: Int) {
        val item = incompleteAttachmentRequestList[position]
        holder.title.text = item.fileName
        holder.title.text = context.getString(R.string.un_sync_attachment_title, item.callNumber, item.fileName)
        when (item.status) {
            Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value -> {
                holder.subtitle.text = ""
                holder.subtitle.visibility = View.GONE
                holder.progressBarContainer.visibility = View.GONE
                holder.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value -> {
                holder.subtitle.text = context.getString(R.string.in_progress)
                holder.progressBarContainer.visibility = View.VISIBLE
                holder.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.SUCCESS.value -> {
                holder.subtitle.text = ""
                holder.subtitle.visibility = View.GONE
                holder.progressBarContainer.visibility = View.GONE
                holder.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value -> {
                holder.subtitle.text = ApiErrorHelper.getFormattedError(item.requestErrorCode, item.requestErrors)
                holder.progressBarContainer.visibility = View.GONE
                holder.warningContainer.visibility = View.VISIBLE
            }
        }

        if(holder.warningContainer.visibility == View.VISIBLE && AppAuth.getInstance().isConnected) {
            holder.delete.visibility = View.VISIBLE
        } else {
            holder.delete.visibility = View.GONE
        }

        holder.delete.setOnClickListener {
            listener.onDeletedAttachment(IncompleteRequestsRepository.getIncompleteAttachmentCopy(item))
        }
    }

    override fun getItemCount(): Int {
        return incompleteAttachmentRequestList.size
    }

    class UnsyncAttachmentViewHolder(v:View): RecyclerView.ViewHolder(v){
        val title: TextView = v.primaryUnSyncTextView
        val subtitle: TextView = v.secondaryUnSyncTextView
        val progressBarContainer: LinearLayout = v.progressBarContainer
        val warningContainer: LinearLayout = v.warningContainer
        val delete: ImageView = v.btnDelete
    }
}