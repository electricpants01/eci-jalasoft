package eci.technician.activities

import android.app.Dialog
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.ShiftsDetailsAdapter
import eci.technician.databinding.ActivityShiftDetailsBinding
import eci.technician.helpers.AppAuth
import eci.technician.models.time_cards.Shift
import eci.technician.models.time_cards.ShiftDetails
import eci.technician.repository.DatabaseRepository
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import eci.technician.viewmodels.TimeCardsFragmentViewModel
import io.realm.Realm
import io.realm.RealmResults

class ShiftDetailsActivity : BaseActivity() {

    companion object {
        const val SHIFT_ID_KEY = "shiftIdKey"
        const val SHIFT_TITLE_KEY = "shiftTitleKey"
    }

    lateinit var binding: ActivityShiftDetailsBinding
    private val shiftsDetailsAdapter = ShiftsDetailsAdapter(ArrayList())
    private val viewModel: TimeCardsFragmentViewModel by viewModels()
    lateinit var realm: Realm
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shift_details)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val shiftId = intent.getStringExtra(SHIFT_ID_KEY)
        val shiftTitle = intent.getStringExtra(SHIFT_TITLE_KEY) ?: ""
        val shift = DatabaseRepository.getInstance().getShiftById(shiftId)
        if (shiftId == null) {
            finish()
        } else {
            initRecyclerView()
            if (!AppAuth.getInstance().isConnected) {
                val shiftDetails = DatabaseRepository.getInstance().getShiftDetailsById(shiftId)
                if (!shiftDetails.isEmpty()) {
                    binding.txtCaption.text = shift.formattedTotalHours
                    fillOfflineAdapter(shiftDetails)
                } else {
                    showOfflineMessage()
                }
            } else {
                getData(shift)
            }
        }

        title = shiftTitle

        observeNetworkError()
        observeShiftDetailSuccess()

    }

    private fun observeShiftDetailSuccess() {
        viewModel.successShiftDetails.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { shiftDetails ->
                fillAdapter(shiftDetails)
            }
        }
    }

    private fun observeNetworkError() {
        viewModel.networkError.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { pair ->
                showNetworkErrorDialog(pair, this, supportFragmentManager)
            }
        }
    }

    private fun getData(shift: Shift) {
        binding.txtCaption.text = shift.formattedTotalHours
        val shouldDelete = !shift.isShiftClosed
        shift.shiftId?.let { viewModel.fetchShiftDetailByShiftId(it, shouldDelete) }
    }

    private fun fillAdapter(shiftDetails: List<ShiftDetails>) {
        val groupedShifts: MutableList<ShiftDetails> = mutableListOf()
        realm.executeTransaction {
            var i = shiftDetails.size - 1
            while (i >= 0) {
                val sd = shiftDetails[i]
                if (i - 1 >= 0 && (shiftDetails[i - 1].action == Constants.STATUS_BRAKE_OUT || shiftDetails[i - 1].action == Constants.STATUS_SIGNED_OUT || shiftDetails[i - 1].action == Constants.STATUS_LUNCH_OUT)) {
                    sd.action?.let { sd.closeAction = it }
                    sd.closeDate = sd.date
                    sd.action = shiftDetails[i - 1].action
                    sd.date = shiftDetails[i - 1].date
                    i--
                }
                groupedShifts.add(0, sd)
                i--
            }
            shiftsDetailsAdapter.setShiftDetails(groupedShifts)
        }
    }

    private fun fillOfflineAdapter(shiftDetails: RealmResults<ShiftDetails>) {
        val filteredList = shiftDetails.distinctBy { it.date }
        val groupedShifts: MutableList<ShiftDetails> = mutableListOf()
        val shiftDetailsCopy = realm.copyFromRealm(filteredList)
        realm.executeTransaction {
            var i = shiftDetailsCopy.size - 1
            while (i >= 0) {
                val sd = shiftDetailsCopy[i]
                if (i - 1 >= 0 && (shiftDetailsCopy[i - 1].action == Constants.STATUS_BRAKE_OUT || shiftDetailsCopy[i - 1].action == Constants.STATUS_SIGNED_OUT || shiftDetailsCopy[i - 1].action == Constants.STATUS_LUNCH_OUT)) {
                    sd.action?.let { sd.closeAction = it }
                    sd.closeDate = sd.date
                    sd.action = shiftDetailsCopy[i - 1].action
                    sd.date = shiftDetailsCopy[i - 1].date
                    i--
                }
                groupedShifts.add(0, sd)
                i--
            }
            shiftsDetailsAdapter.setShiftDetails(groupedShifts)
        }
    }

    private fun initRecyclerView() {
        binding.recShiftDetails.layoutManager = SafeLinearLayoutManager(this)
        binding.recShiftDetails.adapter = shiftsDetailsAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showOfflineMessage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("")
            .setMessage(R.string.offline_warning)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
        val dialog: Dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}