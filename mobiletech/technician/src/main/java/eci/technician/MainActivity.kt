package eci.technician

import android.app.ProgressDialog
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.gms.maps.MapsInitializer
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.picasso.Picasso
import eci.signalr.ConnectionListener
import eci.signalr.messenger.MessengerEventListener
import eci.technician.activities.*
import eci.technician.activities.fieldTransfer.FieldTransferActivity
import eci.technician.activities.mywarehouse.NewMyWarehouseActivity
import eci.technician.activities.searchWarehouses.SearchWarehousesPartsActivity
import eci.technician.activities.transfers.TransfersActivity
import eci.technician.databinding.ActivityMainBinding
import eci.technician.databinding.DialogCustomBodyActionBinding
import eci.technician.dialog.DialogBeforeActionHelper.checkDispatchedCallsBeforeClockOut
import eci.technician.dialog.DialogManager.createErrorDialog
import eci.technician.dialog.DialogManager.showClockOutDialog
import eci.technician.dialog.DialogManager.showSimpleAlertDialog
import eci.technician.dialog.LoginViewModel
import eci.technician.fragments.MessagesFragment
import eci.technician.fragments.OrdersFragment
import eci.technician.fragments.TimeCardsFragment
import eci.technician.helpers.*
import eci.technician.helpers.AppAuth.AuthStateListener
import eci.technician.helpers.ClockOutHelper.performClockIn
import eci.technician.helpers.DateTimeHelper.formatTimeDateWithDay
import eci.technician.helpers.DialogHelperManager.displayOkMessage
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.ErrorHelper.MTErrorListener
import eci.technician.helpers.KeyboardHelper.hideSoftKeyboard
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository.RetrofitRepositoryObject.getInstance
import eci.technician.helpers.chat.ChatHandler
import eci.technician.helpers.notification.Badges
import eci.technician.helpers.notification.BadgesHandler.activateBadge
import eci.technician.helpers.notification.BadgesHandler.deactivateBadge
import eci.technician.helpers.notification.BadgesHandler.hasActiveBadges
import eci.technician.helpers.sortList.EquipmentHistorySortByCode
import eci.technician.interfaces.IOrderListTabListener
import eci.technician.interfaces.IPayPeriodsListener
import eci.technician.interfaces.UpdateChatIconListener
import eci.technician.models.TechnicianUser
import eci.technician.models.equipment.EquipmentSearchModel
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.repository.PartsRepository.getStringOfListOfPartsWithChanges
import eci.technician.service.ChatService
import eci.technician.service.TechnicianService
import eci.technician.tools.CircleTransformation
import eci.technician.tools.Constants
import eci.technician.viewmodels.MainViewModel
import eci.technician.viewmodels.OrderFragmentViewModel
import eci.technician.viewmodels.ViewModelUtils
import eci.technician.workers.OfflineManager.retryAttachmentWorker
import eci.technician.workers.OfflineManager.retryNotesWorker
import eci.technician.workers.OfflineManager.retryWorker
import kotlinx.coroutines.*
import microsoft.aspnet.signalr.client.ConnectionState
import java.util.*

class MainActivity : BaseActivity(), AuthStateListener, ConnectionListener,
    MTErrorListener, UpdateChatIconListener {

    private lateinit var binding: ActivityMainBinding
    private val timeChangeReceiver: TimeChangeReceiver = TimeChangeReceiver()
    private lateinit var connection: NetworkConnection
    private lateinit var banner: TextView
    var unsycCounter: TextView? = null
    private var hasUnSyncData = false
    private var launchedFromChatNotification = false
    private var errorDialog: AlertDialog? = null
    private val viewModel: OrderFragmentViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatHandler.setListener(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        banner = binding.containerOffline.offlineTextView
        hasUnSyncData = false
        MapsInitializer.initialize(this)
        ErrorHandler.get().addErrorListener(this)
        AppAuth.getInstance().addUserUpdateListener(this)
        startChatService()
        if (AppAuth.getInstance().isConnected) {
            banner.visibility = View.GONE
        } else {
            banner.setText(R.string.offline_no_internet_connection)
            banner.setBackgroundColor(resources.getColor(R.color.colorOfflineDark))
            banner.visibility = View.VISIBLE
        }
        connection = NetworkConnection(baseContext)
        connection.observe(this, { aBoolean: Boolean ->
            if (aBoolean) {
                banner.setText(R.string.back_online)
                AppAuth.getInstance().isConnected = true
                banner.setBackgroundColor(resources.getColor(R.color.colorOnline))
                object : CountDownTimer(3000, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        // do nothing
                    }

                    override fun onFinish() {
                        if (AppAuth.getInstance().isConnected) {
                            banner.visibility = View.GONE
                            val incompleteRequests =
                                DatabaseRepository.getInstance().incompleteRequestSync
                            if (incompleteRequests.size > 0) {
                                retryWorker(applicationContext)
                            }
                            retryAttachmentWorker(applicationContext)
                            retryNotesWorker(applicationContext)
                        }
                    }
                }.start()
            } else {
                banner.setText(R.string.offline_no_internet_connection)
                banner.setBackgroundColor(resources.getColor(R.color.colorOfflineDark))
                banner.visibility = View.VISIBLE
                AppAuth.getInstance().isConnected = false
            }
        })
        launchedFromChatNotification = intent.getBooleanExtra(OPEN_FROM_CHAT_NOTIFICATION, false)
        setSupportActionBar(binding.toolbar)
        setupNavigationDrawer()
        setupViewPager()
        val tapped = intent.getBooleanExtra(Constants.TAPPED, false)
        if (tapped || intent.extras != null) {
            if (!AppAuth.getInstance().serverAddress.isEmpty()) {
                try {
                    viewModel.fetchTechnicianActiveServiceCalls()
                } catch (e: Exception) {
                    Log.e(BaseActivity.TAG, e.message, e.fillInStackTrace())
                }
            }
        }
        hideSoftKeyboard(binding.root)
        setNewRequestsCount()
        viewModel.fetchTechnicianActiveServiceCalls()
        observeClockOutEvent()
        observeNetworkError()
        observeTransferPermission()
        mainViewModel.checkTransferMenuVisibility()
    }

    private fun observeTransferPermission() {
        mainViewModel.transferVisibility.observe(this, { updatedTransferPermission ->
            val transferMenuItem = binding.navView.menu.findItem(R.id.nav_transfers)
            transferMenuItem.isVisible = updatedTransferPermission
        })
    }

    private fun observeNetworkError() {
        loginViewModel.error.observe(this,
            { pairEvent: ViewModelUtils.Event<Pair<ErrorType, String?>?> ->
                showNetworkErrorDialog(
                    pairEvent.getContentIfNotHandledOrReturnNull(),
                    this,
                    supportFragmentManager,
                    null
                )
            })
    }

    private fun observeClockOutEvent() {
        loginViewModel.isClockOutSuccess.observe(this,
            {
                it?.getContent()?.let { isSuccess ->
                    if (isSuccess) {
                        showMessageToConfirmLogOut()
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        MapsInitializer.initialize(this)
        setNewRequestsCount()
    }

    private fun fillProfileInfo() {
        val builder = Uri.parse(AppAuth.getInstance().serverAddress).buildUpon()
        val url = builder
            .appendPath("Common")
            .appendPath("GetUserImage")
            .appendQueryParameter("userId", AppAuth.getInstance().technicianUser.id)
            .build().toString()
        val headerView = binding.navView.getHeaderView(0)
        Picasso.with(this)
            .load(url)
            .error(R.drawable.mobile_tech_logo)
            .transform(CircleTransformation())
            .into(headerView.findViewById<View>(R.id.imgUser) as ImageView)
        (headerView.findViewById<View>(R.id.txtUsername) as TextView).text =
            AppAuth.getInstance().technicianUser.technicianName
        (headerView.findViewById<View>(R.id.txtEmail) as TextView).text =
            AppAuth.getInstance().technicianUser.username
        (headerView.findViewById<View>(R.id.txtStatus) as TextView).text =
            AppAuth.getInstance().technicianUser.friendlyState()
    }

    override fun onDestroy() {
        ErrorHandler.get().removeListener(this)
        AppAuth.getInstance().removeUserUpdateListener(this)
        try {
            MainApplication.connection?.let {
                it.removeConnectionListener(this)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(TAG, EXCEPTION, e)
        }
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        launchedFromChatNotification = false
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        lifecycleScope.launch(Dispatchers.IO) {
            hasUnSyncData = IncompleteRequestsRepository.checkUnsyncData()
            withContext(Dispatchers.Main) {
                if (!hasUnSyncData) {
                    deactivateBadge(Badges.UNSYNCED_DATA)
                    val menuItem = binding.navView.menu.findItem(R.id.unSync)
                    menuItem.setIcon(R.drawable.ic_nav_refresh)
                } else {
                    activateBadge(Badges.UNSYNCED_DATA)
                    val menuItem = binding.navView.menu.findItem(R.id.unSync)
                    menuItem.setIcon(R.drawable.ic_refreshwithbadge)
                }
                updateMenuIcon()
                binding.navView.menu.size()
                val createCallMenuItem = binding.navView.menu.findItem(R.id.nav_create_call)
                try {
                    createCallMenuItem.isVisible =
                        AppAuth.getInstance().technicianUser.canCreateCallPermission
                } catch (e: Exception) {
                    createCallMenuItem.isVisible = false
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setNewRequestsCount() {
        if (AppAuth.getInstance().requestHasBeenSeen) {
            deactivateBadge(Badges.FIELD_TRANSFER)
            val menuItem = binding.navView.menu.findItem(R.id.nav_field_transfer)
            menuItem.setIcon(R.drawable.ic_field_transfer)
        } else {
            activateBadge(Badges.FIELD_TRANSFER)
            val menuItem = binding.navView.menu.findItem(R.id.nav_field_transfer)
            menuItem.setIcon(R.drawable.ic_field_transfer_badge)
        }
        updateMenuIcon()
        binding.navView.menu.size()
    }

    private fun updateMenuIcon() {
        if (hasActiveBadges()) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_menu_item_badge)
            return
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu_icon)
    }

    private fun setupNavigationDrawer() {
        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        binding.navView.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            val itemId = item.itemId
            if (itemId == R.id.nav_warehouse) {
                showPartActivityForMyWareHouse()
                return@OnNavigationItemSelectedListener false
            }
            if (AppAuth.getInstance().isConnected) {
                if (itemId == R.id.nav_logout) {
                    logout()
                } else if (itemId == R.id.nav_group_calls) {
                    openGroupCallsActivity()
                } else if (itemId == R.id.nav_search_part) {
                    showPartActivityFromWareHouses()
                } else if (itemId == R.id.nav_completed) {
                    showCompletedCalls()
                } else if (itemId == R.id.nav_request_parts) {
                    showRequestPartsActivity()
                } else if (itemId == R.id.nav_field_transfer) {
                    showFieldTransferActivity()
                } else if (itemId == R.id.nav_equipment_history) {
                    showEquipmentSearch()
                } else if (itemId == R.id.nav_settings) {
                    openSettings()
                } else if (itemId == R.id.unSync) {
                    openUnSyncActivity()
                } else if (itemId == R.id.nav_create_call) {
                    validateClockInBeforeCreatingCall()
                } else if (itemId == R.id.nav_transfers) {
                    openTransfersActivity()
                }
            } else {
                if (itemId == R.id.nav_logout) {
                    logout()
                } else if (itemId == R.id.unSync) {
                    openUnSyncActivity()
                } else {
                    showUnavailableWhenOfflineMessage()
                }
            }
            false
        })
        val headerView = binding.navView.getHeaderView(0)
        binding.navView.itemIconTintList = null
        (headerView.findViewById<View>(R.id.txtVersion) as TextView).text =
            String.format("v.%s", BuildConfig.VERSION_NAME)
    }

    private fun openTransfersActivity() {
        val intent = Intent(this, TransfersActivity::class.java)
        startActivity(intent)
    }

    private fun validateClockInBeforeCreatingCall() {
        if (AppAuth.getInstance().technicianUser.isClockedOut) {
            showQuestionBoxWithCustomButtons(
                getString(R.string.create_call_title),
                getString(R.string.clock_in_question),
                getString(R.string.clock_in),
                getString(R.string.cancel)
            ) { dialog: DialogInterface?, which: Int ->
                performClockIn(getString(R.string.clock_in),
                    supportFragmentManager,
                    this@MainActivity,
                    this@MainActivity,
                    { openCreateCallActivity() },
                    { title: String, message: String, error: Pair<ErrorType, String?>? ->
                        displayDialogError(
                            title,
                            message,
                            error
                        )
                    },
                    { showOfflineMessageForClockIn() },
                    { null },  // TODO FOR LOADING
                    { null } // TODO FOR CANCEL
                )
            }
        } else {
            startActivity(Intent(this, CreateCallActivity::class.java))
        }
    }

    private fun openCreateCallActivity() {
        startActivity(Intent(this, CreateCallActivity::class.java))
        return Unit
    }

    private fun displayDialogError(
        title: String,
        message: String,
        error: Pair<ErrorType, String?>?
    ) {
        displayOkMessage(title, message, getString(R.string.ok), baseContext) { null }
        return Unit
    }

    private fun showOfflineMessageForClockIn() {
        showUnavailableWhenOfflineMessage()
        return Unit
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openUnSyncActivity() {
        val aboutIntent = Intent(this, SyncActivity::class.java)
        startActivity(aboutIntent)
    }

    private fun showFieldTransferActivity() {
        val intent = Intent(this, FieldTransferActivity::class.java)
        startActivity(intent)
    }

    private fun showEquipmentSearch() {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.equipment_search_dialog, null)
        builder.setView(dialogView)
        val search = dialogView.findViewById<EditText>(R.id.search)
        builder.setPositiveButton(
            R.string.search
        ) { dialog, which -> searchEquipment(search.text.toString()) }
        val alertDialog = builder.create()
        if (alertDialog.window != null) {
            alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            search.requestFocus()
            search.setSelection(search.text.length)
        }
        alertDialog.show()
    }

    private fun searchEquipment(s: String) {
        if (TextUtils.isEmpty(s)) {
            return
        }
        val progressDialog = ProgressDialog.show(this, "", getString(R.string.loading))
        getInstance()
            .getEquipmentByText(s, this)
            .observe(this,
                Observer<GenericDataResponse<MutableList<EquipmentSearchModel>>> { (responseType, data, error) ->
                    when (responseType) {
                        RequestStatus.SUCCESS -> {
                            progressDialog.dismiss()
                            data?.let { myData -> showEquipmentSearchResults(myData) }
                        }
                        else -> {
                            progressDialog.dismiss()
                            this.showNetworkErrorDialog(error, this, supportFragmentManager)
                        }
                    }
                })
    }

    private fun showEquipmentSearchResults(result: List<EquipmentSearchModel>) {
        if (result.isEmpty()) {
            showMessageBox("", getString(R.string.no_equipment_found))
            return
        }
        if (result.size == 1) {
            showEquipmentHistory(result[0])
        }
        Collections.sort(result, EquipmentHistorySortByCode())
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.search_results)
        val arrayAdapter =
            ArrayAdapter<String>(this, R.layout.custom_simple_list_item_single_choice)
        for (element in result) {
            if (element != null) {
                var makeCode = element.makeCode
                var modelCode = element.modelCode
                var serialNumber = element.serialNumber
                makeCode = makeCode ?: ""
                modelCode = modelCode ?: ""
                serialNumber = serialNumber ?: ""
                arrayAdapter.add(String.format("%s/%s\n%s", makeCode, modelCode, serialNumber))
            }
        }
        builder.setNegativeButton(
            R.string.cancel
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        builder.setAdapter(
            arrayAdapter
        ) { dialog: DialogInterface?, which: Int ->
            showEquipmentHistory(
                result[which]
            )
        }
        builder.show()
    }

    private fun showEquipmentHistory(equipment: EquipmentSearchModel) {
        val intent = Intent(this@MainActivity, EquipmentHistoryActivity::class.java)
        intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, equipment.equipmentId)
        startActivity(intent)
    }

    private fun showRequestPartsActivity() {
        val intent = Intent(this, RequestPartsActivity::class.java)
        startActivity(intent)
    }

    private fun showCompletedCalls() {
        val intent = Intent(this, CompletedCallsActivity::class.java)
        startActivity(intent)
    }

    private fun showPartActivityFromWareHouses() {
        val intent = Intent(this, SearchWarehousesPartsActivity::class.java)
        startActivity(intent)
    }

    private fun showPartActivityForMyWareHouse() {
        val intent = Intent(this, NewMyWarehouseActivity::class.java)
        startActivity(intent)
    }

    private fun openGroupCallsActivity() {
        val intent = Intent(this, GroupCallsActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        if (AppAuth.getInstance().technicianUser.state == 2) {
            showMessageToConfirmLogOut()
        } else {
            showLogOutDialogWithConditions()
        }
    }

    private fun showMessageWhenStartedBreak() {
        showMessageBox("", getString(R.string.breakout_message_before_clockOut))
    }

    private fun showMessageWhenStartedLunch() {
        showMessageBox("", getString(R.string.lunchOut_message_before_clockOut))
    }

    private fun showMessageToConfirmLogOut() {
        val partsWithChangesString = getStringOfListOfPartsWithChanges()
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogViewBinding = DialogCustomBodyActionBinding.inflate(LayoutInflater.from(this))
        dialogBuilder.setView(dialogViewBinding.root)
        dialogViewBinding.dialogTitle.text = getString(R.string.logout)
        dialogViewBinding.dialogBody3.text = getString(R.string.confirm_log_out)
        dialogViewBinding.dialogBody3.visibility = View.VISIBLE
        if (!partsWithChangesString.isEmpty()) {
            dialogViewBinding.dialogBody.visibility = View.GONE
            dialogViewBinding.dialogBody2.setText(R.string.the_parts_added_locally_will_be_deleted)
            dialogViewBinding.dialogAction.text = partsWithChangesString
        } else {
            dialogViewBinding.dialogBody.visibility = View.GONE
            dialogViewBinding.dialogBody2.visibility = View.GONE
            dialogViewBinding.dialogAction.visibility = View.GONE
        }
        dialogBuilder.setPositiveButton(
            getString(R.string.logout)
        ) { dialog: DialogInterface?, which: Int ->
            try {
                loginViewModel.deleteDataOnLogout()
                AppAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    EXCEPTION,
                    e
                )
            }
        }
        dialogBuilder.setNegativeButton(
            getString(R.string.cancel)
        ) { dialog: DialogInterface?, which: Int -> }
        val dialog = dialogBuilder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun setupViewPager() {
        val pagerAdapter: ViewPagerAdapter = ViewPagerAdapter(
            supportFragmentManager
        )
        binding.viewPager.offscreenPageLimit = 3
        pagerAdapter.addFragment(OrdersFragment())
        val icons: IntArray
        icons = if (AppAuth.getInstance().chatEnabled) {
            pagerAdapter.addFragment(MessagesFragment())
            intArrayOf(R.drawable.orders, getChatIcon(0), statusIcon)
        } else {
            intArrayOf(R.drawable.orders, statusIcon)
        }
        pagerAdapter.addFragment(TimeCardsFragment())
        binding.viewPager.adapter = pagerAdapter
        binding.tabs.setupWithViewPager(binding.viewPager)
        for (i in 0 until binding.tabs.tabCount) {
            val tab = binding.tabs.getTabAt(i)
            tab?.setIcon(icons[i])
        }
        binding.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // do nothing
            }

            override fun onPageSelected(position: Int) {
                if (AppAuth.getInstance().chatEnabled) {
                    if (AppAuth.getInstance().isConnected) {
                        updateTitle(position)
                    } else {
                        if (binding.viewPager.currentItem === 1) {
                            showUnavailableWhenOfflineMessage()
                            binding.viewPager.currentItem = 0
                        }
                    }
                } else {
                    updateTitle(position)
                }
                try {
                    val fragment =
                        (binding.viewPager.adapter as ViewPagerAdapter?)?.getItem(position)
                    if (fragment is IPayPeriodsListener) {
                        (fragment as IPayPeriodsListener).updatePayPeriods()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                }
                try {
                    val fragment =
                        (binding.viewPager.adapter as ViewPagerAdapter?)?.getItem(position)
                    if (fragment is IOrderListTabListener) {
                        (fragment as IOrderListTabListener).updateTheCurrentList()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                // do nothing
            }
        })
    }

    private val statusIcon: Int
        private get() {
            var statusIcon = R.drawable.ic_alarm_clocked_out
            if (AppAuth.getInstance().isLoggedIn) {
                val state = AppAuth.getInstance().technicianUser.state
                statusIcon =
                    if (state == 1 || state == 4 || state == 6) R.drawable.ic_clocked_in else R.drawable.ic_alarm_clocked_out
            }
            return statusIcon
        }

    private fun updateDateTime() {
        if (binding.tabs.selectedTabPosition === 2 || !AppAuth.getInstance().chatEnabled && binding.tabs.selectedTabPosition === 1) {
            val now = Date()
            title = formatTimeDateWithDay(now)
        }
    }

    private fun updateTitle(position: Int) {
        when (position) {
            0 -> {
                setTitle(R.string.service_calls_title)
            }
            1 -> {
                if (AppAuth.getInstance().chatEnabled) {
                    setTitle(R.string.chats_title)
                } else {
                    updateDateTime()
                }
            }
            2 -> {
                updateDateTime()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (AppAuth.getInstance().isLoggedIn) {
            if (!ServiceHelper.isServiceRunning(TechnicianService::class.java, this)) {
                startTechnicianService()
            }
            updateTitle(binding.tabs.selectedTabPosition)
            try {
                registerReceiver(timeChangeReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            }
        } else {
            openLoginActivity()
        }
        goToChatTabIfRequired()
    }

    private fun goToChatTabIfRequired() {
        try {
            if (!launchedFromChatNotification) {
                return
            }
            if (java.lang.Boolean.FALSE == AppAuth.getInstance().chatEnabled) {
                return
            }
            val viewPager = binding.viewPager.adapter as ViewPagerAdapter?
            if (viewPager != null && viewPager.count >= 1) {
                val fragment = viewPager.getItem(1)
                if (fragment is MessengerEventListener) {
                    binding.viewPager.currentItem = 1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    override fun onPause() {
        try {
            unregisterReceiver(timeChangeReceiver)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        super.onPause()
    }

    private fun startService() {
        startTechnicianService()
    }

    private fun startChatService() {
        try {
            val intent = Intent(applicationContext, ChatService::class.java)
            startService(intent)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun startTechnicianService() {
        try {
            val intent = Intent(applicationContext, TechnicianService::class.java)
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun stopService() {
        val intent = Intent(this, TechnicianService::class.java)
        stopService(intent)
        try {
            val chatServiceIntent = Intent(this, ChatService::class.java)
            stopService(chatServiceIntent)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun openLoginActivity() {
        startActivity(Intent(this, UserActivity::class.java))
        finish()
    }

    override fun connected() {
        // do nothing
    }

    override fun disconnected() {
        // do nothing
    }

    override fun stateChanged(oldState: ConnectionState, newState: ConnectionState) {
        // do nothing
    }

    override fun reconnecting() {
        AppAuth.getInstance().isReconnecting = true
    }

    fun setTimeCardIcon() {
        val tab: TabLayout.Tab?
        tab = if (AppAuth.getInstance().chatEnabled) {
            binding.tabs.getTabAt(2)
        } else {
            binding.tabs.getTabAt(1)
        }
        tab?.setIcon(statusIcon)
    }

    fun setChatIcon(count: Int) {
        var tab: TabLayout.Tab? = null
        if (AppAuth.getInstance().chatEnabled) {
            tab = binding.tabs.getTabAt(1)
        }
        tab?.setIcon(getChatIcon(count))
    }

    private fun getChatIcon(count: Int): Int {
        return if (count > 0) R.drawable.ic_chat_bubble_with_notification else R.drawable.ic_chat_bubble
    }

    override fun authStateChanged(appAuth: AppAuth) {
        if (appAuth.isLoggedIn) {
            try {
                MainApplication.connection?.let {
                    it.addConnectionListener(this)
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Log.e(TAG, EXCEPTION, e)
            }
            startService()
        } else {
            stopService()
            openLoginActivity()
        }
    }

    override fun userUpdated(technicianUser: TechnicianUser) {
        fillProfileInfo()
        setTimeCardIcon()
        checkUnsyncData()
        if (technicianUser.status == Constants.STATUS_SIGNED_IN) {
            mainViewModel.checkTransferMenuVisibility()
        }
    }

    private fun checkUnsyncData() {
        lifecycleScope.launch(Dispatchers.IO) {
            hasUnSyncData = IncompleteRequestsRepository.checkUnsyncData()
            withContext(Dispatchers.Main) {
                if (hasUnSyncData) {
                    activateBadge(Badges.UNSYNCED_DATA)
                } else {
                    deactivateBadge(Badges.UNSYNCED_DATA)
                }
                updateMenuIcon()
                invalidateOptionsMenu()
            }
        }
    }

    override fun gpsStateChanged(state: Boolean) {
        val appAuth = AppAuth.getInstance()
        if (!state && appAuth.isLoggedIn) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("GPS")
                .setMessage("GPS turned off, please turn on")
                .setPositiveButton(R.string.turn_on) { dialog, which ->
                    startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )
                }
            builder.create().show()
        }
    }

    override fun requestsChanged(count: Int) {
        setNewRequestsCount()
    }

    private fun showLogOutDialogWithConditions() {
        showSimpleAlertDialog(
            getString(R.string.logout),
            getString(R.string.logout_warning),
            getString(R.string.clock_out),
            supportFragmentManager,
            false
        ) { aBoolean: Boolean ->
            // aBoolean is not needed because this dialog only shows one option
            if (!AppAuth.getInstance().isConnected) {
                showOfflineMessageForClockIn()
                return@showSimpleAlertDialog Unit
            }
            if (aBoolean) {
                when (AppAuth.getInstance().technicianUser.state) {
                    3 -> {
                        showMessageWhenStartedLunch()
                    }
                    5 -> {
                        showMessageWhenStartedBreak()
                    }
                    else -> {
                        if (!checkDispatchedCallsBeforeClockOut(supportFragmentManager)) {
                            showClockOutDialogToEnterOdometer()
                        }
                    }
                }
            }
        }
    }

    private fun showClockOutDialogToEnterOdometer() {
        showClockOutDialog(
            getString(R.string.clock_out),
            supportFragmentManager
        ) { odometerValue: Int ->
            // clocked out
            if (odometerValue >= AppAuth.getInstance().lastOdometer) {
                val incompleteRequests =
                    DatabaseRepository.getInstance().incompleteRequestList
                val attachemntlist =
                    DatabaseRepository.getInstance().attachmentIncompleteRequestList
                try {
                    if (!incompleteRequests.isEmpty() || !attachemntlist.isEmpty()) {
                        showFailedSyncDialogue()
                    } else {
                        performRequestClockOut(odometerValue)
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        EXCEPTION,
                        e
                    )
                }
            } else {
                showSimpleAlertDialog(
                    getString(R.string.warning),
                    getString(R.string.warning_odometer_message),
                    getString(R.string.ok),
                    supportFragmentManager,
                    true
                ) { aBoolean: Boolean? ->
                    showLogOutDialogWithConditions()
                    Unit
                }
            }
            Unit
        }
    }

    private fun showFailedSyncDialogue() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.unsynced_data))
            .setMessage(getString(R.string.unsynced_data_text)).setCancelable(true)
            .setNegativeButton(
                android.R.string.ok
            ) { dialog: DialogInterface?, id: Int -> }
            .setPositiveButton(R.string.show_unsynced_data) { dialogInterface, i ->
                val aboutIntent = Intent(this, SyncActivity::class.java)
                startActivity(aboutIntent)
            }
        builder.create().show()
    }

    fun performRequestClockOut(odometerValue: Int?) {
        loginViewModel.clockOutTechnician(odometerValue)
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem > 0) {
            binding.viewPager.currentItem = 0
        } else {
            super.onBackPressed()
            finishAffinity()
        }
    }

    private fun onDismissErrorDialogForCanceledDeletedReassigned() {
        viewModel.fetchTechnicianActiveServiceCalls(forceUpdate = true)
        errorDialog = null
        return Unit
    }


    override fun onListenError(
        error: Pair<ErrorType, String?>?,
        type: String,
        callId: Int,
        data: String
    ) {
        if (errorDialog != null) return
        if ("SERVICE_CALL_CANCELED_OR_DELETED" == type) {
            errorDialog = createErrorDialog(
                getString(R.string.warning),
                getString(R.string.the_service_call_was_canceled_or_completed, data),
                this,
                { onDismissErrorDialogForCanceledDeletedReassigned() }) { }
            errorDialog?.let { it.show() }
            return
        }
        if ("SERVICE_CALL_REASSIGNED" == type) {
            errorDialog = createErrorDialog(
                getString(R.string.warning),
                getString(R.string.the_service_call_was_reassigned, data),
                this,
                { onDismissErrorDialogForCanceledDeletedReassigned() }) { }
            errorDialog?.let { it.show() }
            return
        }
        if ("IncompleteCall" == type && viewModel.shouldShowMessage) {
            viewModel.isShowingMessage()
            val message = """
                ${error?.second.toString()}
                ${getString(R.string.check_the_unsync_data)}
                """.trimIndent()
            val error2 = error?.let { Pair(it.first, message) }
            this.showNetworkErrorDialog(error2, this, supportFragmentManager, null)
            return
        }
        if ("DepartCall" == type && viewModel.shouldShowMessage) {
            viewModel.isShowingMessage()
            val message = """
                ${error?.second.toString()}
                ${getString(R.string.check_the_unsync_data)}
                """.trimIndent()
            val error2 = error?.let { Pair(it.first, message) }
            this.showNetworkErrorDialog(error2, this, supportFragmentManager, null)
            return
        }
        if (viewModel.shouldShowMessage) {
            viewModel.isShowingMessage()
            val message = """
                ${error?.second.toString()}
                ${getString(R.string.check_the_unsync_data)}
                """.trimIndent()
            val error2 = error?.let { Pair(it.first, message) }
            if (error2 != null) {
                viewModel.checkIfCompletedStatus(
                    callId,
                    error2
                ) { isCompleted: Boolean, error: Pair<ErrorType, String?> ->
                    checkIfIsCompletedToShowError(
                        isCompleted,
                        error
                    )
                }
            }
        }
    }

    private fun checkIfIsCompletedToShowError(
        isCompleted: Boolean,
        error: Pair<ErrorType, String?>
    ) {
        if (isCompleted) {
            this.showNetworkErrorDialog(error, this, supportFragmentManager, null)
        }
        return Unit
    }

    override fun updateChatIcon(count: Int) {
        setChatIcon(count)
    }

    internal inner class TimeChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateDateTime()
        }
    }

    private inner class ViewPagerAdapter internal constructor(manager: FragmentManager) :
        FragmentPagerAdapter(manager) {
        private val mFragmentList: MutableList<Fragment> = ArrayList()
        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment) {
            mFragmentList.add(fragment)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return null
        }
    }

    companion object {
        private const val TAG = "BaseActivity"
        private const val EXCEPTION = "Exception logger"
        var OPEN_FROM_CHAT_NOTIFICATION = "openFromChatNotification"
    }

}