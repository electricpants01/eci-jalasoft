package eci.technician.activities.addParts

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.TechnicianWhUsePartItemBinding
import eci.technician.helpers.DecimalsHelper
import eci.technician.interfaces.IPartClickedInterface
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.TechnicianWarehousePart

class AddPartsAdapter(
    val listener: ISearchableList,
    private val partsListener: IPartClickedInterface,
    private val currentTechnicianWH: Int
) :
    ListAdapter<TechnicianWarehousePart, AddPartsAdapter.AddPartsViewHolder>(
        PARTS_COMPARATOR
    ), Filterable {
    private var originalList: List<TechnicianWarehousePart> = mutableListOf()

    fun setOriginalListFirsTime(list: List<TechnicianWarehousePart>) {
        originalList = list
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddPartsViewHolder {
        return AddPartsViewHolder.create(parent)
    }

    override fun onBindViewHolder(
        holder: AddPartsViewHolder,
        position: Int
    ) {
        val item: TechnicianWarehousePart = getItem(position)
        if (item.isDisabled) {
            holder.binding.partContainerLinearLayout.setBackgroundColor(
                holder.binding.root.context.getColor(
                    R.color.disabledField
                )
            )
        } else {
            holder.binding.partContainerLinearLayout.setBackgroundColor(
                holder.binding.root.context.getColor(
                    R.color.listItemBackground
                )
            )
        }
        holder.binding.txtItem.text = item.item ?: ""
        holder.binding.txtDescription.text = item.description ?: ""
        holder.binding.warehouseNameTextView.text = item.warehouse ?: ""
        if (item.isFromTechWarehouse(currentTechnicianWH)) {
            holder.binding.warehouseNameTextView.setTypeface(null, Typeface.NORMAL)
        } else {
            holder.binding.warehouseNameTextView.setTypeface(null, Typeface.BOLD)
        }
        holder.binding.defaultPriceField.text = String.format(
            holder.binding.root.context.resources.getString(R.string.default_price),
            DecimalsHelper.getAmountWithCurrency(item.defaultPrice)
        )

        holder.binding.root.setOnClickListener {
            if (item.isDisabled) {
                partsListener.onTapDisabledItem()
            } else {
                partsListener.onTapPart(item)
            }

        }
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

    class AddPartsViewHolder(val binding: TechnicianWhUsePartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): AddPartsViewHolder {
                val binding = TechnicianWhUsePartItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return AddPartsViewHolder(binding)
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