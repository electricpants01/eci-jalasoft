package eci.technician.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.helpers.CallPriorityHelper
import eci.technician.models.ClusterObject
import kotlinx.android.synthetic.main.container_part_list_item.view.itemContainerLinearLayout
import kotlinx.android.synthetic.main.container_part_list_item.view.mainTextView
import kotlinx.android.synthetic.main.container_repeated_call_item.view.*

class CustomRepeatedListAdapter (var samePlaceCalls: MutableList<ClusterObject>, var listener: CustomRepeatedCallsListener) : RecyclerView.Adapter<CustomRepeatedListAdapter.CustomRepeatedCallsViewHolder>() {
    interface CustomRepeatedCallsListener {
        fun onItemTap(item: ClusterObject)
    }

    class CustomRepeatedCallsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mainText: TextView = v.mainTextView
        val container: LinearLayout = v.itemContainerLinearLayout
        val priorityIcon: ImageView = v.iconPriorityList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomRepeatedCallsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.container_repeated_call_item, parent, false)
        return CustomRepeatedCallsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return samePlaceCalls.size
    }

    override fun onBindViewHolder(holder: CustomRepeatedCallsViewHolder, position: Int) {
        val item = samePlaceCalls[position]
        var title = item.callNumber_Code
        if (item.status_StatusCode == "H" || item.status_StatusCode == "S") {
            title = "${item.callNumber_Code} (${item.statusCode})"
        }
        var priorityColor: Int =  CallPriorityHelper.parseColor(item.color)
        if(CallPriorityHelper.shouldDisplayPriorityFlag((priorityColor))){
            holder.priorityIcon.setColorFilter(priorityColor)
            holder.priorityIcon.visibility = View.VISIBLE
        }

        holder.mainText.text = title
        holder.container.setOnClickListener {
            listener.onItemTap(item)
        }
    }
}