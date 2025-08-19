package eci.technician.activities.allparts

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.UsedPartItemBinding
import eci.technician.helpers.AppAuth
import eci.technician.models.data.UsedPart
import eci.technician.repository.ServiceOrderRepository
import java.util.*

class AllUsedPartsAdapter(
    private val usedParts: List<UsedPart>,
    val listener: IPartsUpdate,
    val customerWarehouseId: Int,
    val calStatus: ServiceOrderRepository.ServiceOrderStatus,
    val isInHoldProcess: Boolean,
    val isAssist: Boolean
) :
    RecyclerView.Adapter<AllUsedPartsAdapter.AllUsedPartsViewHolder>() {

    inner class AllUsedPartsViewHolder(val binding: UsedPartItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllUsedPartsViewHolder {
        val binding =
            UsedPartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllUsedPartsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AllUsedPartsViewHolder, position: Int) {
        val part = usedParts[position]
        holder.binding.txtItemName.text = part.partName ?: ""
        holder.binding.txtDescription.text = part.partDescription ?: ""
        holder.binding.btnRemove.visibility = partBtnRemoveVisibility(part)
        if (part.serialNumber.isNullOrEmpty()) {
            holder.binding.serialNumber.visibility = View.GONE
        } else {
            holder.binding.serialNumber.text =
                holder.binding.root.context.getString(R.string.serial_number_bin, part.serialNumber)
            holder.binding.serialNumber.visibility = View.VISIBLE
        }
        if (part.deletable && !isInHoldProcess) {
            holder.binding.txtItemName.paintFlags =
                holder.binding.txtItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.binding.txtItemName.paintFlags = 0
        }

        holder.binding.warehouseTextView.text = getWarehouseAndBinName(part)

        when (part.localUsageStatusId) {
            UsedPart.NEEDED_STATUS_CODE -> {
                handleNeededParts(holder, part)
            }
            UsedPart.PENDING_STATUS_CODE -> {
                handlePendingParts(holder, part)
            }
            UsedPart.USED_STATUS_CODE -> {
                handleUsedPart(holder, part)
            }
        }

    }

    private fun getWarehouseAndBinName(part: UsedPart): String {
        val warehouse = part.warehouseName ?: ""
        val binName = if (part.binName.isNullOrEmpty()) "" else " - ${part.binName}"
        return "${warehouse}${binName}"
    }


    private fun isSameWarehouse(currentWarehouseID: Int): Boolean {
        return currentWarehouseID == AppAuth.getInstance().technicianUser.warehouseId
    }

    private fun isCustomerWarehouse(currentWarehouseID: Int): Boolean {
        return currentWarehouseID == customerWarehouseId
    }

    private fun isWarehouseScope(currentWarehouseID: Int): Boolean {
        return isSameWarehouse(currentWarehouseID) || isCustomerWarehouse(currentWarehouseID)
    }

    private fun handleUsedPart(holder: AllUsedPartsAdapter.AllUsedPartsViewHolder, part: UsedPart) {
        if (isWarehouseScope(part.warehouseID) &&
            part.isHasBeenChangedLocally &&
            (calStatus == ServiceOrderRepository.ServiceOrderStatus.ARRIVED)
        ) {
            holder.binding.pendingSwitch.visibility = View.VISIBLE
        } else {
            holder.binding.pendingSwitch.visibility = View.GONE
        }

        holder.binding.warehouseTextView.visibility = View.VISIBLE

        holder.binding.pendingSwitch.isChecked =
            part.localUsageStatusId == 1 && part.isHasBeenChangedLocally
        holder.binding.txtQuantity.text = String.format(
            Locale.getDefault(),
            "%.0f %s %s",
            part.quantity,
            holder.binding.root.context.getString(R.string.used),
            if (part.isSent) "(${holder.binding.root.context.getString(R.string.sent)})" else ""
        )
        holder.binding.pendingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                listener.markAsPending(part.customId)
            }
        }
        holder.binding.btnRemove.setOnClickListener { listener.deleteUsedPart(part.customId) }
    }

    private fun handlePendingParts(
        holder: AllUsedPartsAdapter.AllUsedPartsViewHolder,
        part: UsedPart
    ) {
        if (isWarehouseScope(part.warehouseID) && calStatus == ServiceOrderRepository.ServiceOrderStatus.ARRIVED) {
            holder.binding.pendingSwitch.visibility = View.VISIBLE
        } else {
            holder.binding.pendingSwitch.visibility = View.GONE
        }

        holder.binding.warehouseTextView.visibility = View.VISIBLE

        holder.binding.txtQuantity.text = String.format(
            Locale.getDefault(),
            "%.0f %s %s",
            part.quantity,
            holder.binding.root.context.getString(R.string.pending),
            if (part.isSent) "(${holder.binding.root.context.getString(R.string.sent)})" else ""
        )

        holder.binding.pendingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                listener.markAsUsed(part.customId)
            }
        }
        holder.binding.btnRemove.setOnClickListener { listener.deletePendingPart(part.customId) }

    }

    private fun handleNeededParts(
        holder: AllUsedPartsAdapter.AllUsedPartsViewHolder,
        part: UsedPart
    ) {
        holder.binding.warehouseTextView.visibility = View.GONE
        holder.binding.pendingSwitch.visibility = View.GONE
        holder.binding.txtQuantity.text = String.format(
            Locale.getDefault(),
            "%.0f %s %s",
            part.quantity,
            holder.binding.root.context.getString(R.string.needed),
            if (part.isSent) "(${holder.binding.root.context.getString(R.string.sent)})" else ""
        )
        holder.binding.pendingSwitch.visibility = View.GONE
        holder.binding.btnRemove.setOnClickListener { listener.deleteNeededPart(part.customId) }
        holder.binding.usedPartLayout.setOnLongClickListener {
            listener.onLongPress(part.customId)
            true
        }
    }

    private fun partBtnRemoveVisibility(part: UsedPart): Int {
        return if (part.isSent) {
            if (calStatus == ServiceOrderRepository.ServiceOrderStatus.ON_HOLD || isInHoldProcess) {
                View.GONE
            } else {
                if (isAssist) View.GONE else View.VISIBLE
            }
        } else {
            View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return usedParts.size
    }
}