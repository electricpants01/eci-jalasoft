package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.models.order.CancelCode
import io.realm.Realm
import kotlinx.android.synthetic.main.container_incomplete_code_item.view.*

class CancelCodesAdapter(var cancelCodes: List<CancelCode>, var listener: CancelCodeListener) : RecyclerView.Adapter<CancelCodesAdapter.CancelCodeViewHolder>() {
    val realm: Realm = Realm.getDefaultInstance()


    interface CancelCodeListener {
        fun updateCancelCode(item: CancelCode?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancelCodeViewHolder {
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.container_cancel_code_item, parent, false)
        return CancelCodeViewHolder(viewHolder)
    }

    override fun getItemCount(): Int {
        return cancelCodes.size
    }

    override fun onBindViewHolder(holder: CancelCodeViewHolder, position: Int) {
        val item = cancelCodes[position]
        holder.cancelCode.text = item.code
        holder.cancelCodeDescription.text = item.description
        if (item.checked != null) {
            if (item.checked) {
                holder.checkedRow.visibility = View.VISIBLE
            } else {
                holder.checkedRow.visibility = View.GONE
            }
        } else {
            holder.checkedRow.visibility = View.GONE
        }
        holder.rowContainer.setOnClickListener {

            if (item.checked) {
                realm.executeTransaction {
                    item.checked = false
                    holder.checkedRow.visibility = View.GONE
                }
                setUpList(item)
                listener.updateCancelCode(null)
            } else {
                realm.executeTransaction {
                    item.checked = true
                    holder.checkedRow.visibility = View.VISIBLE
                }
                setUpList(item)
                listener.updateCancelCode(item)
            }
        }
    }

    private fun setUpList(item: CancelCode) {
        realm.executeTransaction {
            for (code in cancelCodes) {
                if (code.cancelCodeId != item.cancelCodeId) {
                    code.checked = false
                }
            }
            notifyDataSetChanged()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        realm.close()
    }

    class CancelCodeViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var cancelCode: TextView = v.text1
        var cancelCodeDescription: TextView = v.text2
        var checkedRow: ImageView = v.checkedImage
        var rowContainer: LinearLayout = v.rowContainer
    }
}