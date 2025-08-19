package eci.technician.adapters;

import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.recyclerview.widget.RecyclerView
import com.google.android.datatransport.runtime.util.PriorityMapping.toInt
import eci.technician.R
import eci.technician.helpers.CallPriorityHelper
import eci.technician.helpers.FilterHelper
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.ServiceOrderRepository
import kotlinx.coroutines.*

class ServiceOrdersAdapter(
    val serviceOrderList: MutableList<ServiceOrder>,
    val listener: ServiceOrderListListener,
    val scope: CoroutineScope
) : RecyclerView.Adapter<ServiceOrdersAdapter.ServiceOrderViewHolder>() {

    private var query: String = ""
    private var filteredServiceOrders: MutableList<ServiceOrder> = mutableListOf()

    interface ServiceOrderListListener {
        fun onServiceOrderTap(serviceOrder: ServiceOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceOrderViewHolder {
        val viewHolder =
            LayoutInflater.from(parent.context).inflate(R.layout.service_order_item, parent, false)
//        val viewHolder = ServiceOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceOrderViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ServiceOrderViewHolder, position: Int) {
        val item = filteredServiceOrders[position]
        holder.customerNameTextView.text = item.customerName ?: ""
        holder.headerTextView.text = item.getHeader()
        holder.callTypeTextView.text = item.callType ?: ""
        holder.callNumberCode.text = item.callNumber_Code ?: ""
        holder.customerAddress.text = item.getCustomerFullAddress()
        holder.makeModel.text = item.getMakeModel()
        scope.launch {
            withContext(Dispatchers.Main) {
                if (ServiceOrderRepository.canShowNeededPartsIndicator(item.callNumber_ID)) {
                    holder.neededPartsIndicator.visibility = View.VISIBLE
                } else {
                    holder.neededPartsIndicator.visibility = View.GONE
                }
                val callPriority = item.callPriority?.let {
                    ServiceOrderRepository.getServiceOrderPriorityById(
                        it
                    )
                }
                callPriority?.color?.let {
                    var priorityColor = CallPriorityHelper.parseColor(it)
                    if(CallPriorityHelper.shouldDisplayPriorityFlag((priorityColor))) {
                        holder.priorityFlag.setColorFilter(priorityColor)
                        holder.priorityFlag.visibility = View.VISIBLE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            holder.priorityFlag?.tooltipText = callPriority.priorityName
                        }
                    } else {
                        holder.priorityFlag.visibility = View.INVISIBLE
                    }
                }
            }
        }

        holder.serviceOrderCard.setOnClickListener {
            listener.onServiceOrderTap(item)
        }
    }

    override fun getItemCount(): Int {
        return filteredServiceOrders.size
    }

    fun setQuery(query: String) {
        this.query = query.trim()
        if (this.query.isEmpty()) {
            this.filteredServiceOrders = serviceOrderList.toMutableList()
        } else {
            this.filteredServiceOrders =
                FilterHelper.filterServiceOrderByQueryText(query, serviceOrderList)
        }
        notifyDataSetChanged()
    }


    class ServiceOrderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val customerNameTextView: TextView = v.findViewById(R.id.customerNameTextView)
        val headerTextView: TextView = v.findViewById(R.id.headerTextView)
        val callTypeTextView: TextView = v.findViewById(R.id.callTypeTextView)
        val callNumberCode: TextView = v.findViewById(R.id.callNumberCodeTextView)
        val customerAddress: TextView = v.findViewById(R.id.txtCustomerAddress)
        val makeModel: TextView = v.findViewById(R.id.makeModelTextView)
        val serviceOrderCard: CardView = v.findViewById(R.id.serviceOrderCard)
        val neededPartsIndicator: FrameLayout = v.findViewById(R.id.neededPartsDot)
        val priorityFlag: ImageView = v.findViewById(R.id.priorityFlag)
    }
}
