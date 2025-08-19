package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.signalr.messenger.Conversation
import eci.signalr.messenger.MessengerEventListener
import eci.technician.databinding.ConversationBinding

class ConversationsAdapter(private val data: MutableList<Conversation>, val listener: MessengerEventListener) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {

    private val MAX_MESSAGES_COUNT = 99
    private val REGULAR_SIZE_COUNT = 1.0F
    private val SMALL_SIZE_COUNT = 0.8F
    private val MAX_COUNT_MESSAGE = "+99"

    inner class ConversationViewHolder(v: ConversationBinding) : RecyclerView.ViewHolder(v.root) {
        val binding: ConversationBinding = v
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ConversationBinding.inflate(inflater, parent, false)
        return ConversationViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = data[position]
        holder.binding.conversationUserName.text = item.userName
        holder.binding.conversationFirstCharacter.text = item.userFirstLetter
        holder.binding.conversationLastMessage.text = item.lastMessage
        holder.binding.conversationUnreadMessage.text = item.unreadMessageCount.toString()
        holder.binding.conversationUnreadMessage.visibility = if (item.unreadMessageCount > 0) View.VISIBLE else View.GONE
        holder.binding.root.setOnClickListener {
            listener.onConversationPressed(item)
        }

        if(item.unreadMessageCount > MAX_MESSAGES_COUNT)
        {
            holder.binding.conversationUnreadMessage.textScaleX = SMALL_SIZE_COUNT
            holder.binding.conversationUnreadMessage.text = MAX_COUNT_MESSAGE
        } else {
            holder.binding.conversationUnreadMessage.textScaleX = REGULAR_SIZE_COUNT
        }
    }

}