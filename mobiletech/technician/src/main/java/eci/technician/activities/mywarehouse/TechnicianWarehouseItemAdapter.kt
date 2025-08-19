package eci.technician.activities.mywarehouse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.SearchableTechnicianWarehouserPartItemBinding
import eci.technician.helpers.DecimalsHelper
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.TechnicianWarehousePart

class TechnicianWarehouseItemAdapter(val listener: ISearchableList) :
    ListAdapter<TechnicianWarehousePart, TechnicianWarehouseItemAdapter.TechnicianWarehouseItemViewHolder>(
        PARTS_COMPARATOR
    ), Filterable {
    private var originalList: List<TechnicianWarehousePart> = mutableListOf()

    fun setOriginalListFirsTime(list: List<TechnicianWarehousePart>) {
        originalList = list
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TechnicianWarehouseItemViewHolder {
        return TechnicianWarehouseItemViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: TechnicianWarehouseItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.item = item
        holder.binding.availableField.text = holder.binding.root.context.getString(
            R.string.transfer_part_data,
            DecimalsHelper.getValueFromDecimal(item.availableQuantityUI)
        )
        holder.binding.defaultPriceField.text = holder.binding.root.context.getString(
            R.string.default_price,
            DecimalsHelper.getAmountWithCurrency(item.defaultPrice)
        )
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
                var filteredList = listOf<TechnicianWarehousePart>()
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
                val filteredList2 = p1?.values as List<TechnicianWarehousePart>
                submitList(filteredList2)
                listener.onEmptyList(filteredList2.isEmpty())
            }
        }
    }

    class TechnicianWarehouseItemViewHolder(val binding: SearchableTechnicianWarehouserPartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): TechnicianWarehouseItemViewHolder {
                val binding = SearchableTechnicianWarehouserPartItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return TechnicianWarehouseItemViewHolder(binding)
            }
        }
    }


    companion object {
        private val PARTS_COMPARATOR = object : DiffUtil.ItemCallback<TechnicianWarehousePart>() {
            override fun areItemsTheSame(
                oldItem: TechnicianWarehousePart,
                newItem: TechnicianWarehousePart
            ): Boolean {
                return oldItem.customId == newItem.customId
            }

            override fun areContentsTheSame(
                oldItem: TechnicianWarehousePart,
                newItem: TechnicianWarehousePart
            ): Boolean {
                return oldItem.availableQuantityUI == newItem.availableQuantityUI &&
                        oldItem.availableQty == newItem.availableQty
            }
        }
    }
}