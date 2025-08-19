package eci.technician.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.ApiErrorHelper
import eci.technician.interfaces.OnDeleteItemListener
import eci.technician.models.order.IncompleteRequests
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.container_unsync_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class UnSyncItemsAdapter(val unSyncList: MutableList<IncompleteRequests>, val listener: OnDeleteItemListener) : RecyclerView.Adapter<UnSyncItemsAdapter.UnSyncItemViewHolder>() {

    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnSyncItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.container_unsync_item, parent, false)
        return UnSyncItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return unSyncList.size
    }

    override fun onBindViewHolder(holder: UnSyncItemViewHolder, position: Int) {
        val item = unSyncList[position]
        val pattern = "hh:mm a"
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

        when (item.requestType) {
            Constants.STRING_DISPATCH_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.dispatch), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STRING_ARRIVE_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.arrive), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STRING_UNDISPATCH_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_undispatch_title, item.callNumberCode, context.resources.getString(R.string.undispatch))
            }
            Constants.STRING_ON_HOLD_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.hold), simpleDateFormat.format(item.dateAdded))
            }

            Constants.STRING_DEPART_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.complete), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STRING_INCOMPLETE_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.incomplete_title), simpleDateFormat.format(item.dateAdded))
            }

            Constants.STATUS_BRAKE_IN -> {
                holder.title.text = context.getString(R.string.un_sync_action_item_title, context.resources.getString(R.string.brake_in), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STATUS_BRAKE_OUT -> {
                holder.title.text = context.getString(R.string.un_sync_action_item_title, context.resources.getString(R.string.brake_out), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STATUS_LUNCH_IN -> {
                holder.title.text = context.getString(R.string.un_sync_action_item_title, context.resources.getString(R.string.lunch_in), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STATUS_LUNCH_OUT -> {
                holder.title.text = context.getString(R.string.un_sync_action_item_title, context.resources.getString(R.string.lunch_out), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STRING_UPDATE_ITEMS_DETAILS -> {
                when (item.itemType) {
                    0 -> {
                        holder.title.text = context.getString(R.string.un_sync_detail_title, item.callNumberCode, context.resources.getString(R.string.update_ip_address))
                    }

                    1 -> {
                        holder.title.text = context.getString(R.string.un_sync_detail_title, item.callNumberCode, context.resources.getString(R.string.update_mac_address))
                    }

                    2 -> {
                        holder.title.text = context.getString(R.string.un_sync_detail_title, item.callNumberCode, context.resources.getString(R.string.update_location_remarks))
                    }
                }
            }
            Constants.STRING_SCHEDULE_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.schedule), simpleDateFormat.format(item.dateAdded))
            }
            Constants.STRING_HOLD_RELEASE_CALL -> {
                holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.release), simpleDateFormat.format(item.dateAdded))
            }
        }

        if (item.requestType == "UpdateLabor" && item.assistActionType == Constants.UPDATE_LABOR_STATUS.DISPATCH.value) {
            holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.dispatch), simpleDateFormat.format(item.dateAdded))
        }
        if (item.requestType == "UpdateLabor" && item.assistActionType == Constants.UPDATE_LABOR_STATUS.ARRIVE.value) {
            holder.title.text = context.getString(R.string.un_sync_item_title, item.callNumberCode, context.resources.getString(R.string.arrive), simpleDateFormat.format(item.dateAdded))
        }

        when (item.status) {
            Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value -> {
                holder.subtitle.text = ""
                holder.subtitle.visibility = View.GONE
                holder.progressBarContainer.visibility = View.GONE
                holder.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value -> {
                holder.subtitle.text = context.getString(R.string.in_progress)
                holder.progressBarContainer.visibility = View.VISIBLE
                holder.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.SUCCESS.value -> {
                holder.subtitle.text = ""
                holder.subtitle.visibility = View.GONE
                holder.progressBarContainer.visibility = View.GONE
                holder.warningContainer.visibility = View.GONE
            }
            Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value -> {
                holder.subtitle.text = ApiErrorHelper.getFormattedError(item.requestErrorCode ?: 0 , item.requestErrors ?: "")
                holder.progressBarContainer.visibility = View.GONE
                holder.warningContainer.visibility = View.VISIBLE
            }
        }

        if(holder.warningContainer.visibility == View.VISIBLE && AppAuth.getInstance().isConnected) {
            holder.delete.visibility = View.VISIBLE
        } else {
            holder.delete.visibility = View.GONE
        }

        holder.delete.setOnClickListener {
            val incompleteRequests = unSyncList.filter { it.callNumberCode == item.callNumberCode }.toMutableList()
            val incompleteRequestsCopy = IncompleteRequestsRepository.getIncompleteRequestsCopy(incompleteRequests)
            listener.onDeletedAction(incompleteRequestsCopy)
        }
    }

    class UnSyncItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.primaryUnSyncTextView
        val subtitle: TextView = v.secondaryUnSyncTextView
        val progressBarContainer: LinearLayout = v.progressBarContainer
        val warningContainer: LinearLayout = v.warningContainer
        val delete: ImageView = v.btnDelete
    }
}