package eci.technician.activities

import android.app.*
import android.content.*
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateFormat
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import eci.technician.BaseActivity
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.activities.allparts.PartsManagerActivity
import eci.technician.activities.attachment.AttachmentsActivity
import eci.technician.activities.notes.NotesActivity
import eci.technician.activities.transfers.TransferViewModel
import eci.technician.activities.transfers.TransfersActivity
import eci.technician.databinding.ActivityOrderDetailBinding
import eci.technician.dialog.DialogBeforeActionHelper.checkDispatchedCallsBeforeDispatch
import eci.technician.dialog.DialogManager.showSimpleAlertDialog
import eci.technician.helpers.*
import eci.technician.helpers.AddressMapHelper.openNavigation
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.ErrorHelper.OrderDetailErrorListener
import eci.technician.helpers.PhoneHelper.getFormattedCustomerPhoneNumber
import eci.technician.helpers.PhoneHelper.getOnlyNumbersBeforeDialing
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.UpdateLaborPostModel
import eci.technician.models.gps.GPSLocation
import eci.technician.models.order.*
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.models.transfers.Warehouse
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.tools.Constants
import eci.technician.viewmodels.OrderDetailViewModel
import eci.technician.workers.OfflineManager.retryWorker
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_order_detail.*
import kotlinx.coroutines.*
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : BaseActivity(), OrderDetailErrorListener {

    private var serviceOrder: ServiceOrder? = null
    private var serviceOrderId: String? = null
    private var serviceOrderCode: String = ""
    private var serviceCallTemporalData: ServiceCallTemporalData? = null
    private var serviceOrderCallNumberId = 0

    private var positiveButton: Button? = null

    private var dateFormat: Format? = null
    private var timeFormat: Format? = null
    private var pattern: String? = null
    private var timePattern: String? = null
    private var dateFormatter: SimpleDateFormat? = null
    private var timeFormatter: SimpleDateFormat? = null
    private var inputDate = Calendar.getInstance().time

    lateinit var realm: Realm
    private var warehouseList: List<Warehouse>? = null

    companion object {
        const val TAG = "OrderViewActivity"
        const val EXCEPTION = "Exception logger"
        const val EDIT_IP = 0
        const val EDIT_MAC = 1
        const val EDIT_REMARKS = 2
        const val COMPLETED_CODE = 100
        const val SC_REASSIGNED = "REASSIGNED"
        const val SC_UNAVAILABLE = "UNAVAILABLE"
    }

    private val viewModel: OrderDetailViewModel by viewModels()
    lateinit var binding: ActivityOrderDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.ORDER_DETAIL_ACTIVITY)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_order_detail)
        ErrorHandler.get().addErrorListener(this)



        binding = DataBindingUtil.setContentView(this, R.layout.activity_order_detail)
        binding.lifecycleOwner = this
        val banner = binding.containerOffline.offlineTextView
        val connection = NetworkConnection(baseContext)
        if (intent.hasExtra(Constants.EXTRA_ORDER_ID)) {
            serviceOrderId = intent.getStringExtra(Constants.EXTRA_ORDER_ID)
            serviceOrderCallNumberId = intent.getIntExtra(Constants.EXTRA_CALL_NUMBER_ID, 0)
            serviceOrderCode = intent.getStringExtra(Constants.EXTRA_CALL_NUMBER_CODE) ?: ""
            serviceCallTemporalData = DatabaseRepository.getInstance()
                .getServiceCallTemporaryData(serviceOrderCallNumberId)
            observeServiceCall()
        } else {
            finish()
        }

        if (AppAuth.getInstance().isConnected) {
            banner.visibility = View.GONE
        } else {
            banner.setText(R.string.offline_no_internet_connection)
            banner.setBackgroundColor(getColor(R.color.colorOfflineDark))
            banner.visibility = View.VISIBLE
        }

        connection.observe(this, { t ->
            t?.let {
                viewModel.hasConnection = it
                if (it) {
                    banner.setText(R.string.back_online)
                    banner.setBackgroundColor(getColor(R.color.colorOnline))
                    binding.item = serviceOrder
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            //do nothing
                        }

                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                            if (AppAuth.getInstance().isConnected) {
                                banner.visibility = View.GONE
                            }
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                    banner.setText(R.string.offline_no_internet_connection)
                    banner.setBackgroundColor(getColor(R.color.colorOfflineDark))
                    banner.visibility = View.VISIBLE
                    if (binding.refreshServiceOrder.isRefreshing) {
                        binding.refreshServiceOrder.isRefreshing = false
                    } else {
                        Log.i(TAG, "It is not refreshing")
                    }
                    retryWorker(this)
                }
            }
        })

        dateFormat = DateFormat.getDateFormat(applicationContext)
        timeFormat = DateFormat.getTimeFormat(applicationContext)

        pattern = (dateFormat as SimpleDateFormat).toLocalizedPattern()
        timePattern = (timeFormat as SimpleDateFormat).toLocalizedPattern()

        dateFormatter = SimpleDateFormat(pattern, Locale.getDefault())
        timeFormatter = SimpleDateFormat(timePattern, Locale.getDefault())

        setSupportActionBar(binding.toolbar)

        //not necessary
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnContactPhone.setOnLongClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("", serviceOrder?.contactPhone)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        binding.btnContactPhone.setOnClickListener {
            val phone = binding.btnContactPhone.text.toString()
            if (phone.isNotEmpty()) {
                callContact(phone)
            }
        }
        binding.refreshServiceOrder.setOnRefreshListener {
            if (AppAuth.getInstance().isConnected) {
                refreshJob()
            } else {
                binding.refreshServiceOrder.isRefreshing = false
                showUnavailableWhenOfflineMessage()
            }
        }

        binding.btnCustomerPhone.setOnClickListener {
            val phone = binding.btnCustomerPhone.text.toString()
            if (phone.isNotEmpty()) {
                callContact(phone)
            }
        }

        binding.btnContactEmail.setOnLongClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("", serviceOrder?.contactEmail)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        binding.btnContactEmail.setOnClickListener {
            val email = binding.btnContactEmail.text.toString()
            if (email.isNotEmpty()) {
                serviceOrder?.let {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "message/rfc822"
                    i.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    i.putExtra(Intent.EXTRA_SUBJECT, "Call# " + it.callNumber_Code)
                    try {
                        startActivity(Intent.createChooser(i, getString(R.string.send_email_title)))
                    } catch (ex: ActivityNotFoundException) {
                        Log.e(TAG, EXCEPTION, ex)
                        Toast.makeText(
                            this,
                            getString(R.string.no_email_clients),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.txtIpAddress.setOnClickListener {
            serviceOrder?.let {
                if (it.equipmentNumber != null) {
                    changeEquipmentDetails(EDIT_IP)
                } else {
                    showMessageBox(
                        getString(R.string.unknown_equipment_title),
                        getString(
                            R.string.unknown_equipment_message,
                            getString(R.string.ip_address)
                        )
                    )
                }
            }
        }

        binding.txtMacAddress.setOnClickListener {
            serviceOrder?.let { serviceOrder ->
                if (serviceOrder.equipmentNumber != null) {
                    changeEquipmentDetails(EDIT_MAC)
                } else {
                    showMessageBox(
                        getString(R.string.unknown_equipment_title),
                        getString(
                            R.string.unknown_equipment_message,
                            getString(R.string.mac_address)
                        )
                    )
                }
            }
        }

        binding.txtRemarks.setOnClickListener {
            serviceOrder?.let { serviceOrder ->
                if (serviceOrder.equipmentNumber != null) {
                    changeEquipmentDetails(EDIT_REMARKS)
                } else {
                    showMessageBox(
                        getString(R.string.unknown_equipment_title),
                        getString(R.string.unknown_equipment_message, getString(R.string.remarks))
                    )
                }
            }
        }

        binding.btnEquipmentId.setOnLongClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("", serviceOrder?.equipmentNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        binding.btnEquipmentId.setOnClickListener {
            serviceOrder?.let { serviceOrder ->
                if (AppAuth.getInstance().isConnected) {
                    if (!TextUtils.isEmpty(serviceOrder.equipmentNumber)) {
                        showEquipmentHistory()
                    }
                } else {
                    showUnavailableWhenOfflineMessage()
                }
            }
        }

        binding.btnCustomerAddress.setOnLongClickListener {
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("", serviceOrder?.getCustomerFullAddress())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        binding.btnCustomerAddress.setOnClickListener {
            if (AppAuth.getInstance().isConnected) {
                openNavigation()
            } else {
                showUnavailableWhenOfflineMessage()
            }
        }


        binding.btnDispatch.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isBreakOrLunchStarted) {
                showBreakLunchMessageBeforeAction(Constants.STRING_DISPATCH)
            } else {
                tryToDispatch()
            }
        }

        binding.btnSchedule.setOnClickListener { tryToSchedule() }

        binding.btnUnDispatch.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isBreakOrLunchStarted) {
                showBreakLunchMessageBeforeAction(Constants.STRING_UNDISPATCH)
            } else {
                tryToUnDispatch()
            }
        }

        binding.btnArrive.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isBreakOrLunchStarted) {
                showBreakLunchMessageBeforeAction(Constants.STRING_ARRIVE)
            } else {
                tryToArrive()
            }
        }

        binding.btnHold.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isBreakOrLunchStarted) {
                showBreakLunchMessageBeforeAction(Constants.STRING_HOLD)
            } else {
                tryToHoldCall()
            }
        }

        binding.btnRelease.setOnClickListener {
            viewModel.updateProgressForActionPerformed(true)
            try {
                serviceOrder?.let { serviceOrder ->
                    val onHoldCode = serviceOrder.onHoldCode
                    val releasableHoldCodes: OrderedRealmCollection<HoldCode> =
                        DatabaseRepository.getInstance().holdCodesWithAllowTechRelease
                    val existHoldCode = releasableHoldCodes.filter {
                        it.onHoldCode == onHoldCode
                    }
                    if (existHoldCode.isNotEmpty()) {
                        val statusChangeModel = StatusChangeModel()
                        statusChangeModel.actionTime = Date()
                        statusChangeModel.callId = serviceOrder.callNumber_ID
                        releaseCallOffline(statusChangeModel)
                    } else {
                        showMessageBox(
                            "",
                            getString(R.string.cant_release_call, serviceOrder.onHoldDescription)
                        )
                    }
                }
            } catch (ignored: java.lang.Exception) {
                Log.e(TAG, EXCEPTION, ignored)
            } finally {
                viewModel.updateProgressForActionPerformed(false)
            }
        }

        binding.btnComplete.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isBreakOrLunchStarted) {
                showBreakLunchMessageBeforeAction(Constants.STRING_COMPLETE)
            } else {
                tryToComplete()
            }
        }

        binding.btnIncomplete.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isBreakOrLunchStarted) {
                showBreakLunchMessageBeforeAction(Constants.STRING_INCOMPLETE)
            } else {
                tryToIncomplete()
            }
        }

        lifecycleScope.launch {
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .getServiceCallNotesTypes(this@OrderDetailActivity)
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .getServiceCallNotesByCallId(serviceOrderCallNumberId, this@OrderDetailActivity)
        }
        getCanonDetails()
        hideKeyboard(this)
        adjustButtonTextSize()
        setEnableOptionsMenu(false)
        observeUpdateForOneServiceCall()
        setPartsButtonListener()
        initWarehouseList()
    }

    private fun setPartsButtonListener() {
        binding.partsCountButton.containerCardView.setOnClickListener {
            if (serviceOrderCallNumberId == 0) return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                val serviceOrderEntity =
                    ServiceOrderRepository.getServiceOrderByCallId(serviceOrderCallNumberId)
                        ?: return@launch
                val partsIntent = Intent(this@OrderDetailActivity, PartsManagerActivity::class.java)

                partsIntent.putExtra(
                    PartsManagerActivity.EXTRA_IS_ASSIST,
                    serviceOrderEntity.isAssist()
                )
                partsIntent.putExtra(PartsManagerActivity.CALL_ID, serviceOrderCallNumberId)
                withContext(Dispatchers.Main) {
                    startActivity(partsIntent)
                }
            }
        }
    }

    private fun observeIncompleteInProgress() {
        val callNumberId = intent.getIntExtra(Constants.EXTRA_CALL_NUMBER_ID, -1)
        DatabaseRepository.getInstance().getIncompleteRequestInProgress(callNumberId)
            .observe(this@OrderDetailActivity, { incomplete ->
                incomplete?.let {
                    if (it.isEmpty()) {
                        viewModel.updateProgressForIncompleteInProgress(false)
                    } else {
                        viewModel.updateProgressForIncompleteInProgress(true)
                    }
                }
            })
    }

    private fun observeUpdateForOneServiceCall() {
        val callNumberId = intent.getIntExtra(Constants.EXTRA_CALL_NUMBER_ID, -1)
        if (callNumberId == -1) return
        callNumberId.let { serviceOrderCallNumberId ->
            viewModel.fetchOneServiceCall(serviceOrderCallNumberId)
        }
    }

    private fun notifyUnavailable() {
        val pair = Pair(ErrorType.SOMETHING_WENT_WRONG, "")
        ErrorHandler.get()
            .notifyListeners(
                error = pair,
                requestType = "SERVICE_CALL_CANCELED_OR_DELETED",
                callId = serviceOrderCallNumberId,
                data = serviceOrderCode
            )
        finish()
    }

    private fun notifyReassigned() {
        val pair = Pair(ErrorType.SOMETHING_WENT_WRONG, "")
        ErrorHandler.get()
            .notifyListeners(
                error = pair,
                requestType = "SERVICE_CALL_REASSIGNED",
                callId = serviceOrderCallNumberId,
                data = serviceOrderCode
            )
        finish()
    }

    private fun showProgressBar() {
        setEnableOptionsMenu(false)
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.buttonsContainer.visibility = View.INVISIBLE
    }

    private fun hideProgressBar() {
        setEnableOptionsMenu(true)
        binding.progressBarContainer.visibility = View.GONE
        binding.buttonsContainer.visibility = View.VISIBLE
    }

    private fun setEnableOptionsMenu(enable: Boolean) {
        for (menuItem in binding.toolbar.menu.iterator()) {
            menuItem.isEnabled = enable
        }
    }

    private fun observeServiceCall() {
        try {
            viewModel.getOrderViewLiveData(serviceOrderCallNumberId)
                .observe(this) { serviceOrders: RealmResults<ServiceOrder?> ->
                    if (!serviceOrders.isEmpty()) {
                        val firstServiceOrder = serviceOrders.first()
                        if (firstServiceOrder != null && firstServiceOrder.isValid) {
                            serviceOrder = firstServiceOrder
                            viewModel.customerWarehouseId = serviceOrder?.customerWarehouseId ?: 0
                            binding.txtBillCode.text = firstServiceOrder.billCode
                            binding.txtCaller.text = firstServiceOrder.caller
                            binding.item = firstServiceOrder
                            updatePartsCountButton()
                            updatePartsDeletable(firstServiceOrder.callNumber_ID)
                            observePhoneNumber()
                        } else {
                            finish()
                        }
                    } else {
                        finish()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            ErrorHandler.get().notifyListeners(
                error = Pair(ErrorType.SOMETHING_WENT_WRONG, ""),
                requestType = "SERVICE_CALL_CANCELED_OR_DELETED",
                callId = serviceOrderCallNumberId,
                data = ""
            )
            finish()
        }
    }

    private fun updatePartsDeletable(callId: Int) {
        viewModel.updatePartDeletable(callId)
    }

    private fun updateCanonButton(canonDetails: CanonDetailsResponse) {
        try {
            val link = canonDetails.token
            val imageURL = canonDetails.snapshotLogo
            val deviceSummary = canonDetails.deviceSummary
            deviceSummary?.let {
                val reportingStatus = deviceSummary.status ?: ""
                val reportingDate =
                    if (deviceSummary.lastReportDate.isNullOrBlank()) "N/A" else deviceSummary.lastReportDate
                binding.txtSnapshotStatus.text =
                    String.format("Last Reporting Status: %s", reportingStatus)
                binding.txtSnapshotReportingDate.text =
                    String.format("Last Reporting Date: %s", reportingDate)
                binding.laySnapshot.visibility = View.VISIBLE
                link?.let {
                    binding.btnCanonSnapshot.setOnClickListener {
                        openWebPage(
                            link
                        )
                    }
                    binding.btnCanonSnapshot.visibility = View.VISIBLE
                    imageURL?.let {
                        loadCanonImage(it)
                    }
                }
            }
        } catch (ex: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ex)
        } finally {
            adjustButtonTextSize()
        }
    }

    private fun getCanonDetails() {
        lifecycleScope.launch(Dispatchers.IO){
            val sc = ServiceOrderRepository.getServiceOrderByCallId(serviceOrderCallNumberId)
            withContext(Dispatchers.Main){
                sc?.let {
                    var equipmentMake = sc.make
                    equipmentMake = equipmentMake?.toLowerCase(Locale.getDefault()) ?: ""
                    if (equipmentMake == "canon") {
                        RetrofitRepository
                            .RetrofitRepositoryObject
                            .getInstance()
                            .getCanonDetails(
                                sc.equipmentId.toString(),
                                sc.callNumber_Code ?: ""
                            )
                            .observe(this@OrderDetailActivity, {
                                it?.let {
                                    updateCanonButton(it)
                                }
                            })
                    }
                }
            }

        }
    }


    private fun loadCanonImage(imageURL: String) {
        val imageUri = imageURL?.toUri()
        Picasso.with(this).load(imageUri).into(binding.btnCanonSnapshot)
    }

    private fun updatePartsCountButton() {
        val orderId = serviceOrderCallNumberId
        lifecycleScope.launch(Dispatchers.IO) {
            var countParts = 0
            var neededParts = 0
            val isAssist = ServiceOrderRepository.isServiceOrderAssist(orderId)
            if (isAssist) {

                val pa = PartsRepository.getAllPartsByOrderIdForAssist(
                    orderId,
                    viewModel.currentTechWarehouseId,
                    viewModel.customerWarehouseId
                )
                countParts = pa.size
            } else {
                countParts = PartsRepository.getAllPartsByOrderId(orderId).size
                neededParts = PartsRepository.getAllSentNeededPartsByOrderId(orderId).size
            }

            withContext(Dispatchers.Main) {
                if (countParts == 0) {
                    binding.partsCountButton.numberOfParts.visibility = View.GONE
                } else {
                    binding.partsCountButton.numberOfParts.visibility = View.VISIBLE
                    binding.partsCountButton.numberOfParts.text = "$countParts"
                }
                if (neededParts == 0) {
                    binding.partsCountButton.redDot.visibility = View.GONE
                } else {
                    binding.partsCountButton.redDot.visibility = View.VISIBLE
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        hideKeyboard(this)
        setObservers()
        observeServiceCall()
    }

    private fun setObservers() {
        observeLoadingOneServiceCallFetch()
        observeToastEvent()
        observeFetchError()
        observeIncompleteInProgress()
        observeProgressBarForButtons()
        observeVerification()
    }

    private fun observePhoneNumber(){
        val customerPhoneNumber = binding.item?.customerPhoneNumber
        val contactPhoneNumber = binding.item?.contactPhone
        lifecycleScope.launch(Dispatchers.IO){
            val customerPhone = async { getFormattedCustomerPhoneNumber(customerPhoneNumber,applicationContext) }.await()
            val contactPhone = async { getFormattedCustomerPhoneNumber(contactPhoneNumber,applicationContext) }.await()
            withContext(Dispatchers.Main){
                customerPhone?.let { btnCustomerPhone.text = customerPhone.toString() }
                contactPhone?.let { btnContactPhone.text = contactPhone.toString() }
            }
        }
    }

    private fun observeVerification() {
        viewModel.verificationError.observe(this) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                if (it == SC_REASSIGNED) notifyReassigned()
                if (it == SC_UNAVAILABLE) notifyUnavailable()
            }
        }
    }

    private fun observeProgressBarForButtons() {
        viewModel.progressBarForButtons.observe(this) { triple ->
            triple?.let {
                if (triple.first || triple.second || triple.third) {
                    showProgressBar()
                }
                if (!triple.first && !triple.second && !triple.third) {
                    hideProgressBar()
                }
            }
        }
    }

    private fun observeFetchError() {
        viewModel.networkError.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                showNetworkErrorDialog(it, this, supportFragmentManager)
            }
        })
    }

    private fun observeToastEvent() {
        viewModel.toastMessage.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun observeLoadingOneServiceCallFetch() {
        viewModel.isLoadingFetchOneServiceCall.observe(this) {
            binding.refreshServiceOrder.isRefreshing = it
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_order_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        serviceOrder?.let {
            if (it.isValid) {
                menu?.findItem(R.id.action_cancel_call)?.isVisible = it.canCancel()
            }
            val allowNotes = AppAuth.getInstance().technicianUser.isAllowServiceCallNotes
            menu?.findItem(R.id.action_notes)?.isVisible = allowNotes

            val enableAttachments = AppAuth.getInstance().technicianUser.isFileAttachmentEnabled
            menu?.findItem(R.id.action_attachments)?.isVisible = enableAttachments
            var serviceCallStatus = ServiceOrderRepository.ServiceOrderStatus.PENDING
            menu?.findItem(R.id.action_transfer)?.isVisible = false
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    serviceOrder?.callNumber_ID?.let { it1 ->
                        serviceCallStatus =
                            ServiceOrderRepository.getServiceOrderStatusByCallNumberId(
                                it1
                            )
                        if (serviceCallStatus == ServiceOrderRepository.ServiceOrderStatus.DISPATCHED ||
                            serviceCallStatus == ServiceOrderRepository.ServiceOrderStatus.ARRIVED
                        ) {
                            val enableTransfer =
                                (AppAuth.getInstance().technicianUser.isAllowWarehousesTransfers && hasCustomerWarehouse())
                            menu?.findItem(R.id.action_transfer)?.isVisible = enableTransfer
                        }
                    }

                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (AppAuth.getInstance().isConnected) {
            serviceOrder?.let {
                when (itemId) {
                    R.id.action_equipment_history -> {
                        showEquipmentHistory()
                    }
                    R.id.action_cancel_call -> {
                        cancelDialog()
                    }
                    R.id.action_view_assistants -> {
                        showAssistants()
                    }
                    R.id.action_attachments -> {
                        openAttachments()
                    }
                    R.id.action_notes -> {
                        openNotes()
                    }
                    R.id.action_transfer -> {
                        openTransfers()
                    }
                    android.R.id.home -> finish()
                }
            }
        } else {
            when (itemId) {
                android.R.id.home -> finish()
                R.id.action_attachments -> {
                    openAttachments()
                }
                R.id.action_notes -> {
                    openNotes()
                }
                else -> {
                    showUnavailableWhenOfflineMessage()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openNotes() {
        val notesIntent = Intent(this, NotesActivity::class.java)
        notesIntent.putExtra(NotesActivity.EXTRA_CALL_ID, serviceOrderCallNumberId)
        notesIntent.putExtra(NotesActivity.EXTRA_CALL_NUMBER_CODE, serviceOrderCode)
        startActivity(notesIntent)
    }

    private fun openTransfers() {
        val transfersIntent = Intent(this, TransfersActivity::class.java)
        transfersIntent.putExtra(TransfersActivity.TRANSFER_TYPE, Warehouse.CUSTOMER_TYPE)
        transfersIntent.putExtra(
            TransfersActivity.CUSTOMER_WAREHOUSE_ID,
            serviceOrder?.customerWarehouseId
        )
        startActivity(transfersIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMPLETED_CODE && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private fun openNavigation() {
        openNavigation(serviceOrder?.getCustomerFullAddress(), this)
    }

    private fun openAttachments() {
        serviceOrder?.let {
            val intent = Intent(this, AttachmentsActivity::class.java)
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_CODE, it.callNumber_Code)
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_ID, it.callNumber_ID)
            startActivity(intent)
        }
    }

    private fun showAssistants() {
        serviceOrder?.let {
            val intent = Intent(this, ViewAssistantsActivity::class.java)
            intent.putExtra(Constants.EXTRA_IS_ASSIST, it.isAssist())
            intent.putExtra(Constants.EXTRA_ORDER_ID, it.callNumber_ID)
            startActivity(intent)
        }
    }

    private fun cancelDialog() {
        serviceOrder?.let {
            RetrofitRepository.RetrofitRepositoryObject.getInstance().getCancelCodes()
            val intent = Intent(this, NewCancelActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDER_ID, it.callNumber_ID)
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_CODE, it.callNumber_Code)
            intent.putExtra(Constants.EXTRA_CALL_STATUS_CODE, it.statusCode_Code?.trim() ?: "")
            startActivity(intent)
        }
    }

    private fun openWebPage(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    private fun tryToIncomplete() {
        try {
            incomplete()
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
        }
    }

    private fun incomplete() {
        if (AppAuth.getInstance().technicianUser.isNotClockedIn) {
            performClockInFollowedByAction(R.string.incomplete) {
                incomplete()
            }
            return
        }

        serviceOrder?.let { serviceOrder ->
            if (serviceOrder.isValid) {
                if (serviceOrder.canIncomplete()) {
                    val intent = Intent(this, CompleteActivity::class.java)
                    intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrder.callNumber_ID)
                    intent.putExtra(Constants.EXTRA_MASTER_CALL_ID, serviceOrder.rescheduledCallId)
                    intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, serviceOrder.equipmentId)
                    intent.putExtra(Constants.EXTRA_INCOMPLETE_MODE, true)
                    intent.putExtra(
                        Constants.EXTRA_ORDER_STATUS,
                        serviceOrder.statusCode_Code?.trim { it <= ' ' } ?: "")
                    startActivityForResult(intent, COMPLETED_CODE)
                } else {
                    showQuestionBoxWithCustomButtons(
                        getString(R.string.incomplete),
                        getString(R.string.messageWhenWaitingForAnAssistant),
                        getString(R.string.view_assistants),
                        getString(android.R.string.cancel)
                    ) { _, _ ->
                        if (AppAuth.getInstance().isConnected) showAssistants() else showUnavailableWhenOfflineMessage()
                    }
                }
            }
        }
    }

    private fun tryToComplete() {
        try {
            complete()
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
        }
    }

    private fun complete() {
        if (AppAuth.getInstance().technicianUser.isNotClockedIn) {
            performClockInFollowedByAction(R.string.complete) {
                complete()
            }
            return
        }
        serviceOrder?.let { serviceOrder ->
            if (serviceOrder.isAssist()) {
                val intent = Intent(this, CompleteActivity::class.java)
                intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrder.callNumber_ID)
                intent.putExtra(Constants.EXTRA_MASTER_CALL_ID, serviceOrder.rescheduledCallId)
                intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, serviceOrder.equipmentId)
                intent.putExtra(Constants.EXTRA_INCOMPLETE_MODE, false)
                intent.putExtra(
                    Constants.EXTRA_ORDER_STATUS,
                    serviceOrder.statusCode_Code?.trim { it <= ' ' } ?: "")
                intent.putExtra("isAssist", true)
                startActivityForResult(intent, COMPLETED_CODE)
            } else {
                if (serviceOrder.canComplete()) {
                    val intent = Intent(this, CompleteActivity::class.java)
                    intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrder.callNumber_ID)
                    intent.putExtra(Constants.EXTRA_MASTER_CALL_ID, serviceOrder.rescheduledCallId)
                    intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, serviceOrder.equipmentId)
                    intent.putExtra(Constants.EXTRA_INCOMPLETE_MODE, false)
                    intent.putExtra(
                        Constants.EXTRA_ORDER_STATUS,
                        serviceOrder.statusCode_Code?.trim { it <= ' ' } ?: "")
                    startActivityForResult(intent, COMPLETED_CODE)
                } else {
                    showQuestionBoxWithCustomButtons(
                        getString(R.string.complete),
                        getString(R.string.messageWhenWaitingForAnAssistant),
                        getString(R.string.view_assistants),
                        getString(android.R.string.cancel)
                    ) { _, _ ->
                        if (AppAuth.getInstance().isConnected) showAssistants() else showUnavailableWhenOfflineMessage()
                    }
                }
            }
        }
    }

    private fun releaseCallOffline(statusChangeModel: StatusChangeModel) {
        serviceOrder?.let { serviceOrder ->
            realm.executeTransaction { realm: Realm ->
                val incompleteRequest = IncompleteRequests(UUID.randomUUID().toString())
                incompleteRequest.requestType = "HoldRelease"
                incompleteRequest.dateAdded = Calendar.getInstance().time
                incompleteRequest.callId = serviceOrder.callNumber_ID
                incompleteRequest.requestCategory = Constants.REQUEST_TYPE.RELEASE_CALL.value
                incompleteRequest.callNumberCode = serviceOrder.callNumber_Code
                incompleteRequest.actionTime = statusChangeModel.actionTime
                incompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                incompleteRequest.callStatusCode = serviceOrder.statusCode_Code?.trim() ?: ""
                realm.insertOrUpdate(incompleteRequest)
                serviceOrder.statusCode = "Pending"
                serviceOrder.statusCode_Code = "P "
                serviceOrder.onHoldCode = ""
                serviceOrder.onHoldDescription = ""
            }
            if (AppAuth.getInstance().isConnected) {
                retryWorker(this)
            }
        }
    }

    private fun tryToArrive() {
        try {
            arrive()
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
        }
    }

    private fun arrive() {
        if (AppAuth.getInstance().technicianUser.isNotClockedIn) {
            performClockInFollowedByAction(R.string.arrive) {
                arrive()
            }
            return
        }

        serviceOrder?.let { serviceOrder ->
            val builder = AlertDialog.Builder(this)
            val dialogView = LayoutInflater.from(this).inflate(R.layout.odometer_dialog, null)
            inputDate = Calendar.getInstance().time
            builder.setView(dialogView)
            val txtOdometer = dialogView.findViewById<EditText>(R.id.txtOdometer)
            txtOdometer.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    //do nothing
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    positiveButton?.let {
                        it.isEnabled = s.isNotEmpty()
                        if (!it.isEnabled) {
                            it.setTextColor(
                                ContextCompat.getColor(baseContext, R.color.disabled_button)
                            )
                        } else {
                            it.setTextColor(
                                ContextCompat.getColor(baseContext, R.color.colorAccent)
                            )
                        }
                    }
                }

                override fun afterTextChanged(s: Editable) {
                    //do nothing
                }
            })
            val lastOdometerValue = dialogView.findViewById<TextView>(R.id.lastOdometerValue)
            val arriveTimeEditText = dialogView.findViewById<EditText>(R.id.dispatchTime)
            val arriveDateEditText = dialogView.findViewById<EditText>(R.id.dispatchDate)
            val myCalendar = Calendar.getInstance()

            if (AppAuth.getInstance().technicianUser.isAllowEditLaborRecord) {
                arriveDateEditText.visibility = View.VISIBLE
                arriveTimeEditText.visibility = View.VISIBLE
                arriveDateEditText.hint = getString(R.string.arrive_date)
                arriveTimeEditText.hint = getString(R.string.arrive_time)
            }

            val arriveDate = dateFormatter?.format(inputDate) ?: ""
            val arriveTime = timeFormatter?.format(inputDate) ?: ""
            arriveDateEditText.setText(arriveDate)
            arriveTimeEditText.setText(arriveTime)

            val time = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val date = Calendar.getInstance()
                date.time = inputDate
                myCalendar[Calendar.HOUR_OF_DAY] = hour
                myCalendar[Calendar.MINUTE] = minute
                myCalendar[Calendar.YEAR] = date[Calendar.YEAR]
                myCalendar[Calendar.MONTH] = date[Calendar.MONTH]
                myCalendar[Calendar.DAY_OF_MONTH] = date[Calendar.DAY_OF_MONTH]
                inputDate = myCalendar.time
                val time = timeFormatter?.format(inputDate) ?: ""
                arriveTimeEditText.setText(time)
            }

            val date = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val hour = Calendar.getInstance()
                hour.time = inputDate
                myCalendar[Calendar.YEAR] = year
                myCalendar[Calendar.MONTH] = monthOfYear
                myCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
                myCalendar[Calendar.HOUR_OF_DAY] = hour[Calendar.HOUR_OF_DAY]
                myCalendar[Calendar.MINUTE] = hour[Calendar.MINUTE]
                inputDate = myCalendar.time
                val date = dateFormatter?.format(inputDate) ?: ""
                arriveDateEditText.setText(date)
            }

            arriveDateEditText.setOnClickListener {
                val auxCalendar = Calendar.getInstance()
                auxCalendar.time = inputDate
                val datePickerDialog = DatePickerDialog(
                    this, android.R.style.Theme_Holo_Dialog, date,
                    auxCalendar[Calendar.YEAR], auxCalendar[Calendar.MONTH],
                    auxCalendar[Calendar.DAY_OF_MONTH]
                )
                datePickerDialog.show()
                datePickerDialog.datePicker.maxDate = Date().time
                datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            }

            arriveTimeEditText.setOnClickListener {
                val auxCalendar = Calendar.getInstance()
                auxCalendar.time = inputDate
                val timePickerDialog = TimePickerDialog(
                    this, android.R.style.Theme_Holo_Dialog,
                    time, auxCalendar[Calendar.HOUR_OF_DAY], auxCalendar[Calendar.MINUTE],
                    DateFormat.is24HourFormat(baseContext)
                )
                timePickerDialog.show()
                timePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                timePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            }

            lastOdometerValue.text = String.format(
                Locale.getDefault(),
                "Last odometer: %d",
                AppAuth.getInstance().lastOdometer
            )

            try {
                val odometerValue = AppAuth.getInstance().lastOdometer
                txtOdometer.setText(odometerValue.toString())
            } catch (ignored: java.lang.Exception) {
                Log.e(TAG, EXCEPTION, ignored)
            }

            builder.setPositiveButton(R.string.arrive, DialogInterface.OnClickListener { _, _ ->
                val odometerValue: Int = try {
                    txtOdometer.text.toString().toInt()
                } catch (e: java.lang.NumberFormatException) {
                    Log.e(TAG, EXCEPTION, e)
                    return@OnClickListener
                }
                val arriveCallTime = realm.where(ServiceCallLabor::class.java)
                    .equalTo(ServiceCallLabor.CALL_ID, serviceOrder.callNumber_ID).equalTo(
                        ServiceCallLabor.TECHNICIAN_ID,
                        AppAuth.getInstance().technicianUser.technicianNumber
                    ).findFirst()
                var dispatchDate: Date? = Date()
                if (arriveCallTime != null && serviceOrder.isAssist()) {
                    dispatchDate = arriveCallTime.dispatchTime
                } else {
                    if (serviceOrder.dispatchTime != null) {
                        dispatchDate = serviceOrder.dispatchTime
                    }
                }
                if (inputDate.before(dispatchDate)) {
                    showSimpleAlertDialog(
                        getString(R.string.warning),
                        getString(R.string.arriveTimeWarning),
                        getString(android.R.string.ok),
                        supportFragmentManager,
                        true
                    ) {
                        arrive()
                    }
                    return@OnClickListener
                }
                if (inputDate.after(Calendar.getInstance().time)) {
                    showSimpleAlertDialog(
                        getString(R.string.future_warning),
                        getString(R.string.future_explanation),
                        getString(android.R.string.ok),
                        supportFragmentManager,
                        true
                    ) {
                        arrive()
                    }
                    return@OnClickListener
                }

                // Validation odometer value shouldn't less than dispatch (last odometer value)
                if (odometerValue < AppAuth.getInstance().lastOdometer) {
                    showMessageBox(
                        getString(R.string.warning),
                        getString(R.string.warning_odometer_message)
                    ) { _, _ -> arrive() }
                    return@OnClickListener
                }
                AppAuth.getInstance().lastOdometer = odometerValue
                arriveCall(odometerValue.toDouble())
            })

            builder.setNeutralButton(R.string.cancel) { _, _ ->
                inputDate = Calendar.getInstance().time
            }

            val dialog = builder.create()
            dialog.window?.let { window ->
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                txtOdometer.requestFocus()
                txtOdometer.setSelection(txtOdometer.text.length)
            }
            dialog.show()
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        }
    }

    private fun arriveCall(odometer: Double) {
        try {
            val msgOdometer = "Odometer: $odometer"
            FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.OrderDetailActivity.ARRIVE_ACTION,msgOdometer)
            viewModel.updateProgressForActionPerformed(true)
            serviceOrder?.let { serviceOrder ->
                if (serviceOrder.isAssist()) {
                    assistChangeStatus(2)
                } else {
                    val statusChangeModel = StatusChangeModel()
                    statusChangeModel.actionTime = inputDate
                    if (odometer >= 0) {
                        statusChangeModel.odometer = odometer
                        statusChangeModel.activityCodeId =
                            serviceOrder.defaultActivityCodeId?.toInt() ?: 0
                    }
                    statusChangeModel.callId = serviceOrder.callNumber_ID
                    realm.executeTransaction { realm ->
                        val incompleteArriveRequests =
                            IncompleteRequests(UUID.randomUUID().toString())
                        incompleteArriveRequests.actionTime = inputDate
                        incompleteArriveRequests.savedOdometer = odometer
                        incompleteArriveRequests.requestType = "ArriveCall"
                        incompleteArriveRequests.dateAdded = Calendar.getInstance().time
                        incompleteArriveRequests.requestCategory =
                            Constants.REQUEST_TYPE.SERVICE_CALLS.value
                        incompleteArriveRequests.activityCodeId =
                            serviceOrder.defaultActivityCodeId?.toInt() ?: 0
                        incompleteArriveRequests.callId = serviceOrder.callNumber_ID
                        incompleteArriveRequests.status =
                            Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                        incompleteArriveRequests.callNumberCode = serviceOrder.callNumber_Code
                        incompleteArriveRequests.callStatusCode =
                            serviceOrder.statusCode_Code?.trim() ?: ""

                        realm.insertOrUpdate(incompleteArriveRequests)

                        serviceOrder.arriveTime = inputDate
                    }
                    retryWorker(this)
                    viewModel.updateProgressForActionPerformed(false)

                }
            }

        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
            viewModel.updateProgressForActionPerformed(false)
        }
    }

    private fun tryToUnDispatch() {
        val msg = "Undispatch action"
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.OrderDetailActivity.UNDISPATCH_ACTION, msg)
        if (AppAuth.getInstance().technicianUser.isNotClockedIn) {
            performClockInFollowedByAction(R.string.undispatch) {
                tryToUnDispatch()
            }
            return
        }

        serviceOrder?.let { serviceO ->
            val serviceCallLabor = realm.where(ServiceCallLabor::class.java)
                .equalTo(ServiceCallLabor.CALL_ID, serviceO.callNumber_ID).equalTo(
                    ServiceCallLabor.TECHNICIAN_ID,
                    AppAuth.getInstance().technicianUser.technicianNumber
                ).findFirst()
            val serviceCallsToBeCompleted = realm.where(IncompleteRequests::class.java).equalTo(
                IncompleteRequests.REQUEST_CATEGORY,
                Constants.REQUEST_TYPE.COMPLETE_CALL.value
            ).findAll()

            val shouldShowWarning =
                ServiceOrderRepository.shouldShowWarningOnUnDispatchByCallId(serviceO.callNumber_ID)

            val builder = AlertDialog.Builder(this)
            if (shouldShowWarning) {
                builder.setMessage(
                    getString(
                        R.string.this_will_also_delete_the_part_changes,
                        getString(R.string.message_to_confirm_undispatch)
                    )
                )
            } else {
                builder.setMessage(getString(R.string.message_to_confirm_undispatch))
            }

            builder
                .setTitle(getString(R.string.undispatch_title_for_dialog))
                .setPositiveButton(getString(R.string.undispatch)) { _, _ ->
                    try {
                        viewModel.updateProgressForActionPerformed(true)
                        serviceOrder?.let { serviceOrder ->
                            val statusChangeModel = StatusChangeModel()
                            statusChangeModel.actionTime = Date()
                            statusChangeModel.callId = serviceOrder.callNumber_ID
                            if (serviceCallsToBeCompleted.isEmpty()) {
                                clearCacheForUnDispatch()
                            }
                            realm.executeTransaction { realm ->
                                val incompleteUnDispatchRequests =
                                    IncompleteRequests(UUID.randomUUID().toString())
                                incompleteUnDispatchRequests.actionTime = Date()
                                incompleteUnDispatchRequests.savedOdometer =
                                    AppAuth.getInstance().lastOdometer.toDouble()
                                incompleteUnDispatchRequests.requestType = "UnDispatchCall"
                                incompleteUnDispatchRequests.dateAdded = Calendar.getInstance().time
                                incompleteUnDispatchRequests.activityCodeId =
                                    serviceOrder.defaultActivityCodeId?.toInt() ?: 0
                                incompleteUnDispatchRequests.callId = serviceOrder.callNumber_ID
                                incompleteUnDispatchRequests.requestCategory =
                                    Constants.REQUEST_TYPE.SERVICE_CALLS.value
                                incompleteUnDispatchRequests.status =
                                    Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                                incompleteUnDispatchRequests.callNumberCode =
                                    serviceOrder.callNumber_Code
                                incompleteUnDispatchRequests.callStatusCode =
                                    serviceOrder.statusCode_Code?.trim() ?: ""

                                realm.insertOrUpdate(incompleteUnDispatchRequests)

                                serviceOrder.dispatchTime = null
                                serviceOrder.arriveTime = null
                                serviceOrder.statusCode_Code = "P  "
                                serviceOrder.statusCode = getString(R.string.callStatusPending)
                                if (serviceCallLabor != null) {
                                    serviceCallLabor.dispatchTime = null
                                    serviceCallLabor.arriveTime = null
                                }
                                serviceOrder.statusOrder = serviceOrder.getStatusOrderForSorting()

                            }
                            retryWorker(this)
                            viewModel.updateProgressForActionPerformed(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, EXCEPTION, e)
                        viewModel.updateProgressForActionPerformed(false)
                    }
                }
                .setNeutralButton(getString(android.R.string.cancel)) { _, _ -> }

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun clearCacheForUnDispatch() {
        DatabaseRepository.getInstance().deleteLocalDataForServiceCall(serviceOrderCallNumberId)
    }

    private fun tryToSchedule() {
        try {
            schedule()
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
        }
    }

    private fun tryToHoldCall() {
        try {
            if (AppAuth.getInstance().technicianUser.isNotClockedIn) {
                performClockInFollowedByAction(R.string.hold) {
                    holdCall()
                }
            } else {
                holdCall()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }


    private fun holdCall() {
        serviceOrder?.let { serviceOrder ->
            val intent = Intent(this, OnHoldActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrder.callNumber_ID)
            intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, serviceOrder.equipmentId)
            intent.putExtra(
                Constants.EXTRA_ORDER_STATUS,
                serviceOrder.statusCode_Code?.trim { it <= ' ' } ?: "")
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_CODE, serviceOrder.callNumber_Code)
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_ID, serviceOrderCallNumberId)
            startActivity(intent)
        }
    }


    private fun schedule() {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = 12
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        val datePickerDialog = DatePickerDialog(
            this, R.style.DialogTheme,
            { _, year, month, dayOfMonth ->
                calendar[Calendar.YEAR] = year
                calendar[Calendar.MONTH] = month
                calendar[Calendar.DAY_OF_MONTH] = dayOfMonth
                showTimePicker(calendar)
            }, calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH]
        )
        datePickerDialog.show()
        datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
    }

    private fun showTimePicker(calendar: Calendar) {
        serviceOrder?.let { serviceOrder ->
            val timePickerDialog = TimePickerDialog(
                this, R.style.DialogTheme,
                { _, hourOfDay, minute ->
                    calendar[Calendar.HOUR_OF_DAY] = hourOfDay
                    calendar[Calendar.MINUTE] = minute
                    if (calendar.time.after(Calendar.getInstance().time)) {
                        val statusChangeModel = StatusChangeModel()
                        statusChangeModel.actionTime = calendar.time
                        statusChangeModel.callId = serviceOrder.callNumber_ID
                        scheduleCallOffline(statusChangeModel)
                    } else {
                        showMessageBox(
                            getString(R.string.warning),
                            getString(R.string.past_warning)
                        )
                    }
                }, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE], true
            )
            timePickerDialog.show()
        }
    }

    private fun scheduleCallOffline(statusChangeModel: StatusChangeModel) {
        viewModel.updateProgressForActionPerformed(true)
        val msg = "Schedule action"
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.OrderDetailActivity.SCHEDULE_ACTION, msg)
        serviceOrder?.let { serviceOrder ->
            realm.executeTransaction { realm: Realm ->
                val incompleteRequest = IncompleteRequests(UUID.randomUUID().toString())
                incompleteRequest.requestType = "ScheduleCall"
                incompleteRequest.dateAdded = Calendar.getInstance().time
                incompleteRequest.callId = serviceOrder.callNumber_ID
                incompleteRequest.requestCategory = Constants.REQUEST_TYPE.SCHEDULE_CALL.value
                incompleteRequest.callNumberCode = serviceOrder.callNumber_Code
                incompleteRequest.actionTime = statusChangeModel.actionTime
                incompleteRequest.callStatusCode = serviceOrder.statusCode_Code?.trim() ?: ""
                serviceOrder.estStartDate = statusChangeModel.actionTime
                incompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value

                realm.insertOrUpdate(incompleteRequest)

                serviceOrder.statusCode = "Scheduled"
                serviceOrder.statusCode_Code = "S "
            }
            retryWorker(this)
            viewModel.updateProgressForActionPerformed(false)
        }
    }

    private fun tryToDispatch() {
        try {
            dispatch()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun dispatch() {
        inputDate = Calendar.getInstance().time
        if (checkDispatchedCallsBeforeDispatch(supportFragmentManager)) {
            return
        }
        // Validation the user is not clocked in (1)
        if (AppAuth.getInstance().technicianUser.isNotClockedIn) {
            performClockInFollowedByAction(R.string.dispatch) {
                dispatch()
            }
            return
        }

        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.odometer_dialog, null)
        builder.setView(dialogView)
        val txtOdometer = dialogView.findViewById<EditText>(R.id.txtOdometer)
        txtOdometer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                positiveButton?.let {
                    it.isEnabled = s.isNotEmpty()
                    if (!it.isEnabled) {
                        it.setTextColor(
                            ContextCompat.getColor(
                                baseContext,
                                R.color.disabled_button
                            )
                        )
                    } else {
                        it.setTextColor(ContextCompat.getColor(baseContext, R.color.colorAccent))
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {
                //do nothing
            }
        })

        val lastOdometerValue = dialogView.findViewById<TextView>(R.id.lastOdometerValue)
        val dispatchTimeEditText = dialogView.findViewById<EditText>(R.id.dispatchTime)
        val dispatchDateEditText = dialogView.findViewById<EditText>(R.id.dispatchDate)
        val myCalendar = Calendar.getInstance()

        if (AppAuth.getInstance().technicianUser.isAllowEditLaborRecord) {
            dispatchDateEditText.visibility = View.VISIBLE
            dispatchTimeEditText.visibility = View.VISIBLE
        }

        val dispatchDate = dateFormatter?.format(inputDate) ?: ""
        val dispatchTime = timeFormatter?.format(inputDate) ?: ""
        dispatchDateEditText.setText(dispatchDate)
        dispatchTimeEditText.setText(dispatchTime)

        val time = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            val date = Calendar.getInstance()
            date.time = inputDate
            myCalendar[Calendar.HOUR_OF_DAY] = hour
            myCalendar[Calendar.MINUTE] = minute
            myCalendar[Calendar.YEAR] = date[Calendar.YEAR]
            myCalendar[Calendar.MONTH] = date[Calendar.MONTH]
            myCalendar[Calendar.DAY_OF_MONTH] = date[Calendar.DAY_OF_MONTH]
            inputDate = myCalendar.time
            val time = timeFormatter?.format(inputDate) ?: ""
            dispatchTimeEditText.setText(time)
        }

        val date = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val timeNow = Calendar.getInstance()
            timeNow.time = inputDate
            myCalendar[Calendar.YEAR] = year
            myCalendar[Calendar.MONTH] = monthOfYear
            myCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
            myCalendar[Calendar.HOUR_OF_DAY] = timeNow[Calendar.HOUR_OF_DAY]
            myCalendar[Calendar.MINUTE] = timeNow[Calendar.MINUTE]
            inputDate = myCalendar.time
            val date = dateFormatter?.format(inputDate) ?: ""
            dispatchDateEditText.setText(date)
        }

        dispatchDateEditText.setOnClickListener {
            val auxCalendar = Calendar.getInstance()
            auxCalendar.time = inputDate
            val datePickerDialog = DatePickerDialog(
                this, android.R.style.Theme_Holo_Dialog, date,
                auxCalendar[Calendar.YEAR], auxCalendar[Calendar.MONTH],
                auxCalendar[Calendar.DAY_OF_MONTH]
            )
            datePickerDialog.show()
            datePickerDialog.datePicker.maxDate = Date().time
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        dispatchTimeEditText.setOnClickListener {
            val auxCalendar = Calendar.getInstance()
            auxCalendar.time = inputDate
            val timePickerDialog = TimePickerDialog(
                this, android.R.style.Theme_Holo_Dialog, time, auxCalendar[Calendar.HOUR_OF_DAY],
                auxCalendar[Calendar.MINUTE], DateFormat.is24HourFormat(baseContext)
            )
            timePickerDialog.show()
            timePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            timePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        lastOdometerValue.text = String.format(
            Locale.getDefault(),
            "Last odometer: %d",
            AppAuth.getInstance().lastOdometer
        )

        try {
            val odometerValue: Int = AppAuth.getInstance().lastOdometer
            txtOdometer.setText(odometerValue.toString())
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
        }

        builder.setPositiveButton(R.string.dispatch, DialogInterface.OnClickListener { _, _ ->
            var odometerValue = 0
            try {
                odometerValue = txtOdometer.text.toString().toInt()
                serviceOrder?.let { serviceOrder ->
                    if (inputDate.before(serviceOrder.callDate)) {
                        showSimpleAlertDialog(
                            getString(R.string.warning),
                            getString(R.string.dispatchTimeWarning),
                            getString(android.R.string.ok),
                            supportFragmentManager,
                            true
                        ) {
                            dispatch()
                        }
                        return@OnClickListener
                    }
                    if (inputDate.after(Calendar.getInstance().time)) {
                        showSimpleAlertDialog(
                            getString(R.string.future_warning),
                            getString(R.string.future_explanation),
                            getString(android.R.string.ok),
                            supportFragmentManager,
                            true
                        ) {
                            dispatch()
                        }
                        return@OnClickListener
                    }
                    if (odometerValue < AppAuth.getInstance().lastOdometer) {
                        showSimpleAlertDialog(
                            getString(R.string.warning),
                            getString(R.string.warning_odometer_message),
                            getString(R.string.next),
                            supportFragmentManager,
                            false
                        ) { aBoolean: Boolean ->
                            if (aBoolean) {
                                val odometerReader = txtOdometer.text.toString().toInt()
                                if (serviceOrder.isValid) {
                                    AppAuth.getInstance().lastOdometer = odometerReader
                                }
                                val lastOdometerReading =
                                    AppAuth.getInstance().lastOdometer.toDouble()
                                try {
                                    dispatchCall(lastOdometerReading)
                                } catch (e: java.lang.Exception) {
                                    AppAuth.getInstance().lastOdometer = odometerReader
                                }
                                return@showSimpleAlertDialog
                            } else {
                                dispatch()
                                return@showSimpleAlertDialog
                            }
                        }
                        return@OnClickListener
                    }
                    if (serviceOrder.isValid) {
                        AppAuth.getInstance().lastOdometer = odometerValue
                    }

                }
            } catch (ignored: java.lang.NumberFormatException) {
                Log.e(TAG, EXCEPTION, ignored)
            }
            try {
                dispatchCall(odometerValue.toDouble())
            } catch (ignored: java.lang.Exception) {
                Log.e(TAG, EXCEPTION, ignored)
            }
        })

        builder.setNeutralButton(R.string.cancel) { _, _ ->
            inputDate = Calendar.getInstance().time
        }

        val dialog = builder.create()
        dialog.window?.let { window ->
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            txtOdometer.requestFocus()
            txtOdometer.setSelection(txtOdometer.text.length)
        }
        dialog.show()
        positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    }


    private fun dispatchCall(odometer: Double) {
        try {
            val msgOdometer = "Odometer: $odometer"
            FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.OrderDetailActivity.DISPATCH_ACTION, msgOdometer)
            viewModel.updateProgressForActionPerformed(true)
            serviceOrder?.let { serviceOrder ->
                if (serviceOrder.isAssist()) {
                    assistChangeStatus(1)
                } else {
                    val statusChangeModel = StatusChangeModel()
                    statusChangeModel.actionTime = inputDate
                    if (odometer >= 0) {
                        statusChangeModel.odometer = odometer
                        statusChangeModel.activityCodeId =
                            serviceOrder.defaultActivityCodeId?.toInt() ?: 0
                    }
                    statusChangeModel.callId = serviceOrder.callNumber_ID
                    realm.executeTransaction { realm ->
                        val incompleteRequests = IncompleteRequests(UUID.randomUUID().toString())
                        incompleteRequests.actionTime = inputDate
                        incompleteRequests.savedOdometer = odometer
                        incompleteRequests.requestType = "DispatchCall"
                        incompleteRequests.requestCategory =
                            Constants.REQUEST_TYPE.SERVICE_CALLS.value
                        incompleteRequests.dateAdded = Calendar.getInstance().time
                        incompleteRequests.activityCodeId =
                            serviceOrder.defaultActivityCodeId?.toInt() ?: 0
                        incompleteRequests.callId = serviceOrder.callNumber_ID
                        incompleteRequests.status =
                            Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                        incompleteRequests.callNumberCode = serviceOrder.callNumber_Code
                        incompleteRequests.callStatusCode =
                            serviceOrder.statusCode_Code?.trim() ?: ""

                        realm.insertOrUpdate(incompleteRequests)

                        serviceOrder.statusCode_Code = "D  "
                        serviceOrder.dispatchTime = inputDate
                        serviceOrder.arriveTime = null
                        serviceOrder.statusCode = getString(R.string.callStatusDispatched)
                        serviceOrder.statusOrder = serviceOrder.getStatusOrderForSorting()
                    }
                    retryWorker(this)
                    viewModel.updateProgressForActionPerformed(false)
                }
            }
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, EXCEPTION, ignored)
            viewModel.updateProgressForActionPerformed(false)
        }
    }

    /**
     * Will change the status for the assistant
     * 1 -> Dispatch
     * 2 -> Arrive
     */
    private fun assistChangeStatus(action: Int) {
        viewModel.updateProgressForActionPerformed(true)
        serviceOrder?.let { serviceOrder ->
            val serviceCallLabor = realm.where(ServiceCallLabor::class.java)
                .equalTo(ServiceCallLabor.CALL_ID, serviceOrder.callNumber_ID).equalTo(
                    ServiceCallLabor.TECHNICIAN_ID,
                    AppAuth.getInstance().technicianUser.technicianNumber
                ).findFirst()
            if (serviceOrder.isValid && action != UpdateLaborPostModel.ACTION_TYPE_COMPLETE) {
                realm.executeTransaction { realm: Realm ->
                    val incompleteRequestsForAssist =
                        IncompleteRequests(UUID.randomUUID().toString())
                    incompleteRequestsForAssist.assistActionType = action
                    incompleteRequestsForAssist.callId = serviceOrder.callNumber_ID
                    incompleteRequestsForAssist.technicianId =
                        AppAuth.getInstance().technicianUser.technicianNumber
                    incompleteRequestsForAssist.savedOdometer =
                        AppAuth.getInstance().lastOdometer.toDouble()
                    incompleteRequestsForAssist.activityCodeId =
                        serviceOrder.defaultActivityCodeId?.toInt() ?: 0
                    incompleteRequestsForAssist.actionTime = inputDate
                    incompleteRequestsForAssist.requestCategory =
                        Constants.REQUEST_TYPE.SERVICE_CALLS.value
                    incompleteRequestsForAssist.dateAdded = Calendar.getInstance().time
                    incompleteRequestsForAssist.requestType = "UpdateLabor"
                    incompleteRequestsForAssist.status =
                        Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                    incompleteRequestsForAssist.callNumberCode = serviceOrder.callNumber_Code
                    incompleteRequestsForAssist.callStatusCode =
                        serviceOrder.statusCode_Code?.trim() ?: ""

                    realm.insertOrUpdate(incompleteRequestsForAssist)
                }
            }
            when (action) {
                1 -> {
                    if (serviceCallLabor != null && serviceCallLabor.isValid) {
                        realm.executeTransaction {
                            serviceCallLabor.dispatchTime = inputDate
                            serviceCallLabor.arriveTime = null
                            serviceCallLabor.departureTime = null
                            serviceOrder.address = serviceOrder.address + ""
                            serviceOrder.statusOrder = serviceOrder.getStatusOrderForSorting()
                        }
                        retryWorker(this)
                        viewModel.updateProgressForActionPerformed(false)
                    }
                }
                2 -> {
                    if (serviceCallLabor != null && serviceCallLabor.isValid) {
                        realm.executeTransaction {
                            serviceCallLabor.arriveTime = inputDate
                            serviceOrder.address = serviceOrder.address + ""
                            serviceOrder.statusOrder = serviceOrder.getStatusOrderForSorting()

                        }
                        retryWorker(this)
                        viewModel.updateProgressForActionPerformed(false)
                    }
                }
            }
        }
    }


    private fun adjustButtonTextSize() {
        val currentServiceOrder =
            realm.where(ServiceOrder::class.java).equalTo("id", serviceOrderId).findFirst()
        var buttonsCount = 0
        if (currentServiceOrder != null) {
            if (currentServiceOrder.canComplete()) buttonsCount += 1
            if (currentServiceOrder.canIncomplete()) buttonsCount += 1
            if (currentServiceOrder.canUnDispatch()) buttonsCount += 1
            if (currentServiceOrder.canArrive()) buttonsCount += 1
            if (binding.btnCanonSnapshot.visibility == View.VISIBLE) buttonsCount += 1
            if (currentServiceOrder.canSchedule()) buttonsCount += 1
            if (currentServiceOrder.canDispatch()) buttonsCount += 1
            if (currentServiceOrder.canOnHold()) buttonsCount += 1
            val textSize = if (buttonsCount >= 4) 11.toFloat() else 14.toFloat()
            binding.btnComplete.textSize = textSize
            binding.btnIncomplete.textSize = textSize
            binding.btnUnDispatch.textSize = textSize
            binding.btnSchedule.textSize = textSize
            binding.btnDispatch.textSize = textSize
            binding.btnHold.textSize = textSize
            binding.btnArrive.textSize = textSize
        }
    }

    private fun showBreakLunchMessageBeforeAction(action: String) {
        if (AppAuth.getInstance().technicianUser.state == 3) {
            showEndBreakEndLunchDialog(Constants.STATUS_LUNCH_OUT, action)
        }
        if (AppAuth.getInstance().technicianUser.state == 5) {
            showEndBreakEndLunchDialog(Constants.STATUS_BRAKE_OUT, action)
        }
    }

    private fun showEndBreakEndLunchDialog(break_lunchStatus: String, action: String) {
        var breakOrLunch = "break"
        when (break_lunchStatus) {
            Constants.STATUS_BRAKE_OUT -> {
                breakOrLunch = "Break"
            }
            Constants.STATUS_LUNCH_OUT -> {
                breakOrLunch = "Lunch"
            }
        }
        val builder = AlertDialog.Builder(this)
        builder.setMessage(
            resources.getString(
                R.string.message_to_end_break_or_lunch_before_an_action,
                action,
                breakOrLunch
            )
        )
            .setTitle(getString(R.string.dialog_title_for_before_end_break_or_lunch, action))
            .setPositiveButton("End $breakOrLunch") { _, _ ->
                changeBreakOrLunchStatus(
                    break_lunchStatus,
                    action
                )
            }
            .setNeutralButton(R.string.cancel) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
    }

    private fun changeBreakOrLunchStatus(breakOrLunchStatus: String, actionToTry: String) {
        val changeStatusModel = ChangeStatusModel()
        if (MainApplication.lastLocation != null) {
            changeStatusModel.gpsLocation =
                GPSLocation.fromAndroidLocation(MainApplication.lastLocation)
        }
        realm.executeTransaction { realm: Realm ->
            val incompleteRequests = IncompleteRequests(UUID.randomUUID().toString())
            incompleteRequests.requestType = breakOrLunchStatus
            incompleteRequests.requestCategory = Constants.REQUEST_TYPE.ACTIONS.value
            incompleteRequests.dateAdded = Calendar.getInstance().time
            incompleteRequests.status = Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value

            realm.insertOrUpdate(incompleteRequests)

            AppAuth.getInstance().changeUserStatus(breakOrLunchStatus)
        }
        when (actionToTry) {
            Constants.STRING_ARRIVE -> {
                tryToArrive()
            }
            Constants.STRING_DISPATCH -> {
                tryToDispatch()
            }
            Constants.STRING_COMPLETE -> {
                tryToComplete()
            }
            Constants.STRING_INCOMPLETE -> {
                tryToIncomplete()
            }
            Constants.STRING_HOLD -> {
                tryToHoldCall()
            }
            Constants.STRING_UNDISPATCH -> {
                tryToUnDispatch()
            }
        }

        if (AppAuth.getInstance().isConnected) {
            retryWorker(this)
        }
    }

    private fun showEquipmentHistory() {
        serviceOrder?.let { serviceOrder ->
            val intent = Intent(this, EquipmentHistoryActivity::class.java)
            intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, serviceOrder.equipmentId)
            startActivity(intent)
        }
    }

    private fun changeEquipmentDetails(type: Int) {
        serviceOrder?.let { serviceOrder ->
            val currentValue: String
            val title: String
            val builder = AlertDialog.Builder(this)

            val dialogView = LayoutInflater.from(this).inflate(R.layout.update_details_dialog, null)

            builder.setView(dialogView)
            val txtValue = dialogView.findViewById<EditText>(R.id.txtValue)
            when (type) {
                EDIT_IP -> {
                    title = "Enter IP Address"
                    currentValue = serviceOrder.ipAddress ?: ""
                    txtValue.keyListener = DigitsKeyListener.getInstance("0123456789.")
                }
                EDIT_MAC -> {
                    title = "Enter MAC Address"
                    currentValue = serviceOrder.macAddress ?: ""
                }
                EDIT_REMARKS -> {
                    title = "Enter Location Remarks"
                    currentValue = serviceOrder.equipmentRemarks ?: ""
                    txtValue.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
                else -> return
            }

            txtValue.setText(currentValue)

            builder.setTitle(title)

            builder.setPositiveButton(R.string.ok) { _, _ ->
                val newValue: String = txtValue.text.toString()
                updateDetails(newValue, type)
            }

            builder.setNegativeButton(R.string.cancel, null)

            val dialog = builder.create()
            dialog.window?.let { window ->
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                txtValue.requestFocus()
                txtValue.setSelection(txtValue.text.length)
            }
            dialog.show()
        }
    }

    private fun updateDetails(newValue: String, type: Int) {
        serviceOrder?.let { serviceOrder ->
            val existingUpdateDetailEntry = DatabaseRepository.getInstance()
                .getServiceCallDetails(serviceOrder.callNumber_Code, type)
            if (existingUpdateDetailEntry != null) {
                realm.executeTransaction {
                    existingUpdateDetailEntry.newValue = newValue
                    if (serviceOrder.isValid) {
                        when (existingUpdateDetailEntry.itemType) {
                            EDIT_IP -> {
                                serviceOrder.ipAddress = newValue
                            }
                            EDIT_MAC -> {
                                serviceOrder.macAddress = newValue
                            }
                            EDIT_REMARKS -> {
                                serviceOrder.equipmentRemarks = newValue
                            }
                        }
                    }
                }
            } else {
                realm.executeTransaction { realm: Realm ->
                    val incompleteUpdateDetails = IncompleteRequests(UUID.randomUUID().toString())
                    incompleteUpdateDetails.requestType = "UpdateItemDetails"
                    incompleteUpdateDetails.dateAdded = Calendar.getInstance().time
                    incompleteUpdateDetails.equipmentId = serviceOrder.equipmentId
                    incompleteUpdateDetails.callId = serviceOrder.callNumber_ID
                    incompleteUpdateDetails.requestCategory =
                        Constants.REQUEST_TYPE.UPDATE_DETAILS.value
                    incompleteUpdateDetails.status =
                        Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                    incompleteUpdateDetails.callNumberCode = serviceOrder.callNumber_Code
                    incompleteUpdateDetails.newValue = newValue
                    incompleteUpdateDetails.itemType = type

                    realm.insertOrUpdate(incompleteUpdateDetails)

                    if (serviceOrder.isValid) {
                        when (type) {
                            EDIT_IP -> {
                                serviceOrder.ipAddress = newValue
                            }
                            EDIT_MAC -> {
                                serviceOrder.macAddress = newValue
                            }
                            EDIT_REMARKS -> {
                                serviceOrder.equipmentRemarks = newValue
                            }
                        }
                    }
                }
            }
            retryWorker(this)
        }
    }

    private fun refreshJob() {
        try {
            observeUpdateForOneServiceCall()
            initWarehouseList()
        } catch (e: Exception) {
            Log.d(TAG, EXCEPTION, e)
        }
    }

    private fun callContact(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        val phoneToCall: String = getOnlyNumbersBeforeDialing(phone)
        intent.data = Uri.parse("tel:$phoneToCall")
        startActivity(intent)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextView) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
        ErrorHandler.get().removeListener(this)
    }

    override fun onListenError(
        error: Pair<ErrorType, String?>?, requestType: String, callId: Int, data: String
    ) {
        if (callId != serviceOrderCallNumberId) return
        when (requestType) {
            "DispatchCall",
            "ArriveCall",
            "UnDispatchCall",
            "HoldRelease",
            "ScheduleCall",
            "OnHoldCall",
            "UpdateLabor",
            "UpdateItemDetails" -> showOfflineRequestError(error, callId)
            "SERVICE_CALL_REASSIGNED" -> finish()
            "SERVICE_CALL_CANCELED_OR_DELETED" -> finish()
        }
    }

    private fun showOfflineRequestError(error: Pair<ErrorType, String?>?, callId: Int) {
        if (callId != serviceOrderCallNumberId) return
        showNetworkErrorDialog(error, this, supportFragmentManager)
    }

    private fun performClockInFollowedByAction(message: Int, action: () -> Unit) {
        showQuestionBoxWithCustomButtons(
            getString(message),
            getString(R.string.clock_in_question),
            getString(R.string.clock_in),
            getString(R.string.cancel)
        ) { _, _ ->
            ClockOutHelper.performClockIn(getString(R.string.clock_in),
                supportFragmentManager,
                this,
                this,
                onLoading = {
                    showProgressBar()
                },
                onSuccess = {
                    hideProgressBar()
                    action()
                },
                onError = { _, _, pair ->
                    hideProgressBar()
                    showNetworkErrorDialog(pair, this, supportFragmentManager)
                },
                onDisconnected = {
                    hideProgressBar()
                    showUnavailableWhenOfflineMessage()
                },
                onCancel = {
                    hideProgressBar()
                })
        }
    }

    private fun initWarehouseList() {
        if (AppAuth.getInstance().technicianUser.isAllowWarehousesTransfers) {
            RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllWarehouses().observe(
                this, { genericDataResponse ->
                    when (genericDataResponse.responseType) {
                        RequestStatus.SUCCESS -> {
                            if (genericDataResponse.data?.isHasError == false) {
                                warehouseList =
                                    TransferViewModel().retrieveWarehouseList(genericDataResponse.data)
                            }
                        }
                        else -> {
                            Log.d(TAG, "Error on warehouse init")
                        }
                    }
                }
            )
        }
    }

    private fun hasCustomerWarehouse(): Boolean {
        return warehouseList?.any { warehouse -> warehouse.warehouseID == serviceOrder?.customerWarehouseId }
            ?: false
    }


}