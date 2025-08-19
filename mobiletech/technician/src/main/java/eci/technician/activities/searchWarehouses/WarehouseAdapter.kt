package eci.technician.activities.searchWarehouses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import eci.technician.R

class WarehouseAdapter(private val items: List<String>) : BaseAdapter() {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView2 = convertView
        val viewHolder: ViewHolder
        if (convertView2 == null) {
            convertView2 = LayoutInflater.from(parent.context)
                .inflate(R.layout.single_item_layout, parent, false)
            viewHolder = ViewHolder()
            viewHolder.text1 = convertView2.findViewById(android.R.id.text1)
            convertView2.tag = viewHolder
        } else {
            viewHolder = convertView2.tag as ViewHolder
        }
        if (items.isNotEmpty()) {
            val item = items[position]
            viewHolder.text1?.text = item
        }
        return convertView2
    }

    private class ViewHolder {
        var text1: TextView? = null
    }
}