package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.ContainerTwoItemLayoutBinding
import eci.technician.databinding.ContainerTwoItemLayoutSelectableBinding
import eci.technician.interfaces.CallTypesAdapterTapListener
import eci.technician.models.create_call.CallType

class CallTypesAdapter(private val callTypesList: MutableList<CallType>, private val listener: CallTypesAdapterTapListener) : RecyclerView.Adapter<CallTypesAdapter.CallTypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerTwoItemLayoutSelectableBinding.inflate(inflater,parent, false)
        return CallTypeViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CallTypeViewHolder, position: Int) {
        val item = callTypesList[position]
        holder.mainText.text = item.callTypeCode
        holder.secondaryText.text = item.callTypeDescription
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
    }
}