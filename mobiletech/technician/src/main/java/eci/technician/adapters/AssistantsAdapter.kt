package eci.technician.adapters;

import android.content.Context
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView;

import eci.technician.R;
import eci.technician.databinding.ContainerTwoItemLayoutBinding
import eci.technician.models.TechnicianItem;
import eci.technician.models.order.ServiceCallLabor;
import eci.technician.tools.ConstantsKotlin

class AssistantsAdapter(private val techList: MutableList<TechnicianItem>, private val serviceCallLaborList: MutableList<ServiceCallLabor>) : RecyclerView.Adapter<AssistantsAdapter.ViewAssistantsViewHolder>() {

    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewAssistantsViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.container_two_item_layout, parent, false)
        return ViewAssistantsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewAssistantsViewHolder, position: Int) {
        val item = serviceCallLaborList[position]
        var techName = ""
        for (technicianItem in techList) {
            if (technicianItem.id == item.technicianId) {
                techName = technicianItem.fullName
            }
        }
        holder.mainText?.text = techName
        val txtStatus = when (item.technicianAssistStatus) {
            ConstantsKotlin.TechnicianServiceCallLaborStatus.DISPATCHED -> context.getString(R.string.callStatusDispatched)
            ConstantsKotlin.TechnicianServiceCallLaborStatus.ARRIVED -> context.getString(R.string.callStatusArrived)
            ConstantsKotlin.TechnicianServiceCallLaborStatus.COMPLETED -> context.getString(R.string.callStatusCompleted)
            ConstantsKotlin.TechnicianServiceCallLaborStatus.PENDING -> context.getString(R.string.callStatusPending)
        }
        holder.secondaryText?.text = txtStatus
    }

    override fun getItemCount(): Int {
        return serviceCallLaborList.size
    }

    class ViewAssistantsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val binding: ContainerTwoItemLayoutBinding? = DataBindingUtil.bind(v)
        val mainText: TextView? = binding?.mainText
        val secondaryText: TextView? = binding?.secondaryText
    }
}