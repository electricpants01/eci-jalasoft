package eci.technician.activities.serviceOrderFilter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.activities.serviceOrderFilter.filterModels.GroupFilter
import kotlinx.android.synthetic.main.container_filter_date_item.view.*

class TechnicianGroupsAdapter(
    var groupList: MutableList<GroupFilter>,
) : RecyclerView.Adapter<TechnicianGroupsAdapter.TechnicianGroupViewHolder>(), FilterAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TechnicianGroupViewHolder {
        val viewHolder = LayoutInflater.from(parent.context)
            .inflate(R.layout.container_filter_date_item, parent, false)
        return TechnicianGroupViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: TechnicianGroupViewHolder, position: Int) {
        val item = groupList[position]
        holder.mainText.text = item.groupName
        holder.secondaryText.text = item.description
        holder.checkImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.container.setOnClickListener {
            checkItem(holder.bindingAdapterPosition, item)
        }
    }

    override fun getItemCount(): Int {
        return groupList.size
    }

    class TechnicianGroupViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mainText: TextView = v.mainText
        val secondaryText: TextView = v.secondaryText
        val container: LinearLayout = v.rowContainer
        val checkImage: AppCompatImageView = v.checkedImage
    }

    override fun unCheckOtherItems(position: Int) {
        groupList.forEachIndexed { index, statusFilter ->
            if (statusFilter.isChecked && index != position) {
                statusFilter.isChecked = false
                notifyItemChanged(index)
            }
        }
    }

    override fun unCheckAllItems() {
        groupList.forEach {
            it.isChecked = false
        }
        notifyItemRangeChanged(0, groupList.size)
    }

    override fun <T> checkItem(position: Int, item: T) {
        if (item is GroupFilter) {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
            unCheckOtherItems(position)
        }
    }
}