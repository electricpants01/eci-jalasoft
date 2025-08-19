package eci.technician.activities.fieldTransfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.TransferRequestItemBinding
import eci.technician.helpers.DateTimeHelper
import eci.technician.helpers.DecimalsHelper
import eci.technician.models.field_transfer.PartRequestTransfer

class FieldTransferItemsAdapter(val listener: FieldTransferRequestListener) :
    ListAdapter<PartRequestTransfer, FieldTransferItemsAdapter.FieldTransferViewHolder>(
        FIELD_TRANSFER_COMPARATOR
    ) {


    class FieldTransferViewHolder(val binding: TransferRequestItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(parent: ViewGroup): FieldTransferViewHolder {
                val binding = TransferRequestItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FieldTransferViewHolder(binding)
            }
        }
    }

    companion object {
        private val FIELD_TRANSFER_COMPARATOR =
            object : DiffUtil.ItemCallback<PartRequestTransfer>() {
                override fun areItemsTheSame(
                    oldItem: PartRequestTransfer,
                    newItem: PartRequestTransfer
                ): Boolean {
                    return oldItem.toID == newItem.toID
                }

                override fun areContentsTheSame(
                    oldItem: PartRequestTransfer,
                    newItem: PartRequestTransfer
                ): Boolean {
                    return oldItem.sourceTechnician == newItem.sourceTechnician &&
                            oldItem.destinationTechnician == newItem.destinationTechnician &&
                            oldItem.itemID == newItem.itemID &&
                            oldItem.quantity == newItem.quantity

                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldTransferViewHolder {
        return FieldTransferViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: FieldTransferViewHolder, position: Int) {
        val item = getItem(position)
        if (item.myRequest) {
            fillWithMyData(holder, item)
        } else {
            fillWithRequestData(holder, item)
        }
        val partItem = item ?: return
        holder.binding.txtPart.text = partItem.item ?: ""
        holder.binding.txtPartDescription.text = partItem.description ?: ""
        holder.binding.txtQuantity.text =
            DecimalsHelper.getValueFromDecimal(partItem.quantity ?: 0.0)
        val date = DateTimeHelper.getDateFromString(partItem.createDateString ?: "")
        holder.binding.txtCreatedDate.text =
            if (date == null) "" else DateTimeHelper.formatTimeDate(
                date,
                holder.binding.root.context
            )
        holder.binding.btnAccept.setOnClickListener { listener.onAcceptClick(partItem) }
        holder.binding.btnReject.setOnClickListener { listener.onRejectClick(partItem) }
        holder.binding.btnCancel.setOnClickListener { listener.onCancelClick(partItem) }
    }

    private fun fillWithRequestData(
        holder: FieldTransferViewHolder,
        item: PartRequestTransfer?
    ) {
        val partItem = item ?: return
        holder.binding.layActions.visibility = View.VISIBLE
        holder.binding.layMyActions.visibility = View.GONE
        holder.binding.txtFromTo.text = holder.binding.root.context.getString(R.string.from)
        holder.binding.txtTechnicianName.text = String.format(
            "%s (%s)",
            partItem.destinationTechnician ?: "",
            partItem.destinationTechnicianName
        )

    }

    private fun fillWithMyData(
        holder: FieldTransferViewHolder,
        item: PartRequestTransfer?
    ) {
        val partItem = item ?: return
        holder.binding.layActions.visibility = View.GONE
        holder.binding.layMyActions.visibility = View.VISIBLE
        holder.binding.txtFromTo.text = holder.binding.root.context.getString(R.string.to)
        holder.binding.txtTechnicianName.text = String.format(
            "%s (%s)",
            partItem.sourceTechnician ?: "",
            partItem.sourceTechnicianName
        )

    }
}