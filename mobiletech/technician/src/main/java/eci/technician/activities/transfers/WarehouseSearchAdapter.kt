package eci.technician.activities.transfers

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.TwoItemLayoutBinding
import eci.technician.models.transfers.Warehouse

class WarehouseSearchAdapter(
    val listener: IWarehouseSearchListener
) : ListAdapter<Warehouse,WarehouseSearchAdapter.SearchWarehouseViewHolder>(WarehouseDiffCallback()),Filterable {

    private var originalList: List<Warehouse> = mutableListOf()
    fun setOriginalListFirsTime(list: List<Warehouse>) {
        val technicianComparator = compareByDescending<Warehouse> { it.isTechnicianWarehouse }
        val letterComparator = technicianComparator.thenBy { it.warehouse }
        originalList = list?.sortedWith(letterComparator)
    }

    interface IWarehouseSearchListener {
        fun onWarehouseSearchPressed(item: Warehouse);
        fun onFilteredAction(empty:Boolean);
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchWarehouseViewHolder {
        return SearchWarehouseViewHolder.create(parent)
    }

    override fun onBindViewHolder(
        holder: SearchWarehouseViewHolder,
        position: Int
    ) {

            val item = getItem(position)
            holder.binding.text1.text = item.warehouse ?: ""
            holder.binding.text2.text = item.description ?: ""
            holder.binding.twoItemsLayoutContainer.setOnClickListener {
                listener.onWarehouseSearchPressed(item)
            }
    }

    class SearchWarehouseViewHolder(val binding: TwoItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): SearchWarehouseViewHolder {
                val binding = TwoItemLayoutBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return SearchWarehouseViewHolder(binding)
            }
        }
    }

    class WarehouseDiffCallback : DiffUtil.ItemCallback<Warehouse>(){

            override fun areItemsTheSame(
                oldItem: Warehouse,
                newItem: Warehouse
            ): Boolean {
                return oldItem.warehouseID == newItem.warehouseID
            }

            override fun areContentsTheSame(
                oldItem: Warehouse,
                newItem: Warehouse
            ): Boolean {
                return oldItem == newItem
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
                var filteredList = listOf<Warehouse>()
                if (charString.isEmpty()) {
                    filteredList = originalList
                } else {
                    filteredList = originalList.filter { warehouse ->
                        warehouse.warehouse?.contains(
                            charString,
                            ignoreCase = true
                        ) ?: false || warehouse.description.contains(charString, ignoreCase = true)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val filteredList2 = p1?.values as List<Warehouse>
                submitList(filteredList2)
                listener.onFilteredAction(filteredList2.isEmpty())
            }
        }
    }
}
