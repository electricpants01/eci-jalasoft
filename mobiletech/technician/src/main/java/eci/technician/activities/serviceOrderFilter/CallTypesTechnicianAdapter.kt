package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.serviceOrderFilter.filterModels.CallTypeFilter
import eci.technician.databinding.ContainerTwoItemLayoutSelectableBinding

class CallTypesTechnicianAdapter(
    val callTypesList: MutableList<CallTypeFilter>,
) : RecyclerView.Adapter<CallTypesTechnicianAdapter.CallTypeViewHolder>(), FilterAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerTwoItemLayoutSelectableBinding.inflate(inflater, parent, false)
        return CallTypeViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CallTypeViewHolder, position: Int) {
        val item = callTypesList[position]
        holder.mainText.text = item.callTypeCode
        holder.secondaryText.text = item.callTypeDescription
        holder.checkImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.container.setOnClickListener {
            checkItem(holder.bindingAdapterPosition, item)
        }
    }


    override fun getItemCount(): Int {
        return callTypesList.size
    }

    class CallTypeViewHolder(v: ContainerTwoItemLayoutSelectableBinding) :
        RecyclerView.ViewHolder(v.root) {
        val binding: ContainerTwoItemLayoutSelectableBinding = v
        val container: LinearLayout = binding.rowContainer
        val mainText: TextView = binding.mainText
        val secondaryText: TextView = binding.secondaryText
        val checkImage: ImageView = binding.checkedImage
    }

    override fun unCheckOtherItems(position: Int) {
        callTypesList.forEachIndexed { index, callType ->
            if (callType.isChecked && index != position) {
                callType.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        callTypesList.forEach {
            it.isChecked = false
        }
        notifyItemRangeChanged(0, callTypesList.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is CallTypeFilter) {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}