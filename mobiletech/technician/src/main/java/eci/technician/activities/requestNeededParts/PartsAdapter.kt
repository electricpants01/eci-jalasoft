package eci.technician.activities.requestNeededParts

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.activities.searchWarehouses.SearchWarehousePartsAdapter
import eci.technician.databinding.PartItemBinding
import eci.technician.models.order.Part

class PartsAdapter(
    val emptyListener: SearchWarehousePartsAdapter.IEmptyList, val listener: PartRequestNeedListener
) : ListAdapter<Part, PartsAdapter.PartsViewHolder>(PARTS_COMPARATOR), Filterable {

    interface PartRequestNeedListener {
        fun onTapPart(part: Part)
    }

    private var originalList: List<Part> = mutableListOf()

    fun setOriginalListFirsTime(list: List<Part>) {
        originalList = list
    }

    class PartsViewHolder(val binding: PartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): PartsViewHolder {
                val binding =
                    PartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return PartsViewHolder(binding)
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
                return oldItem.item == newItem.item &&
                        oldItem.description == newItem.description &&
                        oldItem.availableQty == newItem.availableQty
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartsViewHolder {
        return PartsViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: PartsViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.item = item
        holder.binding.root.setOnClickListener {
            listener.onTapPart(item)
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
                if (p1?.values == null){
                    submitList(null)
                    submitList(listOf())
                }else {
                    val filteredList2 = p1.values as List<Part>
                    submitList(null)
                    submitList(filteredList2.toMutableList())
                    emptyListener.onEmptyList(filteredList2.isEmpty())
                }
            }
        }
    }
}