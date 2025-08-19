package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.serviceOrderFilter.filterModels.DateFilter
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding

class FilterDateAdapter(val list: List<DateFilter>) :
    RecyclerView.Adapter<FilterDateAdapter.FilterDateViewHolder>(), FilterAdapter {

    class FilterDateViewHolder(val binding: ContainerOneItemLayoutSelectableBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: FilterDateViewHolder, position: Int) {
        val item = list[position]
        holder.binding.mainText.text = holder.binding.root.context.getString(item.value)
        holder.binding.checkedImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.binding.rowContainer.setOnClickListener {
            checkItem(holder.bindingAdapterPosition, item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterDateViewHolder {
        val binding = ContainerOneItemLayoutSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterDateViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun unCheckOtherItems(position: Int) {
        list.forEachIndexed { index, dateFilter ->
            if (dateFilter.isChecked && index != position) {
                dateFilter.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        list.forEach { it.isChecked = false }
        notifyItemRangeChanged(0, list.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is DateFilter) {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}