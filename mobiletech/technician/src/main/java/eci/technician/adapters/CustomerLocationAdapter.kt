package eci.technician.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.ContainerThreeItemLayoutSelectableBinding
import eci.technician.databinding.ContainerTwoItemLayoutSelectableBinding
import eci.technician.models.create_call.CustomerItem


class CustomerLocationAdapter(private val customerItemList: MutableList<CustomerItem>, private val listener: CustomerLocationAdapter.CustomerItemListener) : RecyclerView.Adapter<CustomerLocationAdapter.CustomerItemViewHolder>() {
    interface CustomerItemListener {
        fun onTapCustomerItem(item: CustomerItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerThreeItemLayoutSelectableBinding.inflate(inflater, parent, false)
        return CustomerItemViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CustomerItemViewHolder, position: Int) {
        val item = customerItemList[position]
        holder.binding.mainText.text = item.customerName
        holder.binding.secondaryText1.text = holder.itemView.context.getString(R.string.customerNumberCode, item.customerNumberCode)
        holder.binding.secondaryText2.text = item.getLocationJoined()
        holder.binding.rowContainer.setOnClickListener {
            listener.onTapCustomerItem(item)
        }
    }

    override fun getItemCount(): Int {
        return customerItemList.size
    }

    class CustomerItemViewHolder(v: ContainerThreeItemLayoutSelectableBinding) : RecyclerView.ViewHolder(v.root) {
        val binding: ContainerThreeItemLayoutSelectableBinding = v
    }
}