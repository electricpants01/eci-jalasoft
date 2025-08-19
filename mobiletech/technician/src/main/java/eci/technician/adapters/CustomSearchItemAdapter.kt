package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.models.order.Part
import kotlinx.android.synthetic.main.container_part_list_item.view.*

class CustomSearchItemAdapter(var partsList: MutableList<Part>, var listener: CustomSearchItemAdapterListener) : RecyclerView.Adapter<CustomSearchItemAdapter.CustomSearchItemViewHolder>() {

    interface CustomSearchItemAdapterListener {
        fun onItemTap(item: Part)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomSearchItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.container_part_list_item, parent, false)
        return CustomSearchItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return partsList.size
    }

    override fun onBindViewHolder(holder: CustomSearchItemViewHolder, position: Int) {
        val item = partsList[position]
        holder.mainText.text = item.item  ?: ""
        holder.secondText.text = item.description ?: ""
        holder.container.setOnClickListener {
            listener.onItemTap(item)
        }
    }

    class CustomSearchItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mainText: TextView = v.mainTextView
        val secondText: TextView = v.secondTextView
        val container: LinearLayout = v.itemContainerLinearLayout
    }
}