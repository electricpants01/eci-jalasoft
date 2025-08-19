package eci.technician.activities.searchWarehouses

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.PartItemSearchBinding
import eci.technician.helpers.DecimalsHelper
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.Part

class SearchWarehousePartsAdapter(val listener: ISearchableList) :
    ListAdapter<Part, SearchWarehousePartsAdapter.SearchWarehousesPartsViewHolder>(PARTS_COMPARATOR),
    Filterable {

    private var originalList: List<Part> = mutableListOf()

    interface IEmptyList {
        fun onEmptyList(isEmpty: Boolean)
    }

    class SearchWarehousesPartsViewHolder(val binding: PartItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): SearchWarehousesPartsViewHolder {
                val binding = PartItemSearchBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return SearchWarehousesPartsViewHolder(binding)
            }
        }

    }

    companion object {
        private val PARTS_COMPARATOR = object : DiffUtil.ItemCallback<Part>() {
            override fun areItemsTheSame(
                oldItem: Part,
                newItem: Part
            ): Boolean {
                return oldItem.customId == newItem.customId
            }

            override fun areContentsTheSame(
                oldItem: Part,
                newItem: Part
            ): Boolean {
                return oldItem.availableQty == newItem.availableQty
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchWarehousesPartsViewHolder {
        return SearchWarehousesPartsViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: SearchWarehousesPartsViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.txtItem.text = item.item ?: ""
        holder.binding.txtDescription.text = item.description ?: ""
        holder.binding.txtWarehouse.text = item.warehouse ?: ""
        holder.binding.txtSummary.text = String.format(
            holder.binding.root.context.resources.getString(R.string.part_data),
            DecimalsHelper.getValueFromDecimal(item.availableQty),
            DecimalsHelper.getAmountWithCurrency(item.defaultPrice)
        )
    }

    fun setOriginalListFirstTime(it: List<Part>) {
        originalList = it
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                if (charSequence == null) {
                    val filterResults = FilterResults()
                    filterResults.values = originalList
                    return filterResults
                }
                val charString = charSequence.toString()
                var filteredList = listOf<Part>()
                if (charString.isEmpty()) {
                    filteredList = originalList
                } else {
                    filteredList = originalList.filter {
                        it.item?.contains(charString, true) == true ||
                                it.description?.contains(charString, true) == true
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val filteredList2 = p1?.values as List<Part>
                submitList(null)
                submitList(filteredList2)
                listener.onEmptyList(filteredList2.isEmpty())
            }
        }
    }
}