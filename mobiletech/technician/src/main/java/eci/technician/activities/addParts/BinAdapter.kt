package eci.technician.activities.addParts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.ContainerBinItemBinding
import eci.technician.helpers.DecimalsHelper
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.Bin

class BinAdapter(
    val listener: ISearchableList, var partName: String,
    var partDescription: String, private val binListener: IBinTapListener,
) :
    ListAdapter<Bin, BinAdapter.BinViewHolder>(
        BINS_COMPARATOR
    ), Filterable {
    private var originalList: List<Bin> = mutableListOf()

    fun setOriginalListFirsTime(list: List<Bin>) {
        originalList = list
    }

    class BinViewHolder(val binding: ContainerBinItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): BinViewHolder {
                val binding = ContainerBinItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return BinViewHolder(binding)
            }
        }
    }

    companion object {
        private val BINS_COMPARATOR = object : DiffUtil.ItemCallback<Bin>() {
            override fun areItemsTheSame(
                oldItem: Bin,
                newItem: Bin
            ): Boolean {
                return oldItem.binId == newItem.binId
            }

            override fun areContentsTheSame(
                oldItem: Bin,
                newItem: Bin
            ): Boolean {
                return oldItem.binAvailableQuantityUI == newItem.binAvailableQuantityUI &&
                        oldItem.binAvailableQuantity == newItem.binAvailableQuantity
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinViewHolder {
        return BinViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: BinViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.binText.text = item.bin
        holder.binding.itemName.text = partName
        holder.binding.itemDescription.text = partDescription
        holder.binding.availableField.text =
            holder.binding.root.context.getString(
                R.string.bin_data,
                DecimalsHelper.getValueFromDecimal(item.binAvailableQuantityUI)
            )
        if (item.serialNumber.isNullOrEmpty()) {
            holder.binding.serialNumber.visibility = View.GONE
        } else {
            holder.binding.serialNumber.text =
                holder.binding.root.context.getString(
                    R.string.serial_number_bin,
                    item.serialNumber ?: ""
                )
            holder.binding.serialNumber.visibility = View.VISIBLE
        }

        holder.binding.containerBin.setOnClickListener {
            binListener.onBinSelected(item, item.binAvailableQuantityUI)
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
                var filteredList = listOf<Bin>()
                if (charString.isEmpty()) {
                    filteredList = originalList
                } else {
                    filteredList = originalList.filter {
                        it.bin?.contains(charString, true) == true ||
                                it.binDescription?.contains(charString, true) == true
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val filteredList2 = p1?.values as List<Bin>
                submitList(filteredList2)
                listener.onEmptyList(filteredList2.isEmpty())
            }
        }
    }
}