package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.models.RequestPart
import kotlinx.android.synthetic.main.request_part_item.view.*

class RequestPartsAdapter(val listener: ClearAdapterPartListener?) : RecyclerView.Adapter<RequestPartsAdapter.ViewHolder>() {
    private val items = mutableListOf<RequestPart>()

    /**
     *  This interface handle the clear action to disable the "request button" with empty items on the adapter
    */
    interface ClearAdapterPartListener{
        fun onAdapterCleared()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.txtItemName.text = item.name
        holder.itemView.txtQuantity.text = item.quantity.toInt().toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.request_part_item, parent, false))
    }

    override fun getItemCount() = items.size

    fun addPart(itemId: Int, quantity: Double, name: String) {
        items.add(RequestPart(itemId, name, quantity))
        notifyDataSetChanged()
    }

    fun getParts(): List<RequestPart> {
        return items
    }

    fun clear(){
        items.clear()
        notifyDataSetChanged()
        listener?.onAdapterCleared()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.btnRemove.setOnClickListener {
                items.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
                if (items.isEmpty()){
                    listener?.onAdapterCleared()
                }
            }
        }
    }
}