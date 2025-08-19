package eci.technician.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.ContainerThreeItemLayoutSelectableBinding
import eci.technician.databinding.ContainerTwoItemLayoutSelectableBinding
import eci.technician.models.create_call.EquipmentItem

class EquipmentItemAdapter(private val equipmentItemList: MutableList<EquipmentItem>, private val listener:EquipmentItemListener) : RecyclerView.Adapter<EquipmentItemAdapter.EquipmentItemViewHolder>() {

    interface EquipmentItemListener{
        fun onTapEquipmentItem(item: EquipmentItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerThreeItemLayoutSelectableBinding.inflate(inflater, parent, false)
        return EquipmentItemViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: EquipmentItemViewHolder, position: Int) {
        val item = equipmentItemList[position]
        holder.binding.mainText.text = item.equipmentNumberCode
        holder.binding.secondaryText1.text = "${item.modelNumberCode} - ${item.modelDescription}"
        holder.binding.secondaryText2.text = "S/N ${item.serialNumber}"
        holder.binding.rowContainer.setOnClickListener {
            listener.onTapEquipmentItem(item)
        }
    }

    override fun getItemCount(): Int {
        return equipmentItemList.size
    }

    class EquipmentItemViewHolder(v: ContainerThreeItemLayoutSelectableBinding) : RecyclerView.ViewHolder(v.root) {
        val binding: ContainerThreeItemLayoutSelectableBinding = v
    }
}