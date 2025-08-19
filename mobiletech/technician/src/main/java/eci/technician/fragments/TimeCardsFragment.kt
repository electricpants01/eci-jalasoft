package eci.technician.fragments

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import eci.technician.BaseActivity
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.activities.SyncActivity
import eci.technician.activities.TimeCardsCalendarActivity
import eci.technician.adapters.ShiftsAdapter
import eci.technician.databinding.FragmentTimeCardsBinding
import eci.technician.dialog.DialogBeforeActionHelper.checkDispatchedCallsBeforeClockOut
import eci.technician.dialog.DialogManager.showClockInDialog
import eci.technician.dialog.DialogManager.showClockOutDialog
import eci.technician.dialog.DialogManager.showSimpleAlertDialog
import eci.technician.helpers.AppAuth
import eci.technician.helpers.AppAuth.AuthStateListener
import eci.technician.helpers.DialogHelperManager.displayOkMessage
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.ErrorHelper.MTErrorListener
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.ServiceTools
import eci.technician.helpers.TechnicianTimeHelper
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.versionManager.CompatibilityManager
import eci.technician.helpers.versionManager.MessageDisplayer.displayMessage
import eci.technician.interfaces.IPayPeriodsListener
import eci.technician.models.TechnicianUser
import eci.technician.models.gps.GPSLocation
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.models.time_cards.Shift
import eci.technician.models.time_cards.ShiftUI
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.service.TechnicianService
import eci.technician.service.TrackService
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import eci.technician.viewmodels.TimeCardsFragmentViewModel
import eci.technician.workers.OfflineManager.retryWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TimeCardsFragment : Fragment(), AuthStateListener, IPayPeriodsListener, MTErrorListener,
    ShiftsAdapter.IShiftListener {


    companion object {
        private const val TAG = "TimeCardsFragment"
        private const val EXCEPTION = "Exception"
        const val SHIFT_ID_KEY = "shiftIdKey"
        const val SHIFT_TITLE_KEY = "shiftTitleKey"
        const val SHIFT_DATE_KEY = "shiftDateKey"
    }

    private lateinit var binding: FragmentTimeCardsBinding
    private var isFabOpen = false
    var connection: NetworkConnection? = null
    private var fabOpen: Animation? = null
    private var fabClose: Animation? = null

    private var rotateForward: Animation? = null
    private var rotateBackward: Animation? = null
    private val shiftsAdapter = ShiftsAdapter(this)


    private val viewModel by lazy {
        ViewModelProvider(this)[TimeCardsFragmentViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ErrorHandler.get().addErrorListener(this)
        connection = context?.let { NetworkConnection(it) }
        connection?.observe(this) { aBoolean ->
            if (aBoolean) {
                object : CountDownTimer(1500, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        // do nothing
                    }

                    override fun onFinish() {
                        AppAuth.getInstance().isConnected = true
                    }
                }.start()
            } else {
                AppAuth.getInstance().isConnected = false
                retryWorker(AppAuth.getInstance().context)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ErrorHandler.get().removeListener(this)
        AppAuth.getInstance().removeUserUpdateListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_time_cards, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)
        AppAuth.getInstance().addUserUpdateListener(this)
        fabOpen = AnimationUtils.loadAnimation(activity, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(activity, R.anim.fab_close)
        rotateForward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        rotateBackward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
        binding.btnAction.setOnClickListener { animateFAB() }
        binding.frmTransparentLayer.setOnClickListener { animateFAB() }
        fabClose?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.layActions.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.layActions.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {
                //do nothing
            }
        })
        setBindingUser()
        clickListeners()
        initRecyclerView()
        observeShiftList()
        viewModel.fetchShiftsList()
    }

    private fun observeShiftList() {
        viewModel.shiftList.observe(viewLifecycleOwner) {
            fillAdapter(it)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerShiftDays.layoutManager = SafeLinearLayoutManager(activity)
        binding.recyclerShiftDays.adapter = shiftsAdapter
    }


    private fun fillAdapter(shifts: List<Shift>) {
        shiftsAdapter.submitList(shifts.map { ShiftUI.mapToShiftUI(it, requireContext()) }
            .sortedByDescending { it.date })
    }

    private fun setBindingUser() {
        binding.technicianUser = AppAuth.getInstance().technicianUser
    }

    private fun onErrorVersion(title: String, message: String) {
        disableButton(isEnabled = true)
        context?.let {
            displayOkMessage(
                title, message,
                getString(R.string.ok),
                it, ::showClockIn
            )
        }
    }

    private fun doClockInOperations() {
        if (AppAuth.getInstance().isConnected) {
            val compatibilityManager = context?.let { it1 -> CompatibilityManager(it1) }
            compatibilityManager?.checkCompatibility(
                lifecycle,
                onSuccess = ::resolveCompatibility,
                onError = ::onErrorVersion,
                onLoading = {
                    disableButton(false)
                })
        } else {
            showOfflineDialogue()
        }
    }

    private fun disableButton(isEnabled: Boolean) {
        binding.btnAction.isEnabled = isEnabled
        binding.btnClockIn.isEnabled = isEnabled
        binding.cardViewClockIn.isEnabled = isEnabled
    }

    private fun clickListeners() {

        // clock in
        binding.btnClockIn.setOnClickListener { doClockInOperations() }
        binding.cardViewClockIn.setOnClickListener { doClockInOperations() }

        //clock out
        binding.btnClockOut.setOnClickListener { showClockOutDialog() }
        binding.cardViewClockOut.setOnClickListener { showClockOutDialog() }

        //lunch in
        binding.btnLunchIn.setOnClickListener { action(Constants.STATUS_LUNCH_IN, -1) }
        binding.cardViewLunchIn.setOnClickListener { action(Constants.STATUS_LUNCH_IN, -1) }

        //lunch out
        binding.btnLunchOut.setOnClickListener { action(Constants.STATUS_LUNCH_OUT, -1) }
        binding.cardViewLunchOut.setOnClickListener { action(Constants.STATUS_LUNCH_OUT, -1) }

        //brake In
        binding.btnBrakeIn.setOnClickListener { action(Constants.STATUS_BRAKE_IN, -1) }
        binding.cardViewBreakIn.setOnClickListener { action(Constants.STATUS_BRAKE_IN, -1) }

        //brake out
        binding.btnBrakeOut.setOnClickListener { action(Constants.STATUS_BRAKE_OUT, -1) }
        binding.cardViewBreakout.setOnClickListener { action(Constants.STATUS_BRAKE_OUT, -1) }
    }

    private fun resolveCompatibility(compatibilityMessage: String) {
        disableButton(isEnabled = true)
        if (compatibilityMessage != CompatibilityManager.COMPATIBILITY_OK) {
            if (compatibilityMessage == CompatibilityManager.MESSAGE_OLD_MOBILE) displayMessage(
                compatibilityMessage,
                requireContext(),
                this::showClockIn
            )
            if (compatibilityMessage == CompatibilityManager.MESSAGE_OLD_HOST ||
                compatibilityMessage == CompatibilityManager.MESSAGE_UNKNOWN_HOST
            ) displayOkMessage(
                this.getString(R.string.warning),
                compatibilityMessage,
                this.getString(R.string.ok),
                requireContext(),
                this::showClockIn
            )
        } else {
            showClockIn()
        }
    }

    private fun showClockIn() {
        showClockInDialog(
            getString(R.string.clock_in),
            requireActivity().supportFragmentManager,
            { aInteger: Int ->
                try {
                    AppAuth.getInstance().lastOdometer = aInteger
                } catch (e: NumberFormatException) {
                    Log.e(TAG, EXCEPTION, e)
                    Snackbar.make(
                        binding.btnAction,
                        R.string.invalid_odometer_value,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                action(Constants.STATUS_SIGNED_IN, aInteger)
            },
            {
                // TODO IMPLEMENT PROGRESS BAR FOR CLOCKIN
            })
    }

    private fun action(action: String, odometer: Int) {
        if (action == Constants.STATUS_SIGNED_OUT) {
            val progressDialog = ProgressDialog.show(context, "", getString(R.string.syncing))
            context?.let { retryWorker(it) }
            lifecycleScope.launch(Dispatchers.IO) {
                val hasUnSyncData = IncompleteRequestsRepository.checkUnsyncData()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    if (hasUnSyncData) {
                        showFailedSyncDialogue()
                    } else {
                        continueWithAction(action, odometer)
                    }
                }
            }
        } else {
            continueWithAction(action, odometer)
        }
    }

    private fun continueWithAction(action: String, odometer: Int) {
        if (action == Constants.STATUS_SIGNED_IN || action == Constants.STATUS_SIGNED_OUT) {
            clockInOut(action, odometer.toDouble())
            return
        }
        viewModel.fetchShiftsList()
        lifecycleScope.launch(Dispatchers.IO) {
            IncompleteRequestsRepository.createClockActionIncompleteRequest(action, odometer)
            withContext(Dispatchers.Main) {
                animateFAB()
                AppAuth.getInstance().changeUserStatus(action)
                if (AppAuth.getInstance().isConnected) {
                    retryWorker(requireContext())
                }
            }
        }
    }

    private fun displayDialogError(
        title: String,
        message: String,
        pair: Pair<ErrorType, String?>?
    ) {
        requireActivity().let { pActivity ->
            if (pActivity is BaseActivity) {
                pActivity.showNetworkErrorDialog(pair, requireContext(), childFragmentManager)
            }
        }
    }

    private fun clockInOut(action: String, odometer: Double?) {
        animateFAB()
        val changeStatusModel: ChangeStatusModel = odometer?.let { ChangeStatusModel(it) }
            ?: ChangeStatusModel()
        if (MainApplication.lastLocation != null) {
            changeStatusModel.gpsLocation =
                GPSLocation.fromAndroidLocation(MainApplication.lastLocation)
        }
        context?.let { notNullContext ->
            if (action == Constants.STATUS_SIGNED_IN) {
                TechnicianTimeHelper(notNullContext).clockIn(changeStatusModel, lifecycle, {
                    AppAuth.getInstance().changeUserStatus(action)
                    if (!ServiceTools.isServiceRunning(
                            TechnicianService::class.java.name,
                            notNullContext
                        )
                    ) {
                        notNullContext.startService(Intent(activity, TechnicianService::class.java))
                    }
                }, ::displayDialogError)
            } else {
                TechnicianTimeHelper(notNullContext).clockOut(changeStatusModel, lifecycle, {
                    AppAuth.getInstance().changeUserStatus(action)
                    TrackService.stopTrackingService(notNullContext)
                }, ::displayDialogError)
            }
        }
        changeStatusModel.actionType = action
    }

    // Extract the clock out dialog to recall it
    private fun showClockOutDialog() {
        if (AppAuth.getInstance().isConnected) {
            if (!checkDispatchedCallsBeforeClockOut(childFragmentManager)) {
                showClockOutDialog(
                    getString(R.string.clock_out),
                    requireActivity().supportFragmentManager
                ) { aInteger: Int ->
                    clockOut(aInteger.toString())
                }
            }
        } else {
            showOfflineDialogue()
        }
    }

    private fun showOfflineDialogue() {
        val builder = context?.let { AlertDialog.Builder(it) }
        builder?.setMessage(getString(R.string.offline_warning))?.setCancelable(false)
            ?.setPositiveButton(android.R.string.ok) { _, _ -> }
        builder?.create()?.show()
    }

    private fun showFailedSyncDialogue() {
        val builder = context?.let { AlertDialog.Builder(it) }
        builder?.setTitle(getString(R.string.unsynced_data))
            ?.setMessage(getString(R.string.unsynced_data_text))?.setCancelable(true)
            ?.setNegativeButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
            ?.setPositiveButton(R.string.show_unsynced_data) { _: DialogInterface?, _: Int ->
                val aboutIntent = Intent(activity, SyncActivity::class.java)
                startActivity(aboutIntent)
            }
        builder?.create()?.show()
    }

    // Try the clock out action
    private fun clockOut(odometerTextValue: String) {
        val odometerValue: Int
        try {
            odometerValue = odometerTextValue.toInt()


            // Validation odometer value shouldn't less than dispatch (last odometer value)
            if (odometerValue < AppAuth.getInstance().lastOdometer) {
                showSimpleAlertDialog(
                    getString(R.string.warning),
                    getString(R.string.warning_odometer_message),
                    getString(R.string.ok),
                    childFragmentManager,
                    true
                ) {
                    // aBoolean is not needed because this dialog only shows one option
                    showClockOutDialog()
                }
                return
            }
            AppAuth.getInstance().lastOdometer = odometerValue
        } catch (e: NumberFormatException) {
            Snackbar.make(binding.btnAction, R.string.invalid_odometer_value, Snackbar.LENGTH_LONG)
                .show()
            return
        }
        action(Constants.STATUS_SIGNED_OUT, odometerValue)
    }

    private fun animateFAB() {
        isFabOpen = if (isFabOpen) {
            binding.btnAction.startAnimation(rotateBackward)
            binding.layActions.startAnimation(fabClose)
            false
        } else {
            binding.layActions.visibility = View.VISIBLE
            binding.btnAction.startAnimation(rotateForward)
            binding.layActions.startAnimation(fabOpen)
            true
        }
        binding.fabOpen = isFabOpen
    }

    override fun authStateChanged(appAuth: AppAuth) {
        // do nothing
    }

    override fun userUpdated(technicianUser: TechnicianUser) {
        setBindingUser()
    }

    override fun gpsStateChanged(state: Boolean) {
        // do nothing
    }

    override fun requestsChanged(count: Int) {
        // do nothing
    }

    override fun updatePayPeriods() {
        try {
            viewModel.fetchShiftsList()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    override fun onListenError(
        error: Pair<ErrorType, String?>?,
        requestType: String,
        callId: Int,
        data: String
    ) {
        when (requestType) {
            "LaunchIn", "LaunchOut", "BrakeIn", "BrakeOut" -> {
                requireActivity().let { activity ->
                    if (activity is BaseActivity) {
                        activity.showNetworkErrorDialog(
                            error,
                            requireContext(),
                            childFragmentManager
                        )
                    }
                }
            }
        }

    }

    override fun onTapShift(shiftUI: ShiftUI) {
        shiftUI.date?.let { shiftDate ->
            val timeIntent = Intent(requireContext(), TimeCardsCalendarActivity::class.java)
            timeIntent.putExtra(SHIFT_ID_KEY, shiftUI.shiftId)
            timeIntent.putExtra(SHIFT_TITLE_KEY, shiftUI.title)
            timeIntent.putExtra(SHIFT_DATE_KEY, shiftDate.time)
            startActivity(timeIntent)
        } ?: kotlin.run {
            Toast.makeText(requireContext(), getString(R.string.invalid_date), Toast.LENGTH_SHORT)
                .show()
        }

    }
}