package eci.technician.activities.problemCodes

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.TwoItemLayoutBinding
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.ProblemCode

class ProblemCodesListAdapter(
    val listener: IProblemCodeListener,
    val searchableListener: ISearchableList
) : ListAdapter<ProblemCode, ProblemCodesListAdapter.ProblemCodesViewHolder>(
    PROBLEM_CODE_COMPARATOR
), Filterable {

    private var originalList: List<ProblemCode> = mutableListOf()

    interface IProblemCodeListener {
        fun onProblemCodePressed(item: ProblemCode)
    }

    fun setOriginalListFirsTime(list: List<ProblemCode>) {
        originalList = list
        submitList(originalList)
    }

    companion object {
        private val PROBLEM_CODE_COMPARATOR = object : DiffUtil.ItemCallback<ProblemCode>() {
            override fun areItemsTheSame(
                oldItem: ProblemCode,
                newItem: ProblemCode
            ): Boolean {
                return oldItem.problemCodeId == newItem.problemCodeId
            }

            override fun areContentsTheSame(
                oldItem: ProblemCode,
                newItem: ProblemCode
            ): Boolean {
                return oldItem.problemCodeName == newItem.problemCodeName &&
                        oldItem.isActive == newItem.isActive
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemCodesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = TwoItemLayoutBinding.inflate(inflater, parent, false)
        return ProblemCodesViewHolder(viewHolder)
    }

    class ProblemCodesViewHolder(v: TwoItemLayoutBinding) : RecyclerView.ViewHolder(v.root) {
        val text1: TextView = v.text1
        val text2: TextView = v.text2
        val container: LinearLayout = v.twoItemsLayoutContainer
    }

    override fun onBindViewHolder(holder: ProblemCodesViewHolder, position: Int) {
        val item = getItem(position)
        holder.text1.text = item.problemCodeName ?: ""
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
                        it.problemCodeName?.contains(charString, true) == true ||
                                it.description?.contains(charString, true) == true
                    }
                }
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                val filteredList2 = p1?.values as List<ProblemCode>
                submitList(filteredList2)
                searchableListener.onEmptyList(filteredList2.isEmpty())
            }
        }
    }

}