package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.serviceOrderFilter.filterModels.StatusFilter
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding

class FilterStatusAdapter(val list: MutableList<StatusFilter>) :
    RecyclerView.Adapter<FilterStatusAdapter.FilterStatusViewHolder>(), FilterAdapter {

    class FilterStatusViewHolder(val binding: ContainerOneItemLayoutSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): FilterStatusViewHolder {
                val binding = ContainerOneItemLayoutSelectableBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FilterStatusViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterStatusViewHolder {
        return FilterStatusViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: FilterStatusViewHolder, position: Int) {
        val item = list[position]
        holder.binding.mainText.text = holder.binding.root.context.getString(item.nameId)
        holder.binding.checkedImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.binding.rowContainer.setOnClickListener {
            checkItem(holder.bindingAdapterPosition, item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun unCheckOtherItems(position: Int) {
        list.forEachIndexed { index, statusFilter ->
            if (statusFilter.isChecked && index != position) {
                statusFilter.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        list.forEach {
            it.isChecked = false
        }
        notifyItemRangeChanged(0, list.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is StatusFilter) {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}