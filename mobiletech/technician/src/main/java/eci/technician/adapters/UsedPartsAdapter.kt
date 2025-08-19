package eci.technician.adapters

import android.graphics.Paint
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import eci.technician.databinding.UsedPartItemBinding
import eci.technician.helpers.AppAuth
import eci.technician.interfaces.OnTouchedPartListener
import eci.technician.models.data.UsedPart
import eci.technician.repository.DatabaseRepository
import eci.technician.tools.Constants

import io.realm.Realm
import java.lang.Exception
import java.util.*

class UsedPartsAdapter(list: MutableList<UsedPart>, deleteListener: OnTouchedPartListener) : RecyclerView.Adapter<UsedPartsAdapter.UsedPartsViewHolder>() {
    private var usedPartList: MutableList<UsedPart> = list
    private var onTouchedPartListener: OnTouchedPartListener = deleteListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsedPartsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = UsedPartItemBinding.inflate(inflater, parent, false)
        return UsedPartsViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: UsedPartsViewHolder, position: Int) {
        val part: UsedPart = usedPartList[position]
        holder.txtItemName.text = part.partName ?: ""
        holder.textDescription.text = part.partDescription ?: ""
        if (part.warehouseID > 0 && !part.warehouseName.isNullOrEmpty()) {
            holder.warehouseName.text = part.warehouseName ?: ""
            holder.warehouseName.visibility = if (!isSameWarehouse(part.warehouseID)) View.VISIBLE else View.INVISIBLE
        } else {
            holder.warehouseName.visibility = View.GONE
        }
        var usageStatus = "Unknown"
        when (part.usageStatusId) {
            1 -> usageStatus = "used"
            2 -> usageStatus = "needed"
            3 -> usageStatus = "pending"
        }

        holder.txtQuantity.text = String.format(Locale.getDefault(), "%.0f %s %s", part.quantity,
                usageStatus, if (part.isSent) "(sent)" else "")

        if (part.warehouseID == AppAuth.getInstance().technicianUser.warehouseId) {
            if ((part.isSent || part.isHasBeenChangedLocally)
                    &&
                    part.usageStatusId == Constants.USED_PART_USAGE_STATUS.PENDING.value) {
                holder.pendingSwitch.visibility = View.VISIBLE
            } else {
                holder.pendingSwitch.visibility = View.GONE
            }
        } else {
            holder.pendingSwitch.visibility = View.GONE
        }

        holder.pendingSwitch.setOnCheckedChangeListener(null)

        holder.pendingSwitch.isChecked = part.localUsageStatusId == 1 && !part.deletable
        holder.pendingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                val realm = Realm.getDefaultInstance()
                try {
                    realm.executeTransaction {
                        part.localUsageStatusId = Constants.USED_PART_USAGE_STATUS.PENDING.value
                        part.actionType = "update"
                        part.isDeleteWhenRefreshing = false
                        part.isHasBeenChangedLocally = true
                        notifyItemChanged(position)
                    }
                } catch (e: Exception) {
                    Log.d("UsedPartsAdapter", e.toString())
                } finally {
                    realm.close()
                }
            } else {
                setUsedStatus(part, position)
            }
        }


        if (part.deletable) {
            holder.txtItemName.paintFlags = holder.txtItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.txtItemName.paintFlags = 0
        }

        holder.btnRemove.setOnClickListener {
            if (part.isSent) {
                if (!isSameWarehouse(part.warehouseID) && part.deletable) {
                    setPendingStatus(part, position)
                } else {
                    onTouchedPartListener.onDeletedUsedItem(part, position)
                }
            } else {
                val realm = Realm.getDefaultInstance()
                try {
                    realm.executeTransaction {
                        part.deleteFromRealm()
                    }
                } catch (e: Exception) {
                    Log.d("UsedPartsAdapter", e.toString())
                } finally {
                    realm.close()
                }
            }
        }

        if (AppAuth.getInstance().technicianUser.isAllowUnknownItems && !part.isSent && part.usageStatusId == Constants.USED_PART_USAGE_STATUS.NEEDED.value) {
            holder.usedItemLayout.setOnLongClickListener {
                onTouchedPartListener.onLongPressedItem(part)
                false
            }
        }
    }

    override fun getItemCount(): Int {
        return usedPartList.size
    }


    private fun setUsedStatus(part: UsedPart, position: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                part.deletable = false
                part.localUsageStatusId = Constants.USED_PART_USAGE_STATUS.USED.value
                part.actionType = "update"
                part.isHasBeenChangedLocally = true
                part.isDeleteWhenRefreshing = false
                notifyItemChanged(position)
            }
        } catch (e: Exception) {
            Log.d("UsedPartsAdapter", e.toString())
        } finally {
            realm.close()
        }
    }

    private fun setPendingStatus(part: UsedPart, position: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                part.deletable = false
                part.localUsageStatusId = Constants.USED_PART_USAGE_STATUS.PENDING.value
                part.actionType = "update"
                part.isHasBeenChangedLocally = true
                part.isDeleteWhenRefreshing = false
                notifyItemChanged(position)
            }
        } catch (e: Exception) {
            Log.d("UsedPartsAdapter", e.toString())
        } finally {
            realm.close()
        }

    }

    private fun isSameWarehouse(currentWarehouseID: Int): Boolean {
        return currentWarehouseID == AppAuth.getInstance().technicianUser.warehouseId
    }

    class UsedPartsViewHolder(itemView: UsedPartItemBinding) : RecyclerView.ViewHolder(itemView.root) {
        var txtItemName: TextView = itemView.txtItemName
        var txtQuantity: TextView = itemView.txtQuantity
        var warehouseName: TextView = itemView.warehouseTextView
        var textDescription: TextView = itemView.txtDescription
        var btnRemove: Button = itemView.btnRemove
        var pendingSwitch: Switch = itemView.pendingSwitch
        var usedItemLayout: LinearLayout = itemView.usedPartLayout
    }
}