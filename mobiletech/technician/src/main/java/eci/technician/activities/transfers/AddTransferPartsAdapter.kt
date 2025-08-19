package eci.technician.activities.transfers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.TechnicianwarehousepartItemBinding
import eci.technician.helpers.DecimalsHelper
import eci.technician.models.transfers.Part
import eci.technician.models.transfers.Warehouse

class AddTransferPartsAdapter(parts: List<Part>,var listener: ITransferPartClickedInterface) : RecyclerView.Adapter<AddTransferPartsAdapter.ViewHolder>() {
    private var partsList = parts
    lateinit var context: Context
    private var originalList: List<Part> = parts

    init{
        partsList = partsList.sortedBy { part -> part.item }
        originalList = partsList
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        val viewHolder = TechnicianwarehousepartItemBinding.inflate(inflater, parent, false)
        return ViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Part = partsList[position]
        holder.txtItem.text = item.item
        holder.txtDescription.text = item.description
        val qtt = DecimalsHelper.getValueFromDecimal(item.updatedAvailableQty)
        holder.txtSummary.text =  context.resources.getString(R.string.transfer_part_data,qtt)
        holder.txtSummary.visibility = View.GONE // enable when availability is correct
        holder.root.setOnClickListener{
            listener.onTapPart(item)
        }
    }
    fun filterListByQuery(query: String) {
        partsList = originalList.filter { part ->
            part.item.contains(
                query,
                ignoreCase = true
            ) || part.description.contains(query, ignoreCase = true)

        }
        notifyDataSetChanged()
        listener.onFilteredAction()
    }
    fun resetList() {
        partsList = originalList
        listener.onFilteredAction()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return partsList.size
    }

    fun setParts(parts: List<Part>) {
        partsList = parts
        notifyDataSetChanged()
    }

    inner class ViewHolder(holder: TechnicianwarehousepartItemBinding) : RecyclerView.ViewHolder(holder.root) {
        var txtItem = holder.txtItem
        var txtDescription = holder.txtDescription
        var txtSummary = holder.txtSummary
        var root = holder.root
    }

}