package eci.technician.activities.repairCode

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.TwoItemLayoutBinding
import eci.technician.interfaces.IRepairCodeListener
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.RepairCode

class RepairCodesListAdapter(
    val searchableListener: ISearchableList
) : ListAdapter<RepairCode, RepairCodesListAdapter.RepairCodesViewHolder>(
    PROBLEM_CODE_COMPARATOR
), Filterable {

    private var originalList: List<RepairCode> = mutableListOf()
    lateinit var listener: IRepairCodeListener

    fun setRepairCodeListener(repairListener: IRepairCodeListener){
        listener = repairListener
    }

    fun setOriginalListFirsTime(list: List<RepairCode>) {
        originalList = list
        submitList(originalList)
    }

    companion object {
        private val PROBLEM_CODE_COMPARATOR = object : DiffUtil.ItemCallback<RepairCode>() {
            override fun areItemsTheSame(
                oldItem: RepairCode,
                newItem: RepairCode
            ): Boolean {
                return oldItem.repairCodeId == newItem.repairCodeId
            }

            override fun areContentsTheSame(
                oldItem: RepairCode,
                newItem: RepairCode
            ): Boolean {
                return oldItem.repairCodeName == newItem.repairCodeName &&
                        oldItem.isActive == newItem.isActive &&
                        oldItem.description == newItem.description
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepairCodesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = TwoItemLayoutBinding.inflate(inflater, parent, false)
        return RepairCodesViewHolder(viewHolder)
    }

    class RepairCodesViewHolder(v: TwoItemLayoutBinding) : RecyclerView.ViewHolder(v.root) {
        val text1: TextView = v.text1
        val text2: TextView = v.text2
        val container: LinearLayout = v.twoItemsLayoutContainer
    }

    override fun onBindViewHolder(holder: RepairCodesViewHolder, position: Int) {
        val item = getItem(position)
        holder.text1.text = item.repairCodeName ?: ""
        holder.text2.text = item.description ?: ""

        holder.container.setOnClickListener {
            listener.onProblemCodePressed(item)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (charSequence == null) {
                    filterResults.values = originalList
                    return filterResults
                }
                val charString = charSequence.toString()
                val filteredList = if (charString.isEmpty()) {
                    originalList
                } else {
                    originalList.filter {
                        it.repairCodeName?.contains(charString, true) == true ||
                                it.description?.contains(charString, true) == true
                    }
                }
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val filteredList2 = p1?.values as List<RepairCode>
                submitList(filteredList2)
                searchableListener.onEmptyList(filteredList2.isEmpty())
            }
        }
    }

}