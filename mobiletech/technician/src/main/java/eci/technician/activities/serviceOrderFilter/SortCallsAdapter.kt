package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.serviceOrderFilter.filterModels.CallSortItem
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding

class SortCallsAdapter(val list: List<CallSortItem>) :
    RecyclerView.Adapter<SortCallsAdapter.SortCallViewHolder>(), FilterAdapter {

    class SortCallViewHolder(val binding: ContainerOneItemLayoutSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): SortCallViewHolder {
                return SortCallViewHolder(
                    ContainerOneItemLayoutSelectableBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortCallViewHolder {
        return SortCallViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: SortCallViewHolder, position: Int) {
        val item = list[position]
        holder.binding.mainText.text = holder.binding.root.context.getString(item.name)
        holder.binding.checkedImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.binding.rowContainer.setOnClickListener {
            if (!item.isChecked){
                checkItem(holder.absoluteAdapterPosition, item)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun unCheckOtherItems(position: Int) {
        list.forEachIndexed { index, sortItem ->
            if (sortItem.isChecked && index != position) {
                sortItem.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        list.forEach {
            it.isChecked = false
        }
        list.forEach {
            if (it.id == 1) it.isChecked = true
        }
        notifyItemRangeChanged(0, list.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is CallSortItem) {
            if (item.id == 1) {
                item.isChecked = true
            }else {
                item.isChecked = !item.isChecked
            }
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}