package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import eci.technician.adapters.RequestTransferAdapter.ViewHolder
import eci.technician.databinding.PartResultItemBinding
import eci.technician.helpers.AppAuth
import eci.technician.models.order.RequestPartTransferItem

class RequestTransferAdapter(private val items: MutableList<RequestPartTransferItem>, val listener: RequestTransferListener) :
        RecyclerView.Adapter<ViewHolder>() {

    interface RequestTransferListener {
        fun onPartRequestedClick(item: RequestPartTransferItem)
        fun onPhoneNumberClick(item: RequestPartTransferItem)
        fun onChatClick(item: RequestPartTransferItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PartResultItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding?.item = item

        holder.binding?.btnChat?.visibility = if (!AppAuth.getInstance().chatEnabled || item.chatIdent.isNullOrEmpty()) INVISIBLE else VISIBLE
        holder.binding?.txtViewDispatchedCallAddress?.text = dispatchedCallAddressString(item.currentCallAddress, item.city, item.state, item.zip)
    }

    private fun dispatchedCallAddressString(address: String?, city: String?, state: String?, zip: String?): String {
        var res = ""
        address?.let { res += "$it\n" }
        city?.let { res += it }
        state?.let { res += ", $it" }
        zip?.let { res += ", $it" }
        return res
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: PartResultItemBinding? = null

        init {
            binding = DataBindingUtil.bind(itemView)
            binding?.btnRequest?.setOnClickListener { listener.onPartRequestedClick(items[adapterPosition]) }
            binding?.txtPhoneNumber?.setOnClickListener { listener.onPhoneNumberClick(items[adapterPosition]) }
            binding?.btnChat?.setOnClickListener { listener.onChatClick(items[adapterPosition]) }
        }
    }
}