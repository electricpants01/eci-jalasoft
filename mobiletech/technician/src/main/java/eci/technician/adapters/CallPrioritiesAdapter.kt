package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding
import eci.technician.interfaces.ICallPrioritiesList
import eci.technician.models.filters.CallPriorityFilter

class CallPrioritiesAdapter(private val callPrioritiesList:MutableList<CallPriorityFilter>, val listener:ICallPrioritiesList):RecyclerView.Adapter<CallPrioritiesAdapter.CallPrioritiesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallPrioritiesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = ContainerOneItemLayoutSelectableBinding.inflate(inflater,parent, false)
        return CallPrioritiesViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CallPrioritiesViewHolder, position: Int) {
        val item = callPrioritiesList[position]
        holder.mainTextView.text = item.priorityName
        holder.checkImage.visibility = if (item.isChecked) View.VISIBLE else View.GONE
        holder.container.setOnClickListener {
            listener.onTapPriority(item)
        }
    }

    override fun getItemCount(): Int {
        return callPrioritiesList.size
    }

    class CallPrioritiesViewHolder(v:ContainerOneItemLayoutSelectableBinding):RecyclerView.ViewHolder(v.root){
        val binding: ContainerOneItemLayoutSelectableBinding = v
        val mainTextView:TextView = binding.mainText
        val container: LinearLayout = binding.rowContainer
        val checkImage:ImageView = binding.checkedImage
    }
}