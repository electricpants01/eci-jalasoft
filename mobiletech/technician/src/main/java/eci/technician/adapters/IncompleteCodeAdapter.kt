package eci.technician.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.models.order.IncompleteCode
import io.realm.Realm
import kotlinx.android.synthetic.main.container_incomplete_code_item.view.*

class IncompleteCodeAdapter(var incompleteCodes: List<IncompleteCode>, var listener: IncompleteCodeAdapterListener) : RecyclerView.Adapter<IncompleteCodeAdapter.IncompleteCodeViewHold>() {

    interface IncompleteCodeAdapterListener {
        fun updateIncompleteCode(item: IncompleteCode?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncompleteCodeViewHold {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.container_incomplete_code_item, parent, false)
        return IncompleteCodeViewHold(view)
    }

    override fun getItemCount(): Int {
        return incompleteCodes.size
    }

    override fun onBindViewHolder(holder: IncompleteCodeViewHold, position: Int) {
        val item = incompleteCodes[position]
        holder.incompleteCode.text = item.code
        holder.incompleteCodeDescription.text = item.description
        if (item.isChecked) {
            holder.checkedRow.visibility = View.VISIBLE
        } else {
            holder.checkedRow.visibility = View.GONE
        }
        holder.rowContainer.setOnClickListener {
            val realm = Realm.getDefaultInstance()
            if (item.isChecked) {
                realm.executeTransaction {
                    item.isChecked = false
                    holder.checkedRow.visibility = View.GONE
                }
                setUpList(item)
                listener.updateIncompleteCode(null)
            } else {
                realm.executeTransaction {
                    item.isChecked = true
                    holder.checkedRow.visibility = View.VISIBLE
                }
                setUpList(item)
                listener.updateIncompleteCode(item)
            }
        }
    }

    private fun setUpList(item: IncompleteCode) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            for (code in incompleteCodes) {
                if (code.incompleteCodeId != item.incompleteCodeId) {
                    code.isChecked = false
                }
            }
            notifyDataSetChanged()
        }
    }

    class IncompleteCodeViewHold(v: View) : RecyclerView.ViewHolder(v) {
        var incompleteCode: TextView = v.text1
        var incompleteCodeDescription: TextView = v.text2
        var checkedRow: ImageView = v.checkedImage
        var rowContainer: LinearLayout = v.rowContainer
    }
}