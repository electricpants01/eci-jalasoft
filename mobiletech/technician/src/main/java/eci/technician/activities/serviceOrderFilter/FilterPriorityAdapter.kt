package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.serviceOrderFilter.filterModels.PriorityFilter
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding

class FilterPriorityAdapter :
    ListAdapter<PriorityFilter, FilterPriorityAdapter.FilterPriorityViewHolder>(
        FILTER_DATE_COMPARATOR
    ), FilterAdapter {

    class FilterPriorityViewHolder(val binding: ContainerOneItemLayoutSelectableBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        const val TAG = "FilterPriorityAdapter"
        const val EXCEPTION = "Exception"
        private val FILTER_DATE_COMPARATOR = object : DiffUtil.ItemCallback<PriorityFilter>() {
            override fun areItemsTheSame(
                oldItem: PriorityFilter,
                newItem: PriorityFilter
            ): Boolean {
                return oldItem.priorityName == newItem.priorityName && oldItem.isChecked == newItem.isChecked
            }

            override fun areContentsTheSame(
                oldItem: PriorityFilter,
                newItem: PriorityFilter
            ): Boolean {
                return oldItem.isChecked == newItem.isChecked && oldItem.priorityName == newItem.priorityName
            }
        }
    }

    override fun onBindViewHolder(holder: FilterPriorityViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.mainText.text = item.priorityName
        holder.binding.checkedImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.binding.rowContainer.setOnClickListener {
            notifyItemChanged(holder.bindingAdapterPosition)
            item.isChecked = !item.isChecked
            disSelectOtherItems(holder.bindingAdapterPosition)
        }
    }

    private fun disSelectOtherItems(position: Int) {
        currentList.forEachIndexed { index, priorityFilter ->
            if (priorityFilter.isChecked && index != position) {
                priorityFilter.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterPriorityViewHolder {
        val binding = ContainerOneItemLayoutSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterPriorityViewHolder(binding)
    }

    override fun unCheckOtherItems(position: Int) {
        currentList.forEachIndexed { index, priorityFilter ->
            if (priorityFilter.isChecked && index != position) {
                priorityFilter.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        currentList.forEach { it.isChecked = false }
        notifyItemRangeChanged(0, currentList.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is PriorityFilter) {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}