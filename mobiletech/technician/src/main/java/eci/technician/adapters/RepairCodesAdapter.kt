package eci.technician.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.UsedRepairCodeBinding
import eci.technician.interfaces.IUsedRepairCodeListener
import eci.technician.models.data.UsedRepairCode

class RepairCodesAdapter(list: MutableList<UsedRepairCode>, val listener: IUsedRepairCodeListener) :
    RecyclerView.Adapter<RepairCodesAdapter.UsedRepairCodesViewHolder>() {

    private var repairCodesList: MutableList<UsedRepairCode> = mutableListOf()

    init {
        repairCodesList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsedRepairCodesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = UsedRepairCodeBinding.inflate(inflater, parent, false)
        return UsedRepairCodesViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: UsedRepairCodesViewHolder, position: Int) {
        val item: UsedRepairCode = repairCodesList[position]
        holder.txtName.text = item.repairCodeName ?: ""
        holder.txtDescription.text = item.description ?: ""
        holder.btnRemove.setOnClickListener {
            listener.onUsedRepairCodePressed(item)
        }
    }

    override fun getItemCount(): Int {
        return repairCodesList.size
    }

    class UsedRepairCodesViewHolder(itemView: UsedRepairCodeBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val txtName: TextView = itemView.txtName
        val txtDescription: TextView = itemView.txtDescription
        val btnRemove: ImageView = itemView.btnRemove
    }
}