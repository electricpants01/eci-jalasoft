package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.ContainerTwoItemLayoutSelectableBinding
import eci.technician.interfaces.ICallTypesGroupList
import eci.technician.models.filters.GroupCallType

class CallTypesAdapterFilter(private val callTypesList: MutableList<GroupCallType>, private val listener: ICallTypesGroupList) : RecyclerView.Adapter<CallTypesAdapterFilter.CallTypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerTwoItemLayoutSelectableBinding.inflate(inflater, parent, false)
        return CallTypeViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CallTypeViewHolder, position: Int) {
        val item = callTypesList[position]
        holder.mainText.text = item.callTypeCode
        holder.secondaryText.text = item.callTypeDescription
        holder.checkImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.container.setOnClickListener {
            listener.onTapCallType(item)
        }
    }

    override fun getItemCount(): Int {
        return callTypesList.size
    }

    class CallTypeViewHolder(v: ContainerTwoItemLayoutSelectableBinding) : RecyclerView.ViewHolder(v.root) {
        val binding: ContainerTwoItemLayoutSelectableBinding = v
        val container: LinearLayout = binding.rowContainer
        val mainText: TextView = binding.mainText
        val secondaryText: TextView = binding.secondaryText
        val checkImage: ImageView = binding.checkedImage
    }
}