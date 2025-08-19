package eci.technician.activities.transfers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.TechnicianwarehousepartItemBinding
import eci.technician.databinding.TwoItemLayoutBinding
import eci.technician.helpers.DecimalsHelper
import eci.technician.models.transfers.Bin
import eci.technician.models.transfers.Warehouse


class BinSearchAdapter(
    binList: List<Bin>,
    val listener: IBinSearchListener
) : RecyclerView.Adapter<BinSearchAdapter.SearchBinViewHolder>() {
    private var filteredBinList: List<Bin> = binList
    private var originalList: List<Bin> = binList
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchBinViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = TechnicianwarehousepartItemBinding.inflate(inflater, parent, false)
        context = parent.context
        return SearchBinViewHolder(viewHolder)
    }

    fun filterListByQuery(query: String) {
        filteredBinList = originalList.filter { bin ->
            bin.bin?.contains(query, ignoreCase = true) ?: false ||
                    bin.binDescription?.contains(query, ignoreCase = true) ?: false ||
                    bin.description?.contains(query, ignoreCase = true) ?: false ||
                    bin.serialNumber?.contains(query, ignoreCase = true) ?: false
        }
        notifyDataSetChanged()
        listener.onFilteredAction()
    }

    fun resetList() {
        filteredBinList = originalList
        listener.onFilteredAction()
        notifyDataSetChanged()
    }

    interface IBinSearchListener {
        fun onBinSearchPressed(item: Bin);
        fun onFilteredAction();
    }

    override fun getItemCount(): Int {
        return filteredBinList.size;
    }

    class SearchBinViewHolder(holder: TechnicianwarehousepartItemBinding) :
        RecyclerView.ViewHolder(holder.root) {
        var txtItem = holder.txtItem
        var txtDescription = holder.txtDescription
        var txtSummary = holder.txtSummary
        var txtSerial = holder.txtSerial
        var root = holder.root
    }

    override fun onBindViewHolder(
        holder: SearchBinViewHolder,
        position: Int
    ) {
        val bin = filteredBinList[position]
        holder.txtItem.text = bin.bin ?: ""
        holder.txtDescription.text = bin.binDescription ?: bin.description?: ""
        val qtt = DecimalsHelper.getValueFromDecimal(bin.updatedBinQty)
        holder.txtSummary.text =
            String.format(context.resources.getString(R.string.transfer_part_data), qtt)
        if(bin.binDescription == null){
            holder.txtSummary.visibility = View.GONE
        }
        if (holder.txtDescription.text.isNullOrBlank()) {
            holder.txtDescription.visibility = View.GONE
            if (bin.binAvailableQty == 0.0) {
                holder.txtSummary.visibility = View.GONE
            }
        }
        if (!bin.serialNumber.isNullOrBlank()) {
            holder.txtSerial.text =
                context.resources.getString(R.string.serial_number_transfer) + ": " + bin.serialNumber
            holder.txtSerial.visibility = View.VISIBLE
        }
        holder.root.setOnClickListener {
            listener.onBinSearchPressed(bin)
        }
    }
}