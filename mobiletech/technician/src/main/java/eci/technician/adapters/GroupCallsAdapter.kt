package eci.technician.adapters

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import eci.technician.adapters.GroupCallsAdapter.ViewHolder
import eci.technician.databinding.GroupCallItemBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.CallPriorityHelper
import eci.technician.helpers.FilterHelper
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.ServiceOrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupCallsAdapter(
    serviceOrders: MutableList<GroupCallServiceOrder>,
    private val listener: GroupCallListener,
    val scope: CoroutineScope
) : RecyclerView.Adapter<ViewHolder>() {

    private var serviceOrders: MutableList<GroupCallServiceOrder> = mutableListOf()
    private var filteredServiceOrders: MutableList<GroupCallServiceOrder> = mutableListOf()
    private var query: String = ""
    private var reassinedCalls = mutableListOf<Int>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = GroupCallItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredServiceOrders[position]
        holder.binding?.item = item
        scope.launch {
            withContext(Dispatchers.Main) {
                val callPriority = item.callPriority?.let {
                    ServiceOrderRepository.getServiceOrderPriorityById(it)
                }
                callPriority?.color?.let {
                    var priorityColor = CallPriorityHelper.parseColor(it)
                    if(CallPriorityHelper.shouldDisplayPriorityFlag((priorityColor))) {
                        holder.binding?.priorityFlag?.setColorFilter(priorityColor)
                        holder.binding?.priorityFlag?.visibility = View.VISIBLE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            holder.binding?.priorityFlag?.tooltipText = callPriority.priorityName
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return filteredServiceOrders.size
    }

    fun getItemPositionById(callNumber_Code: String): Int {
        val selectedServiceCall = serviceOrders.find { it.callNumber_Code == callNumber_Code }
        return serviceOrders.indexOf(selectedServiceCall)
    }

    fun getItemPositionByIdFiltered(callNumber_Code: String): Int {
        val selectedServiceCall =
            filteredServiceOrders.find { it.callNumber_Code == callNumber_Code }
        return filteredServiceOrders.indexOf(selectedServiceCall)
    }

    fun setQuery(query: String) {
        this.query = query.trim()
        if (query.isBlank()) {
            this.filteredServiceOrders = serviceOrders.toMutableList()
        } else {
            this.filteredServiceOrders =
                FilterHelper.filterGroupServiceOrderByQueryText(query, serviceOrders)
        }
        notifyDataSetChanged()
    }

    fun getServiceCall(position: Int): GroupCallServiceOrder {
        return this.serviceOrders[position]
    }

    fun deleteFromList(serviceOrder: GroupCallServiceOrder) {
        this.filteredServiceOrders.remove(serviceOrder)
    }

    fun deleteFromLists(serviceOrder: GroupCallServiceOrder) {
        this.filteredServiceOrders.remove(serviceOrder)
        this.serviceOrders.remove(serviceOrder)
    }


    fun updateFromList(serviceOrder: GroupCallServiceOrder) {
        val position = getItemPositionById(serviceOrder.callNumber_Code)
        this.serviceOrders[position] = serviceOrder
        notifyDataSetChanged()
    }

    fun updateFromFilteredList(serviceOrder: GroupCallServiceOrder) {
        val position = getItemPositionByIdFiltered(serviceOrder.callNumber_Code)
        this.filteredServiceOrders[position] = serviceOrder
        notifyDataSetChanged()
    }

    fun addReasignedCalls(callNumber_ID: Int) {
        this.reassinedCalls.add(callNumber_ID)
    }

    fun setServiceOrderList(groupServiceOrderList: MutableList<GroupCallServiceOrder>) {
        this.serviceOrders = groupServiceOrderList
        this.filteredServiceOrders = groupServiceOrderList

    }

    interface GroupCallListener {
        fun onContactPhoneClick(serviceOrder: GroupCallServiceOrder)
        fun onContactEmailClick(serviceOrder: GroupCallServiceOrder)
        fun onReassignServiceCall(serviceOrder: GroupCallServiceOrder)
        fun copyContactPhone(serviceOrder: GroupCallServiceOrder)
        fun copyContactEmail(serviceOrder: GroupCallServiceOrder)
        fun onDetailedGroupClick(serviceOrder: GroupCallServiceOrder)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: GroupCallItemBinding? = DataBindingUtil.bind(itemView)

        init {
            binding?.let { binding ->

                binding.groupCallItemCardView.setOnClickListener {
                    listener.onDetailedGroupClick(filteredServiceOrders[adapterPosition])
                }
                if (AppAuth.getInstance().technicianUser.isAllowReassignment) {
                    binding.btnReassignMyself.setOnClickListener {
                        listener.onReassignServiceCall(filteredServiceOrders[adapterPosition])
                    }
                }

            }
        }
    }

    init {
        this.serviceOrders = serviceOrders.toMutableList()
        this.filteredServiceOrders = serviceOrders.toMutableList()
    }
}