package eci.technician.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.signalr.messenger.Message
import eci.technician.R
import eci.technician.databinding.MessageItemBinding
import eci.technician.helpers.DateTimeHelper

class MessagesAdapter(private val dataSet: MutableList<Message>, private val context: Context)
    : ListAdapter<Message,MessagesAdapter.ViewHolder>(MessagesDiffCallback()){

    var data: MutableList<Message> = dataSet

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MessageItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let {
            if (item.isMyMessage) {
                holder.binding?.friendConstraint?.visibility = View.GONE
                holder.binding?.myUserConstraint?.visibility = View.VISIBLE
                holder.binding?.myUserTextView?.text = item.message
                holder.binding?.myUserMessageTime?.text = DateTimeHelper.formatTimeDate(item.messageTime)
                holder.binding?.myUserFrameStatus?.background =
                    if (item.statusState == 2) ContextCompat.getDrawable(
                        context,
                        R.drawable.circle_message_status_seen
                    )
                    else ContextCompat.getDrawable(context, R.drawable.circle_message_status)
                if (item.statusState == 1 || item.statusState == 2){
                    holder.binding?.myUserMessageStatus?.setImageResource(R.drawable.seen)
                } else{
                    holder.binding?.myUserMessageStatus?.setImageResource(R.drawable.delivered)
                }
            }else{
                holder.binding?.friendConstraint?.visibility = View.VISIBLE
                holder.binding?.myUserConstraint?.visibility = View.GONE
                holder.binding?.friendFirstLetter?.text = item.userFirstLetter
                holder.binding?.friendDateTime?.text = DateTimeHelper.formatTimeDate(item.messageTime)
                holder.binding?.friendName?.text = item.senderName
                holder.binding?.friendTextView?.text = item.message
            }
        }
    }


    inner class ViewHolder(itemView: MessageItemBinding) : RecyclerView.ViewHolder(itemView.root) {
        var binding: MessageItemBinding? = null

        init {
            binding = itemView
        }
    }
}
class MessagesDiffCallback: DiffUtil.ItemCallback<Message>(){
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.messageId == newItem.messageId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }

}