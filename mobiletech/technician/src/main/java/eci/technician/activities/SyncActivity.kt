package eci.technician.activities

import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.UnSyncNoteAdapter
import eci.technician.adapters.UnSyncItemsAdapter
import eci.technician.adapters.UnsyncAttachmentAdapter
import eci.technician.databinding.ActivitySyncBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.OnDeleteItemListener
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest
import eci.technician.models.order.IncompleteRequests
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.tools.Constants
import eci.technician.viewmodels.SyncViewModel
import eci.technician.workers.OfflineManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class SyncActivity : BaseActivity(), OnDeleteItemListener {

    companion object {
        const val TAG = "SyncActivity"
        const val EXCEPTION = "Exception"
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[SyncViewModel::class.java]
    }

    lateinit var binding: ActivitySyncBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.SYNC_ACTIVITY)
        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val connection = NetworkConnection(baseContext)

        connection.observe(this, { t ->
            t?.let {
                (binding.unSyncListRecyclerView.adapter as UnSyncItemsAdapter).notifyDataSetChanged()
                (binding.unSyncAttachmentRecyclerView.adapter as UnsyncAttachmentAdapter).notifyDataSetChanged()
                if (it) {
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            //Not in use
                        }

                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                            OfflineManager.retryWorker(baseContext)
                            OfflineManager.retryAttachmentWorker(baseContext)
                            OfflineManager.retryNotesWorker(baseContext)
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                }
            }
        })

        setSupportActionBar(binding.toolbar)
        title = getString(R.string.unsynced_data)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.unSyncListRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.unSyncListRecyclerView.setHasFixedSize(true)
        prepareRecycler()

        binding.unSyncAttachmentRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.unSyncAttachmentRecyclerView.setHasFixedSize(true)
        prepareAttachmentRecycler()

        binding.unSyncNotesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.unSyncNotesRecyclerView.setHasFixedSize(true)
        prepareNotesRecycler()


    }

    private fun prepareNotesRecycler() {
        DatabaseRepository.getInstance().incompleteNotesRequest.observe(
            this,
            androidx.lifecycle.Observer {
                viewModel.isNotesEmpty = it != null && it.size == 0
                binding.unSyncNotesRecyclerView.adapter =
                    UnSyncNoteAdapter(it ?: mutableListOf(), this)
                (binding.unSyncNotesRecyclerView.adapter as UnSyncNoteAdapter).notifyDataSetChanged()
                checkVisibilityForViews()
            })
    }

    private fun prepareAttachmentRecycler() {
        DatabaseRepository.getInstance().incompleteAttachmentRequest.observe(this, {
            viewModel.isAttachmentEmpty = it != null && it.size == 0
            binding.unSyncAttachmentRecyclerView.adapter = UnsyncAttachmentAdapter(
                it
                    ?: mutableListOf(), this
            )
            (binding.unSyncAttachmentRecyclerView.adapter as UnsyncAttachmentAdapter).notifyDataSetChanged()
            checkVisibilityForViews()
        })
    }

    private fun checkVisibilityForViews() {
        binding.unSyncServiceCallDataLinearLayout.visibility =
            if (viewModel.isServiceCallListEmpty) View.GONE else View.VISIBLE
        binding.unSyncAttachmentDataLinearLayout.visibility =
            if (viewModel.isAttachmentEmpty) View.GONE else View.VISIBLE
        binding.unSyncNotesLinearLayout.visibility =
            if (viewModel.isNotesEmpty) View.GONE else View.VISIBLE
        binding.emptyLinearLayout.visibility =
            if (viewModel.isAttachmentEmpty &&
                viewModel.isServiceCallListEmpty &&
                viewModel.isNotesEmpty
            ) View.VISIBLE else View.GONE
    }

    private fun prepareRecycler() {
        DatabaseRepository.getInstance().incompleteRequest.observe(this, {
            viewModel.isServiceCallListEmpty = it != null && it.size == 0
            binding.unSyncListRecyclerView.adapter = UnSyncItemsAdapter(it ?: mutableListOf(), this)
            (binding.unSyncListRecyclerView.adapter as UnSyncItemsAdapter).notifyDataSetChanged()
            checkVisibilityForViews()
            viewModel.refreshSCList(it.size)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sync, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.sync_now -> {
                if (AppAuth.getInstance().isConnected) {
                    syncNow()
                } else {
                    showUnavailableWhenOfflineMessage()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun syncNow() {
        try {
            GlobalScope.launch(Dispatchers.IO) {
                syncDataWithServerEvery()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun syncDataWithServerEvery() {
        try {
            OfflineManager.retryWorker(baseContext)
            OfflineManager.retryAttachmentWorker(baseContext)
            OfflineManager.retryNotesWorker(baseContext)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    override fun onDeletedAction(incompleteRequests: MutableList<IncompleteRequests>) {
        showDeleteWarning(incompleteRequests) { _: DialogInterface, _: Int ->
            viewModel.removeSelectedActions(
                incompleteRequests.first().callNumberCode ?: "",
                this
            )
        }
    }

    override fun onDeletedAttachment(incompleteRequest: AttachmentIncompleteRequest) {
        showDeleteWarningAttachments(incompleteRequest) { _: DialogInterface, _: Int ->
            viewModel.removeSelectedAttachments(
                incompleteRequest.callNumberId,
                incompleteRequest.dateAdded,
                incompleteRequest.fileName
            )
            OfflineManager.retryAttachmentWorker(this)
        }
    }

    override fun onDeletedNote(customNoteId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val noteIncompleteRequest =
                IncompleteRequestsRepository.getIncompleteNoteCopy(customNoteId)
            withContext(Dispatchers.Main) {
                noteIncompleteRequest?.let {
                    showDeleteNoteWarning(it) { _: DialogInterface, _: Int ->
                        viewModel.removeSelectedNote(customNoteId)
                    }
                }
            }
        }
    }

    private fun showDeleteNoteWarning(
        noteIncompleteRequest: NoteIncompleteRequest,
        listener: DialogInterface.OnClickListener
    ) {
        val message = getString(R.string.the_following_note_will_be_deleted)
        val items = noteIncompleteRequest.note ?: ""
        showCustomDeleteDialog(
            message,
            items,
            getString(R.string.delete),
            getString(R.string.cancel),
            listener
        )
    }

    private fun showDeleteWarning(
        incompleteRequests: MutableList<IncompleteRequests>,
        listener: DialogInterface.OnClickListener
    ) {
        val message: String = baseContext.getString(R.string.remove_actions_warning)
        var items = ""
        for ((index, item) in incompleteRequests.withIndex()) {
            items += getActionString(item)
            if (index != incompleteRequests.size - 1) {
                items += "\n"
            }
        }
        showCustomDeleteDialog(
            message,
            items,
            getString(R.string.delete),
            getString(R.string.cancel),
            listener
        )
    }

    private fun showDeleteWarningAttachments(
        incompleteRequest: AttachmentIncompleteRequest,
        listener: DialogInterface.OnClickListener
    ) {
        val message: String = baseContext.getString(R.string.remove_attachments_warning)
        val items = (getAttachmentString(incompleteRequest))
        showCustomDeleteDialog(
            message,
            items,
            getString(R.string.delete),
            getString(R.string.cancel),
            listener
        )
    }

    private fun getAttachmentString(item: AttachmentIncompleteRequest): String {
        return baseContext.getString(
            R.string.un_sync_attachment_title,
            item.callNumber,
            item.fileName
        )
    }

    private fun getActionString(item: IncompleteRequests): String {
        val simpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        var actionString = ""
        when (item.requestType) {
            Constants.STRING_DISPATCH_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.dispatch),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STRING_ARRIVE_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.arrive),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STRING_UNDISPATCH_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_undispatch_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.undispatch)
                )
            }
            Constants.STRING_ON_HOLD_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.hold),
                    simpleDateFormat.format(item.dateAdded)
                )
            }

            Constants.STRING_DEPART_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.complete),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STRING_INCOMPLETE_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.incomplete_title),
                    simpleDateFormat.format(item.dateAdded)
                )
            }

            Constants.STATUS_BRAKE_IN -> {
                actionString = baseContext.getString(
                    R.string.un_sync_action_item_title,
                    baseContext.resources.getString(R.string.brake_in),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STATUS_BRAKE_OUT -> {
                actionString = baseContext.getString(
                    R.string.un_sync_action_item_title,
                    baseContext.resources.getString(R.string.brake_out),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STATUS_LUNCH_IN -> {
                actionString = baseContext.getString(
                    R.string.un_sync_action_item_title,
                    baseContext.resources.getString(R.string.lunch_in),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STATUS_LUNCH_OUT -> {
                actionString = baseContext.getString(
                    R.string.un_sync_action_item_title,
                    baseContext.resources.getString(R.string.lunch_out),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STRING_UPDATE_ITEMS_DETAILS -> {
                when (item.itemType) {
                    0 -> {
                        actionString = baseContext.getString(
                            R.string.un_sync_detail_title,
                            item.callNumberCode,
                            baseContext.resources.getString(R.string.update_ip_address)
                        )
                    }

                    1 -> {
                        actionString = baseContext.getString(
                            R.string.un_sync_detail_title,
                            item.callNumberCode,
                            baseContext.resources.getString(R.string.update_mac_address)
                        )
                    }

                    2 -> {
                        actionString = baseContext.getString(
                            R.string.un_sync_detail_title,
                            item.callNumberCode,
                            baseContext.resources.getString(R.string.update_location_remarks)
                        )
                    }
                }
            }
            Constants.STRING_SCHEDULE_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.schedule),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
            Constants.STRING_HOLD_RELEASE_CALL -> {
                actionString = baseContext.getString(
                    R.string.un_sync_item_title,
                    item.callNumberCode,
                    baseContext.resources.getString(R.string.release),
                    simpleDateFormat.format(item.dateAdded)
                )
            }
        }

        if (item.requestType == "UpdateLabor" && item.assistActionType == Constants.UPDATE_LABOR_STATUS.DISPATCH.value) {
            actionString = baseContext.getString(
                R.string.un_sync_item_title,
                item.callNumberCode,
                baseContext.resources.getString(R.string.dispatch),
                simpleDateFormat.format(item.dateAdded)
            )
        }
        if (item.requestType == "UpdateLabor" && item.assistActionType == Constants.UPDATE_LABOR_STATUS.ARRIVE.value) {
            actionString = baseContext.getString(
                R.string.un_sync_item_title,
                item.callNumberCode,
                baseContext.resources.getString(R.string.arrive),
                simpleDateFormat.format(item.dateAdded)
            )
        }
        return actionString
    }

    private fun showCustomDeleteDialog(
        bodyMessage: String,
        secondaryBodyMessage: String,
        positiveButtonMessage: String,
        negativeButtonMessage: String,
        listener: DialogInterface.OnClickListener
    ) {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.remove_action_dialog, null)
        builder.setView(dialogView)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        titleText.text = getString(R.string.warning)

        val bodyText = dialogView.findViewById<TextView>(R.id.dialogBody)
        bodyText.text = bodyMessage

        val actionsText = dialogView.findViewById<TextView>(R.id.dialogAction)
        actionsText.text = secondaryBodyMessage

        val confirmationText = dialogView.findViewById<TextView>(R.id.dialogBody2)
        confirmationText.text = getString(R.string.confirmation_dialog)

        builder.setPositiveButton(positiveButtonMessage, listener)
        builder.setNegativeButton(negativeButtonMessage, null)

        val dialog = builder.create()
        dialog.window?.let { window ->
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        dialog.show()
    }
}