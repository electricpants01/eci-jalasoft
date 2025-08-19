package eci.technician.activities.attachment

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import eci.technician.R
import eci.technician.databinding.ContainerAttachmentItemBinding
import eci.technician.models.attachments.ui.AttachmentItemUI
import java.io.File


class AttachmentAdapter(val listener: AttachmentAdapterListener) :
    ListAdapter<AttachmentItemUI, AttachmentAdapter.AttachmentViewHolder>(ATTACHMENT_COMPARATOR) {


    interface AttachmentAdapterListener {
        fun onItemTap(item: AttachmentItemUI)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        return AttachmentViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        val item = getItem(position)
        val fullDescription = item.description + " " + item.mimeHeader
        holder.binding.txtMain.text = item.filename
        holder.binding.txtSecondary.text = fullDescription
        holder.binding.containerAttachmentRow.setOnClickListener {
            listener.onItemTap(item)
        }
        if (item.filename?.endsWith(".jpg") == true || item.filename?.endsWith(".jpeg") == true || item.filename?.endsWith(
                ".png"
            ) == true
        ) {
            val imageFile = File(item.localPath ?: "")
            if (imageFile.exists()) {
                try {
                    Picasso.with(holder.binding.root.context)
                        .load(imageFile)
                        .resize(200, 200)
                        .error(R.drawable.preview_not_available)
                        .into(holder.binding.imageThumb, object : Callback {
                            override fun onSuccess() {
                                holder.binding.containerImage.visibility = View.VISIBLE
                            }

                            override fun onError() {
                                holder.binding.containerImage.visibility = View.VISIBLE
                            }
                        })
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                    holder.binding.containerImage.visibility = View.GONE
                }
            } else {
                holder.binding.containerImage.visibility = View.GONE
            }
        } else {
            holder.binding.containerImage.visibility = View.GONE
        }
    }


    class AttachmentViewHolder(val binding: ContainerAttachmentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): AttachmentViewHolder {
                val binding = ContainerAttachmentItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return AttachmentViewHolder(binding)
            }
        }
    }

    companion object {
        const val TAG = "AttachmentAdapter"
        const val EXCEPTION = "Exception"
        private val ATTACHMENT_COMPARATOR = object : DiffUtil.ItemCallback<AttachmentItemUI>() {
            override fun areItemsTheSame(
                oldItem: AttachmentItemUI,
                newItem: AttachmentItemUI
            ): Boolean {
                return oldItem.id == newItem.id && oldItem.downloadTime == newItem.downloadTime
            }

            override fun areContentsTheSame(
                oldItem: AttachmentItemUI,
                newItem: AttachmentItemUI
            ): Boolean {

                return oldItem.localPath == newItem.localPath &&
                        oldItem.isCreatedLocally == newItem.isCreatedLocally &&
                        oldItem.downloadTime == newItem.downloadTime
            }
        }
    }
}