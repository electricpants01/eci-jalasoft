package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.serviceOrderFilter.filterModels.TechnicianFilter
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding

class CallTechnicianAdapter(
    val callTechnicianList: MutableList<TechnicianFilter>,
) : RecyclerView.Adapter<CallTechnicianAdapter.CallTechnicianViewHolder>(), FilterAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallTechnicianViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerOneItemLayoutSelectableBinding.inflate(inflater, parent, false)
        return CallTechnicianViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CallTechnicianViewHolder, position: Int) {
        val item = callTechnicianList[position]
        holder.mainTextView.text = item.technicianName
        holder.checkImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.container.setOnClickListener {
            checkItem(holder.bindingAdapterPosition, item)
        }
    }

    override fun getItemCount(): Int {
        return callTechnicianList.size
    }

    class CallTechnicianViewHolder(v: ContainerOneItemLayoutSelectableBinding) :
        RecyclerView.ViewHolder(v.root) {
        val binding: ContainerOneItemLayoutSelectableBinding = v
        val mainTextView: TextView = binding.mainText
        val container: LinearLayout = binding.rowContainer
        val checkImage: ImageView = binding.checkedImage
    }

    override fun unCheckOtherItems(position: Int) {
        callTechnicianList.forEachIndexed { index, technicianFilter ->
            if (technicianFilter.isChecked && index != position) {
                technicianFilter.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        callTechnicianList.forEach {
            it.isChecked = false
        }
        notifyItemRangeChanged(0, callTechnicianList.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is TechnicianFilter) {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}