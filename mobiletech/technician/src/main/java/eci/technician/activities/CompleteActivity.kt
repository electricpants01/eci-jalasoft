package eci.technician.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.android.gms.common.util.Base64Utils
import com.google.gson.JsonElement
import eci.technician.BaseActivity
import eci.technician.MainActivity
import eci.technician.R
import eci.technician.activities.allparts.AllPartsViewModel
import eci.technician.activities.allparts.NeededPartsFragment
import eci.technician.activities.allparts.PendingPartsFragment
import eci.technician.activities.allparts.UsedPartsFragment
import eci.technician.activities.notes.NotesActivity
import eci.technician.activities.problemCodes.ProblemCodesSearchActivity
import eci.technician.activities.repairCode.RepairCodeSearchActivity
import eci.technician.adapters.*
import eci.technician.adapters.IncompleteCodeAdapter.IncompleteCodeAdapterListener
import eci.technician.adapters.MetersAdapter.HideKeyboardListener
import eci.technician.databinding.ActivityCompleteBinding
import eci.technician.dialog.DialogManager.showSimpleAlertDialog
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.TextValidationHelper
import eci.technician.helpers.api.ApiHelperBuilder
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.IUsedProblemCodeListener
import eci.technician.interfaces.IUsedRepairCodeListener
import eci.technician.models.UpdateLaborPostModel
import eci.technician.models.data.UsedPart
import eci.technician.models.data.UsedProblemCode
import eci.technician.models.data.UsedRepairCode
import eci.technician.models.order.*
import eci.technician.repository.CompletedCallsRepository
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import eci.technician.viewmodels.CompleteViewModel
import eci.technician.workers.OfflineManager
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

class CompleteActivity : BaseActivity(), HideKeyboardListener,
    IncompleteCodeAdapterListener, EmailSummaryAdapter.EmailSummaryListener,
    IUsedProblemCodeListener, IUsedRepairCodeListener {

    lateinit var binding: ActivityCompleteBinding

    private var orderId = 0
    private var masterCallId = 0
    private var equipmentId = 0
    private var orderStatus: String? = null
    private var incompleteMode = false
    private var meters = mutableListOf<EquipmentMeter>()
    private lateinit var activityCodesListAdapter: ActivityCodesListAdapter
    private lateinit var emailSummaryAdapter: EmailSummaryAdapter
    private var emailList = mutableListOf<String>()
    private var currentStep = 1

    private var isAssist = false
    private var currentServiceOrder: ServiceOrder? = null
    private var currentIncompleteCode: IncompleteCode? = null
    lateinit var serviceCallTemporalData: ServiceCallTemporalData
    private val viewModel by lazy {
        ViewModelProvider(this)[CompleteViewModel::class.java]
    }
    val viewModelParts: AllPartsViewModel by viewModels()
    lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setContentView(R.layout.activity_complete)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_complete)
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.COMPLETE_ACTIVITY)
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getServiceCallProblemCodes()
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getServiceCallRepairCodes()
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllActivityCallTypes()


        val connection = NetworkConnection(baseContext)
        setSupportActionBar(binding.toolbar)
        if (isTablet(baseContext)) {
            fixSignatureRotation()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        if (!intent.hasExtra(Constants.EXTRA_ORDER_ID)) {
            finish()
            return
        }

        orderId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        orderStatus = intent.getStringExtra(Constants.EXTRA_ORDER_STATUS)
        equipmentId = intent.getIntExtra(Constants.EXTRA_EQUIPMENT_ID, 0)
        masterCallId = intent.getIntExtra(Constants.EXTRA_MASTER_CALL_ID, 0)
        incompleteMode = intent.getBooleanExtra(Constants.EXTRA_INCOMPLETE_MODE, false)


        viewModelParts.allowChangePartStatus = true
        viewModelParts.currentCallId = orderId


        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .getEquipmentMetersByEquipmentId(equipmentId)

        // If the is an assistant technician
        isAssist = intent.getBooleanExtra("isAssist", false)

        if (incompleteMode) {
            setTitle(R.string.incomplete)
            binding.btnComplete.setText(R.string.incomplete)
        }

        connection.observe(this, Observer { t ->
            t?.let {
                if (it) {
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {/*not used*/
                        }

                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                }
            }
        })


        binding.btnAddProblemCode.setOnClickListener {
            val intent = Intent(this, ProblemCodesSearchActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDER_ID, orderId)
            startActivity(intent)
        }

        currentServiceOrder =
            realm.where(ServiceOrder::class.java).equalTo("callNumber_ID", orderId)
                .findFirst()

        serviceCallTemporalData =
            DatabaseRepository.getInstance().getServiceCallTemporaryData(orderId)

        realm.executeTransaction {
            currentServiceOrder?.let {
                it.departTime = Calendar.getInstance().time
            }
        }

        setActivityCode(mutableListOf())
        DatabaseRepository.getInstance().activityCodes.observe(this, Observer {
            setActivityCode(it)
        })

        binding.btnAddResolutionCode.setOnClickListener {
            val intent = Intent(this, RepairCodeSearchActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDER_ID, orderId)
            startActivity(intent)
        }

        binding.btnComplete.setOnClickListener {
            if (isAssist) {
                if (AppAuth.getInstance().isConnected) {
                    completeLaborForAssist()
                } else {
                    showUnavailableWhenOfflineMessage()
                }
            } else {
                if (incompleteMode) {
                    incompleteOffline()
                } else {

                    offlineComplete()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            hideKeyboard(this)
            changeStep(-1)
        }

        binding.btnNext.setOnClickListener {
            changeStep(1)
            hideKeyboard(this)
        }

        currentServiceOrder?.let { currentServiceOrder ->
            if (AppAuth.getInstance().technicianUser.isAllowEditLaborRecord && !currentServiceOrder.isAssist()) {
                binding.dispatchTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorHeadingText
                    )
                )
                binding.arriveTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorHeadingText
                    )
                )
                binding.departureTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorHeadingText
                    )
                )
                binding.dispatchTimeBtn.setOnClickListener { showCalendarAndClock(1) }
                binding.arriveTimeBtn.setOnClickListener { showCalendarAndClock(2) }
                binding.departureTimeBtn.setOnClickListener { showCalendarAndClock(3) }
            }
        }

        binding.recProblemCodes.layoutManager = SafeLinearLayoutManager(this)

        DatabaseRepository.getInstance().getUsedProblemCodes(orderId).observe(this, Observer {
            binding.recProblemCodes.adapter = ProblemCodesAdapter(it, this)
        })

        binding.recResolutionCodes.layoutManager = SafeLinearLayoutManager(this)

        DatabaseRepository.getInstance().getUsedRepairCodes(orderId).observe(this, {
            binding.recResolutionCodes.adapter = RepairCodesAdapter(it, this)
        })

        binding.item = currentServiceOrder
        binding.technician = AppAuth.getInstance().technicianUser

        initIncompleteCodes()

        if (!isAssist) {
            val equipmentMeters = DatabaseRepository.getInstance().getEquipmentMeters(equipmentId)
            if (equipmentMeters.isNotEmpty()) {
                equipmentMeters.forEach {
                    val equipmentMeterFromDatabase = EquipmentMeter()
                    equipmentMeterFromDatabase.actual = it.actual
                    equipmentMeterFromDatabase.display = it.display
                    equipmentMeterFromDatabase.equipmentId = it.equipmentId
                    equipmentMeterFromDatabase.initialDisplay = it.initialDisplay
                    equipmentMeterFromDatabase.isMeterSet = it.isMeterSet
                    equipmentMeterFromDatabase.isValidMeter = it.isValidMeter
                    equipmentMeterFromDatabase.isRequired = it.isRequired
                    equipmentMeterFromDatabase.isRequiredMeterOnServiceCalls =
                        it.isRequiredMeterOnServiceCalls
                    equipmentMeterFromDatabase.meterId = it.meterId
                    equipmentMeterFromDatabase.meterType = it.meterType
                    equipmentMeterFromDatabase.meterTypeId = it.meterTypeId
                    equipmentMeterFromDatabase.userLastMeter = it.userLastMeter
                    meters.add(equipmentMeterFromDatabase)
                }
                updateMetersOnScreen()
            } else {
                binding.recMeters.layoutManager = SafeLinearLayoutManager(this)
                binding.recMeters.adapter = MetersAdapter()
            }
        }

        val realm1 = Realm.getDefaultInstance()
        val serviceCallProperty = realm1.where(ServiceCallProperty::class.java)
            .equalTo("serviceCallId", orderId)
            .findFirst()
        if (serviceCallProperty != null) {
            binding.txtCompleteRemarks.setText(serviceCallProperty.comments)
        }
        realm1.close()

        initScreen()

        binding.btnClearSign.setOnClickListener { binding.signPad.clear() }


        binding.signPad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {/*not used*/
            }

            override fun onSigned() {
                binding.dispatchTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this@CompleteActivity,
                        R.color.disabled_button
                    )
                )
                binding.arriveTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this@CompleteActivity,
                        R.color.disabled_button
                    )
                )
                binding.departureTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this@CompleteActivity,
                        R.color.disabled_button
                    )
                )
                binding.dispatchTimeBtn.isEnabled = false
                binding.arriveTimeBtn.isEnabled = false
                binding.departureTimeBtn.isEnabled = false
                realm.executeTransaction {
                    val signatureBitmap = binding.signPad.signatureBitmap
                    val bos = ByteArrayOutputStream()
                    signatureBitmap?.compress(Bitmap.CompressFormat.JPEG, 0, bos)
                    val bytes = bos.toByteArray()
                    serviceCallTemporalData.signatureByteArray = bytes
                }
            }

            override fun onClear() {
                binding.dispatchTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this@CompleteActivity,
                        R.color.colorHeadingText
                    )
                )
                binding.arriveTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this@CompleteActivity,
                        R.color.colorHeadingText
                    )
                )
                binding.departureTimeBtn.setTextColor(
                    ContextCompat.getColor(
                        this@CompleteActivity,
                        R.color.colorHeadingText
                    )
                )
                binding.dispatchTimeBtn.isEnabled = true
                binding.arriveTimeBtn.isEnabled = true
                binding.departureTimeBtn.isEnabled = true
                realm.executeTransaction {
                    val bytes = ByteArrayOutputStream().toByteArray()
                    serviceCallTemporalData.signatureByteArray = bytes
                }
            }
        })

        setupSignatureName()

        binding.recMeters.setOnClickListener { hideKeyboard(this) }

        binding.txtCompleteRemarks.setOnClickListener {
            binding.txtCompleteRemarks.isFocusable = true
            binding.txtCompleteRemarks.isFocusableInTouchMode = true
            binding.txtCompleteRemarks.requestFocus()
        }

        binding.preventiveMaintenanceContainer.visibility =
            if (isAssist) View.GONE else View.VISIBLE
        binding.preventiveMaintenanceSwitch.let {
            it.isChecked = serviceCallTemporalData.preventiveMaintenance
            it.setOnCheckedChangeListener { _, isChecked ->
                realm.executeTransaction {
                    serviceCallTemporalData.preventiveMaintenance = isChecked
                }
            }
        }


        if (serviceCallTemporalData.signatureByteArray != null) {
            val signatureBitmap = serviceCallTemporalData.signatureByteArray?.size?.let {
                BitmapFactory.decodeByteArray(
                    serviceCallTemporalData.signatureByteArray,
                    0,
                    it
                )
            }
            if (signatureBitmap != null) {
                binding.signPad.signatureBitmap = signatureBitmap
            }
        }


        binding.recEmails.layoutManager = LinearLayoutManager(this)

        emailSummaryAdapter = EmailSummaryAdapter(emailList, this)
        binding.recEmails.adapter = emailSummaryAdapter
        binding.emailListEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {/*not used*/
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {/*not used*/
            }

            override fun afterTextChanged(s: Editable) {
                if (s.isEmpty()) return
                if (s.last() == ',' || s.last() == ';') {
                    val localEmailList = s.toString().split(',', ';')
                    if (localEmailList.size - 1 + emailList.size > Constants.EMAIL_LIST_LIMIT) {
                        binding.recEmails.adapter?.notifyDataSetChanged()
                        showMessageBox(
                            getString(R.string.email_limit_title),
                            getString(R.string.email_limit_message)
                        )
                        return
                    }
                    createEmailList(s)
                }
            }
        })


        binding.emailListEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.emailListEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)

        currentServiceOrder?.contactEmail?.let {
            if (!it.isNullOrEmpty()) {
                emailList.add(it)
            }
        }

        binding.emailListEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                if (emailList.size < Constants.EMAIL_LIST_LIMIT) {
                    createEmailList(textView.text)
                } else {
                    binding.emailListEditText.setText(textView.text)
                    showMessageBox(
                        getString(R.string.email_limit_title),
                        getString(R.string.email_limit_message)
                    )
                }
                return@setOnEditorActionListener false
            }
            return@setOnEditorActionListener true
        }

        binding.emailListSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                binding.emailUILinearLayout.visibility = View.VISIBLE
            } else {
                binding.emailUILinearLayout.visibility = View.GONE
            }
        }

        prepareNotesView()
        loadDescription()

        if (isAssist) {
            currentStep = USED_PARTS_STEP_3
            initScreen()
        }
    }

    private fun loadDescription() {
        var serviceCallTemporalData = DatabaseRepository
            .getInstance()
            .getServiceCallTemporaryDataImproved(orderId)

        val desc = currentServiceOrder?.description
        if (serviceCallTemporalData.description?.isEmpty() == true) {
            binding.txtProblemDescription.setText(serviceCallTemporalData.description)
            return
        }
        if (serviceCallTemporalData.description == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                desc?.let {
                    CompletedCallsRepository.saveDescriptionInServiceCallTemporalData(orderId, it)
                    withContext(Dispatchers.Main) {
                        binding.txtProblemDescription.setText(serviceCallTemporalData.description)
                    }
                }
            }
        } else {
            binding.txtProblemDescription.setText(serviceCallTemporalData.description)
        }
    }

    private fun prepareNotesView() {
        if (!AppAuth.getInstance().technicianUser.isAllowServiceCallNotes) {
            binding.containerNotes.visibility = View.GONE
            return
        }
        binding.containerNotes.visibility = View.VISIBLE
        binding.btnAddNotes.setOnClickListener {
            currentServiceOrder?.let {
                val notesIntent = Intent(this, NotesActivity::class.java)
                notesIntent.putExtra(NotesActivity.EXTRA_CALL_ID, it.callNumber_ID)
                notesIntent.putExtra(NotesActivity.EXTRA_CALL_NUMBER_CODE, it.callNumber_Code)
                startActivity(notesIntent)
            }
        }

    }

    private fun createEmailList(emailChain: CharSequence): Boolean {
        binding.emailListEditText.setText("")
        val unfilteredEmails = emailChain.split(",", ";").map { it.trim() }
        val emails = unfilteredEmails.filter { it.isNotEmpty() }
        val invalidEmails = mutableListOf<String>()
        val duplicatedEmails = mutableListOf<String>()
        var res = true
        var errorEmailsString = ""
        if (emails.size - 1 + emailList.size >= Constants.EMAIL_LIST_LIMIT) {
            binding.emailListEditText.setText(emailChain)
            showMessageBox(
                getString(R.string.email_limit_title),
                getString(R.string.email_limit_message)
            )
            return false
        }

        emails.forEach {
            if (TextValidationHelper.isEmailValid(it)) {
                if (emailList.size >= Constants.EMAIL_LIST_LIMIT) {
                    binding.emailListEditText.setText(emailChain)
                    binding.recEmails.adapter?.notifyDataSetChanged()
                    showMessageBox(
                        getString(R.string.email_limit_title),
                        getString(R.string.email_limit_message)
                    )
                    return false
                }
                if (emailList.contains(it)) {
                    duplicatedEmails.add(it)
                } else {
                    emailList.add(it)
                }
            } else {
                if (it.isNotBlank()) {
                    invalidEmails.add(it)
                }
            }
        }


        if (duplicatedEmails.isNotEmpty()) {
            val duplicatedEmailString =
                if (duplicatedEmails.size > 1) duplicatedEmails.joinToString { it } else duplicatedEmails.first()
            errorEmailsString += duplicatedEmailString
            showMessageBox("", getString(R.string.email_already_exists, duplicatedEmailString))
            res = false
        }
        if (invalidEmails.isNotEmpty()) {
            val invalidMailString =
                if (invalidEmails.size > 1) invalidEmails.joinToString { it } else invalidEmails.first()
            if (errorEmailsString.isNotEmpty()) {
                errorEmailsString += ",$invalidMailString"
            } else {
                errorEmailsString += invalidMailString
            }
            showMessageBox("", getString(R.string.invalid_email_format, invalidMailString))
            res = false
        }
        binding.emailListEditText.setText(errorEmailsString)
        binding.recEmails.adapter?.notifyDataSetChanged()
        return res
    }

    private fun fixSignatureRotation() {
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = resources.displayMetrics.density
        val dpHeight = outMetrics.heightPixels / density
        val dpWidth = outMetrics.widthPixels / density
        var lowest: Float
        lowest = if (dpHeight < dpWidth) {
            dpHeight
        } else {
            dpWidth
        }
        lowest -= 32
        val scale = resources.displayMetrics.density
        val pixels = (lowest * scale + 0.5f).toInt()
        binding.signPad.layoutParams.width = pixels
    }

    private fun isTablet(context: Context): Boolean {
        return ((context.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isTablet(baseContext)) {
            fixSignatureRotation()
        }
    }

    /**
     * setupSignatureName() will ask if there is a name saved locally
     * - Saves the name while typing
     */
    private fun setupSignatureName() {
        val signatureName = binding.txtTypeName
        if (serviceCallTemporalData.signatureName != null) {
            signatureName.setText(serviceCallTemporalData.signatureName)
        }
        signatureName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {/*not used*/
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {/*not used*/
            }

            override fun afterTextChanged(s: Editable) {
                realm.beginTransaction()
                serviceCallTemporalData.signatureName = s.toString()
                realm.commitTransaction()
            }
        })
    }

    private fun setActivityCode(items: MutableList<ActivityCode>) {
        activityCodesListAdapter = ActivityCodesListAdapter(items)
        binding.spinActivityCodes.adapter = activityCodesListAdapter
        if (serviceCallTemporalData.activityCodeId != null) {
            for (i in 0 until binding.spinActivityCodes.count) {
                val countActivityCode =
                    binding.spinActivityCodes.getItemAtPosition(i) as ActivityCode
                if (countActivityCode.activityCodeId == serviceCallTemporalData.activityCodeId) {
                    binding.spinActivityCodes.setSelection(i)
                    break
                }
            }
        } else {
            setDefaultActivityCode()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun initIncompleteCodes() {
        realm.executeTransaction { realm ->
            currentServiceOrder?.let {
                val serviceCallTemporalData = realm
                    .where(ServiceCallTemporalData::class.java)
                    .equalTo(ServiceCallTemporalData.COLUMNS.CALL_NUMBER_ID, it.callNumber_ID)
                    .findFirst()
                if (serviceCallTemporalData != null) {
                    val incompleteCodeToEdit = realm
                        .where(IncompleteCode::class.java)
                        .equalTo(
                            IncompleteCode.COLUMNS.INCOMPLETE_CODE_ID,
                            serviceCallTemporalData.incompleteCodeId?.toInt()
                                ?: 0
                        )
                        .findFirst()
                    if (incompleteCodeToEdit != null) {
                        currentIncompleteCode = incompleteCodeToEdit
                        incompleteCodeToEdit.isChecked = true
                    }
                }
            }
        }
        viewModel.getIncompleteCodes().observe(this, Observer {
            val incompleteCodeAdapter = IncompleteCodeAdapter(it, this)
            binding.incompleteCodeRecyclerView.adapter = incompleteCodeAdapter
            binding.layIncompleteCode.visibility = if (incompleteMode) View.VISIBLE else View.GONE
            binding.incompleteCodeRecyclerView.layoutManager = LinearLayoutManager(this)
            (binding.incompleteCodeRecyclerView.adapter as IncompleteCodeAdapter).notifyDataSetChanged()
        })
    }

    private fun updateMetersOnScreen() {
        binding.recMeters.layoutManager = SafeLinearLayoutManager(this)
        binding.recMeters.adapter = MetersAdapter(meters.toTypedArray(), this, orderId)
    }

    override fun onResume() {
        super.onResume()

        setPendingPartsFragment()
        setUsedPartsFragment()
        setNeededPartsFragment()
        observeOneServiceCall(viewModelParts.currentCallId)

        binding.recProblemCodes.adapter?.let {
            it.notifyDataSetChanged()
        }
        binding.recResolutionCodes.adapter?.let {
            it.notifyDataSetChanged()
        }

        hideKeyboard(this)
    }

    private fun observeOneServiceCall(currentCallId: Int) {
        DatabaseRepository.getInstance().getServiceOrderLiveDataByNumberId(currentCallId)
            .observe(this) { serviceOrders: RealmResults<ServiceOrder?> ->
                if (!serviceOrders.isEmpty()) {
                    val firstServiceOrder = serviceOrders.first()
                    if (firstServiceOrder != null && firstServiceOrder.isValid) {
                        viewModelParts.updateServiceOrderStatus(firstServiceOrder.callNumber_ID)
                    } else {
                        // finish()
                    }
                } else {
                    // finish()
                }
            }
    }

    private fun setNeededPartsFragment() {
        supportFragmentManager.commit {
            replace(binding.containerNeededComplete.id, NeededPartsFragment())
        }
        supportFragmentManager.commit {
            replace(binding.containerNeededPartsIncomplete.id, NeededPartsFragment())
        }
    }

    private fun setUsedPartsFragment() {
        supportFragmentManager.commit {
            replace(binding.containerUsedComplete.id, UsedPartsFragment())
        }
    }

    private fun setPendingPartsFragment() {
        supportFragmentManager.commit {
            replace(binding.containerPendingComplete.id, PendingPartsFragment())
        }
    }

    private fun completeLaborForAssist() {
        val progressDialog = ProgressDialog.show(this, "", getString(R.string.loading))
        val updateLaborPostModel = UpdateLaborPostModel.createInstanceForComplete(orderId)
        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .updateLaborForAssist(this, updateLaborPostModel, orderId).observe(this, Observer {
                try {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                }
                when (it.responseType) {
                    RequestStatus.SUCCESS -> {
                        viewModel.deleteParts(orderId)
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    RequestStatus.ERROR -> {
                        showMessageBox(
                            it.onError?.title ?: getString(R.string.somethingWentWrong),
                            it.onError?.description ?: getString(R.string.error)
                        )
                    }
                    else -> {
                        showMessageBox(
                            getString(R.string.somethingWentWrong),
                            getString(R.string.error)
                        )
                    }
                }
            })
    }


    private fun changeStep(direction: Int) {
        saveUsedActivityCode()
        when (currentStep) {
            PROBLEM_REPAIR_CODE_STEP_1 -> {
                changeStepFromStep1(direction)
            }
            REMARKS_STEP_2 -> {
                changeStepFromStep2(direction)
            }
            USED_PARTS_STEP_3 -> {
                changeStepFromStep3(direction)
            }
            INCOMPLETE_CODE_STEP_4 -> {
                changeStepFromStep4(direction)
            }
            NEED_PARTS_STEP_5 -> {
                changeStepFromStep5(direction)
            }
            DESCRIPTION_STEP_6 -> {
                changeStepFromStep6(direction)
            }
            SUMMARY_STEP_7 -> {
                changeStepFromStep7(direction)
            }
        }
    }

    private fun changeStepFromStep1(direction: Int) {
        if (direction > 0) {
            if (checkRequiredFields()) {
                currentStep = REMARKS_STEP_2
                initScreen()
            } else {
                showMessageBox(
                    getString(R.string.required_title),
                    getString(R.string.please_fill_required_fields)
                )
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun changeStepFromStep7(direction: Int) {
        if (direction > 0) {
            // do nothing
        } else {
            currentStep = if (incompleteMode) DESCRIPTION_STEP_6 else USED_PARTS_STEP_3
            initScreen()
        }
    }

    private fun changeStepFromStep6(direction: Int) {
        if (direction > 0) {
            currentStep = SUMMARY_STEP_7
        } else {
            if (currentIncompleteCode != null) {
                currentIncompleteCode?.let {
                    if (it.category != null) {
                        currentStep = if (it.category == "NP") {
                            NEED_PARTS_STEP_5
                        } else {
                            INCOMPLETE_CODE_STEP_4
                        }
                    }
                }
            } else {
                currentStep = INCOMPLETE_CODE_STEP_4
            }
        }
        initScreen()
    }

    private fun changeStepFromStep5(direction: Int) {
        if (direction > 0) {
            lifecycleScope.launch(Dispatchers.IO) {
                val neededParts = PartsRepository.getAllLocalNeededPartsByOrderId(orderId).size
                withContext(Dispatchers.Main) {
                    if (incompleteMode && neededParts <= 0) {
                        showSimpleAlertDialog(
                            "",
                            getString(R.string.no_needed_parts),
                            getString(android.R.string.ok),
                            supportFragmentManager,
                            true
                        ) { }
                    } else {
                        currentStep = DESCRIPTION_STEP_6
                        initScreen()
                    }
                }
            }
        } else {
            currentStep = INCOMPLETE_CODE_STEP_4
            initScreen()
        }
    }

    private fun changeStepFromStep4(direction: Int) {
        if (direction > 0) {
            if (currentIncompleteCode != null) {
                currentIncompleteCode?.let {
                    if (it.category == "NP") {
                        currentStep = NEED_PARTS_STEP_5
                    }
                    it.category?.let { category ->
                        if (category.trim { it <= ' ' } == "O") {
                            currentStep = DESCRIPTION_STEP_6
                        }
                    }
                    initScreen()
                }
            } else {
                showMessageBox(
                    "",
                    getString(R.string.incomplete_code_message_for_unselected)
                )
            }
        } else {
            currentStep = USED_PARTS_STEP_3
            initScreen()
        }
    }

    private fun changeStepFromStep3(direction: Int) {
        if (direction > 0) {
            if (isAssist) {
                initScreen()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    val canComplete =
                        PartsRepository.canPassPartsStepBeforeComplete(orderId)
                    withContext(Dispatchers.Main) {
                        if (canComplete) {
                            if (incompleteMode) {
                                currentStep = INCOMPLETE_CODE_STEP_4
                                initScreen()
                            } else {
                                currentStep = SUMMARY_STEP_7
                                initScreen()
                            }
                        } else {
                            showMessageBox(
                                resources.getString(R.string.pending_items),
                                resources.getString(R.string.pending_needed_items_instructions)
                            )
                        }
                    }
                }
            }
        } else {
            if (isAssist) {
                // do nothing
            } else {
                currentStep = REMARKS_STEP_2
                initScreen()
            }
        }
    }

    private fun changeStepFromStep2(direction: Int) {
        if (direction > 0) {
            if (checkRequiredFields()) {
                currentStep = USED_PARTS_STEP_3
                initScreen()
            } else {
                showMessageBox(
                    getString(R.string.required_title),
                    getString(R.string.please_fill_required_fields)
                )
            }
        } else {
            currentStep = PROBLEM_REPAIR_CODE_STEP_1
            initScreen()
        }
    }

    private fun saveUsedActivityCode() {
        val item = activityCodesListAdapter.getItem(binding.spinActivityCodes.selectedItemPosition)
        if (item != null) {
            realm.beginTransaction()
            serviceCallTemporalData.activityCodeId = item.activityCodeId
            realm.commitTransaction()
        }
    }

    private fun checkRequiredFields(): Boolean {
        return when (currentStep) {
            1 -> checkStep1()
            2 -> checkStep2()
            3 -> checkStep3()
            4 -> checkStep4()
            5 -> checkStep5()
            7 -> checkStep7()
            else -> true
        }
    }

    private fun setDefaultActivityCode() {
        currentServiceOrder?.let {
            val defaultActivity = DatabaseRepository.getInstance()
                .getActivityCodeById((it.defaultActivityCodeId ?: "0").toInt())
            if (defaultActivity != null) {
                for (i in 0 until binding.spinActivityCodes.count) {
                    if (binding.spinActivityCodes.getItemAtPosition(i)
                            .toString() == defaultActivity.toString()
                    ) {
                        binding.spinActivityCodes.setSelection(i)
                        break
                    }
                }
            }
        }
    }

    private fun showCalendarAndClock(operation: Int) {
        val myCalendar = Calendar.getInstance()
        when (operation) {
            1 -> {
                val datePickerDialogDispatch = DatePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Dialog,
                    DatePickerDialog.OnDateSetListener { datePicker, year, monthOfYear, dayOfMonth ->
                        myCalendar[Calendar.YEAR] = year
                        myCalendar[Calendar.MONTH] = monthOfYear
                        myCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
                        showTimePicker(myCalendar, operation)
                    },
                    myCalendar[Calendar.YEAR],
                    myCalendar[Calendar.MONTH],
                    myCalendar[Calendar.DAY_OF_MONTH]
                )
                datePickerDialogDispatch.show()
                currentServiceOrder?.let {
                    datePickerDialogDispatch.datePicker.minDate = it.callDate?.time ?: Date().time
                }
                datePickerDialogDispatch.datePicker.maxDate = Calendar.getInstance().time.time
            }
            2, 3 -> {
                val datePickerDialogArrive = DatePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Dialog,
                    DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                        myCalendar[Calendar.YEAR] = year
                        myCalendar[Calendar.MONTH] = monthOfYear
                        myCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
                        showTimePicker(myCalendar, operation)
                    },
                    myCalendar[Calendar.YEAR],
                    myCalendar[Calendar.MONTH],
                    myCalendar[Calendar.DAY_OF_MONTH]
                )
                datePickerDialogArrive.show()
                currentServiceOrder?.let {
                    datePickerDialogArrive.datePicker.minDate = it.callDate?.time ?: Date().time
                }
                datePickerDialogArrive.datePicker.maxDate = Date().time
            }
        }
    }

    private fun showTimePicker(calendar: Calendar, operation: Int) {
        val builderAux = AlertDialog.Builder(this)
        val timePickerDialog = TimePickerDialog(
            this,
            android.R.style.Theme_Holo_Dialog,
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar[Calendar.HOUR_OF_DAY] = hourOfDay
                calendar[Calendar.MINUTE] = minute
                val currentTime = Calendar.getInstance().time
                realm.executeTransaction {
                    when (operation) {
                        1 -> {
                            currentServiceOrder?.let { currentServiceOrder ->
                                if (calendar.time.before(currentServiceOrder.arriveTime)) {
                                    if (calendar.time.after(currentServiceOrder.callDate)) {
                                        if (calendar.time.before(currentTime)) {
                                            currentServiceOrder.dispatchTime = calendar.time
                                            binding.dispatchTimeBtn.text =
                                                currentServiceOrder.getFormattedDispatchTime()
                                        } else {
                                            showMessageBox(
                                                getString(R.string.future_warning),
                                                getString(R.string.future_explanation)
                                            )
                                        }
                                    } else {
                                        showMessageBox(
                                            getString(R.string.warning),
                                            getString(R.string.dispatchTimeWarning)
                                        )
                                    }
                                } else {
                                    if (calendar.time.before(currentServiceOrder.departTime)) {
                                        builderAux.setTitle(getString(R.string.warning))
                                            .setMessage(resources.getString(R.string.arriveTimeWarningChange))
                                            .setPositiveButton(getString(android.R.string.ok)) { dialogInterface, i ->
                                                Realm.getDefaultInstance().executeTransaction {
                                                    if (calendar.time.before(currentTime)) {
                                                        currentServiceOrder.arriveTime =
                                                            calendar.time
                                                        currentServiceOrder.dispatchTime =
                                                            calendar.time
                                                        binding.arriveTimeBtn.text =
                                                            currentServiceOrder.getFormattedArriveTime()
                                                        binding.dispatchTimeBtn.text =
                                                            currentServiceOrder.getFormattedDispatchTime()
                                                    } else {
                                                        showMessageBox(
                                                            getString(R.string.future_warning),
                                                            getString(R.string.future_explanation)
                                                        )
                                                    }
                                                }
                                            }
                                            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                                            .show()
                                    } else {
                                        builderAux.setTitle(getString(R.string.warning))
                                            .setMessage(resources.getString(R.string.arriveDepartureTimeWarningChange))
                                            .setPositiveButton(getString(android.R.string.ok)) { dialogInterface, i ->
                                                Realm.getDefaultInstance().executeTransaction {
                                                    if (calendar.time.before(currentTime)) {
                                                        currentServiceOrder.arriveTime =
                                                            calendar.time
                                                        currentServiceOrder.dispatchTime =
                                                            calendar.time
                                                        currentServiceOrder.departTime =
                                                            calendar.time
                                                        binding.arriveTimeBtn.text =
                                                            currentServiceOrder.getFormattedArriveTime()
                                                        binding.dispatchTimeBtn.text =
                                                            currentServiceOrder.getFormattedDispatchTime()
                                                        binding.departureTimeBtn.text =
                                                            currentServiceOrder.getFormattedDepartTime()
                                                    } else {
                                                        showMessageBox(
                                                            getString(R.string.future_warning),
                                                            getString(R.string.future_explanation)
                                                        )
                                                    }
                                                }
                                            }
                                            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                                            .show()
                                    }
                                }
                            }
                        }
                        2 -> {
                            currentServiceOrder?.let { currentServiceOrder ->
                                if (calendar.time.after(currentServiceOrder.dispatchTime)) {
                                    val departDate =
                                        if (currentServiceOrder.departTime == null) currentTime else currentServiceOrder.departTime
                                    if (calendar.time.before(departDate)) {
                                        if (calendar.time.before(currentTime)) {
                                            currentServiceOrder.arriveTime = calendar.time
                                            binding.arriveTimeBtn.text =
                                                currentServiceOrder.getFormattedArriveTime()
                                        } else {
                                            showMessageBox(
                                                getString(R.string.future_warning),
                                                getString(R.string.future_explanation)
                                            )
                                        }
                                    } else {
                                        if (calendar.time.before(currentTime)) {
                                            builderAux.setTitle(getString(R.string.warning))
                                                .setMessage(resources.getString(R.string.departureTimeWarningChange))
                                                .setPositiveButton(getString(android.R.string.ok)) { dialogInterface, i ->
                                                    Realm.getDefaultInstance().executeTransaction {
                                                        currentServiceOrder.arriveTime =
                                                            calendar.time
                                                        currentServiceOrder.departTime =
                                                            calendar.time
                                                        binding.arriveTimeBtn.text =
                                                            currentServiceOrder.getFormattedArriveTime()
                                                        binding.departureTimeBtn.text =
                                                            currentServiceOrder.getFormattedDepartTime()
                                                    }
                                                }
                                                .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                                                .show()
                                        } else {
                                            showMessageBox(
                                                getString(R.string.future_warning),
                                                getString(R.string.future_explanation)
                                            )
                                        }
                                    }
                                } else {
                                    showMessageBox(
                                        getString(R.string.warning),
                                        getString(R.string.arriveTimeWarning)
                                    )
                                }
                            }
                        }
                        3 -> {
                            currentServiceOrder?.let { currentServiceOrder ->
                                if (calendar.time.after(currentServiceOrder.arriveTime)) {
                                    if (calendar.time.before(currentTime)) {
                                        currentServiceOrder.departTime = calendar.time
                                        binding.departureTimeBtn.text =
                                            currentServiceOrder.getFormattedDepartTime()
                                    } else {
                                        showMessageBox(
                                            getString(R.string.future_warning),
                                            getString(R.string.future_explanation)
                                        )
                                    }
                                } else {
                                    showMessageBox(
                                        getString(R.string.warning),
                                        getString(R.string.departureTimeWarning)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            calendar[Calendar.HOUR_OF_DAY],
            calendar[Calendar.MINUTE],
            DateFormat.is24HourFormat(
                baseContext
            )
        )
        timePickerDialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        try {
            super.onSaveInstanceState(outState)
            outState.clear()
        } catch (e: Exception) {
            Log.d(TAG, "On save instance state fail")
        }
    }

    override fun onStop() {
        super.onStop()
        val realm2 = Realm.getDefaultInstance()
        val serviceCallProperty =
            ServiceCallProperty(orderId, binding.txtCompleteRemarks.text.toString())
        realm2.beginTransaction()
        realm2.insertOrUpdate(serviceCallProperty)
        realm2.commitTransaction()
        realm2.close()

        currentServiceOrder?.let {
            if (!it.isValid) {
                return
            }
            val currentId = it.callNumber_ID
            lifecycleScope.launch(Dispatchers.IO) {
                CompletedCallsRepository.saveDescriptionInServiceCallTemporalData(
                    currentId,
                    binding.txtProblemDescription.text.toString()
                )
            }

        }
    }


    private fun checkStep7(): Boolean {
        return true
    }

    private fun checkStep6(): Boolean {
        return !binding.txtProblemDescription.text.isNullOrEmpty()
    }

    private fun checkStep5(): Boolean {
        return true
    }

    private fun checkStep4(): Boolean {
        return true
    }

    private fun checkStep3(): Boolean {
        return true
    }

    private fun checkStep2(): Boolean {
        if (!checkMeters()) {
            return false
        }
        return binding.txtCompleteRemarks.text.toString().isNotEmpty()
    }

    private fun checkStep1(): Boolean {
        if (realm.where(UsedProblemCode::class.java).equalTo(UsedProblemCode.CALL_ID, orderId)
                .count() == 0L
        ) return false
        if (realm.where(UsedRepairCode::class.java).equalTo(UsedRepairCode.CALL_ID, orderId)
                .count() == 0L
        ) return false
        return binding.spinActivityCodes.selectedItem != null
    }

    private fun checkMeters(): Boolean {
        for (meter in meters) {
            if (meter.isRequiredMeterOnServiceCalls && meter.isRequired && !meter.isMeterSet) {
                return false
            }
        }
        return true
    }

    private fun initScreen() {
        binding.step = currentStep
        viewModelParts.updateCurrentStep(currentStep)
        binding.btnNext.visibility = if (currentStep != SUMMARY_STEP_7) View.VISIBLE else View.GONE
        binding.btnComplete.visibility =
            if (currentStep == SUMMARY_STEP_7) View.VISIBLE else View.GONE
        if (currentStep == SUMMARY_STEP_7) {
            fillExtendedData()
        }
        if (currentStep == USED_PARTS_STEP_3) {
            viewModelParts.updatePartsByCallId()
        }
        if (isAssist) {
            binding.btnBack.visibility = View.GONE
            binding.btnNext.visibility = View.GONE
            binding.btnComplete.visibility = View.VISIBLE
        }
    }

    private fun fillExtendedData() {
        val currentWarehouseId = AppAuth.getInstance().technicianUser.warehouseId
        val usedPartsData: List<UsedPart>? = PartsRepository.getUsedPartData(orderId)
        if (usedPartsData.isNullOrEmpty()) {
            binding.usedPartTitle.visibility = View.GONE
            binding.usedPartDivider.visibility = View.GONE
            binding.txtUsedParts.visibility = View.GONE
        } else {
            binding.usedPartTitle.visibility = View.VISIBLE
            binding.usedPartDivider.visibility = View.VISIBLE
            binding.txtUsedParts.visibility = View.VISIBLE
            val formattedUsedPart: String = UsedPart.getFormattedUsedPartData(
                usedPartsData,
                currentWarehouseId,
                applicationContext
            )
            binding.txtUsedParts.text = formattedUsedPart
        }
        val helper = ApiHelperBuilder(JsonElement::class.java)
            .addPath("ServiceCall")
            .addPath("GetServiceCallLabor")
            .addParameter("callId", orderId.toString())
            .setAuthorized(true)
            .build()
        helper.runAsync { success, result, _, _ ->
            if (success && result != null) {
                val jsonObject = if (result.isJsonNull) return@runAsync else result.asJsonObject
                binding.txtTravelHours.text =
                    String.format(Locale.getDefault(), "%.0f", jsonObject["TravelHours"].asDouble)
                binding.txtStandardHours.text =
                    String.format(Locale.getDefault(), "%.0f", jsonObject["LaborHours"].asDouble)
                binding.txtOvertimeHours.text =
                    String.format(Locale.getDefault(), "%.0f", jsonObject["OvertimeHours"].asDouble)
            }
        }
    }

    private fun offlineComplete() {
        val msg = "Completing action"
        FBAnalyticsConstants.logEvent(
            this,
            FBAnalyticsConstants.CompleteActivity.COMPLETE_ACTION,
            msg
        )
        try {
            if (binding.emailListSwitch.isChecked) {
                var addedMailSuccessfully = false
                if (binding.emailListEditText.text.toString().isNotEmpty()) {
                    addedMailSuccessfully =
                        createEmailList(binding.emailListEditText.text.toString())
                    if (!addedMailSuccessfully) {
                        return
                    }
                }
                if (emailList.isEmpty()) {
                    if (!addedMailSuccessfully) {
                        showEmptyEmailListMessage()
                    }
                    return
                }
            }
            currentServiceOrder =
                realm.where(ServiceOrder::class.java).equalTo("callNumber_ID", orderId)
                    .findFirst()
            currentServiceOrder?.let {
                val size = DatabaseRepository.getInstance()
                    .getNotDeletedNeededPartsFromEAutomate(it.callNumber_ID).size
                if (size > 0) {
                    showMessageBox(
                        getString(R.string.cant_complete),
                        getString(R.string.remove_or_use_parts_in_ea)
                    )
                    return
                }
                /**
                 * *Delete UsedParts with usageStatusId as Needed that have been added locally, since we don't send neededparts in the complete flow
                 */
                DatabaseRepository.getInstance().deleteNeededPartsAddedLocally(it.callNumber_ID)
            }
            if (!binding.signPad.isEmpty && AppAuth.getInstance().technicianUser.isFileAttachmentEnabled) {
                val signeeName = binding.txtTypeName.text.toString()
                if (signeeName.isEmpty()) {
                    showMessageBox(
                        getString(R.string.required_title),
                        getString(R.string.name_is_required)
                    )
                    return
                }
            }
            currentServiceOrder =
                realm.where(ServiceOrder::class.java).equalTo("callNumber_ID", orderId)
                    .findFirst()
            currentServiceOrder?.let { currentServiceOrder ->
                if (currentServiceOrder.isValid) {
                    val remarks = binding.txtCompleteRemarks.text.toString()
                    if (remarks.isEmpty()) {
                        Toast.makeText(this, "Remarks is required", Toast.LENGTH_LONG).show()
                        return
                    }
                    realm.executeTransaction {
                        currentServiceOrder.completedCall = true
                        AppAuth.getInstance().completedCallId = orderId
                        val incompleteCompleteRequest =
                            IncompleteRequests(UUID.randomUUID().toString())
                        val emailDetail = realm.createObject(EmailDetail::class.java)
                        binding.bccCheckBox.let {
                            if (it.isChecked) {
                                emailDetail.bccAddress =
                                    AppAuth.getInstance().technicianUser.email ?: ""
                            }
                        }
                        binding.emailListSwitch.let {
                            if (it.isChecked) {
                                emailDetail.toAddress = emailList.joinToString(";")
                                emailDetail.emailCallNumberId = orderId
                            }
                        }
                        incompleteCompleteRequest.actionTime = currentServiceOrder.departTime
                        incompleteCompleteRequest.callNumberCode =
                            currentServiceOrder.callNumber_Code
                        incompleteCompleteRequest.callId = currentServiceOrder.callNumber_ID
                        incompleteCompleteRequest.dateAdded = Calendar.getInstance().time
                        incompleteCompleteRequest.requestType =
                            if (incompleteMode) "IncompleteCall" else "DepartCall"
                        incompleteCompleteRequest.comments = remarks
                        incompleteCompleteRequest.completeCallDispatchTime =
                            currentServiceOrder.dispatchTime
                        incompleteCompleteRequest.completeCallArriveTime =
                            currentServiceOrder.arriveTime
                        incompleteCompleteRequest.isAssist = isAssist
                        incompleteCompleteRequest.isPreventiveMaintenance =
                            serviceCallTemporalData.preventiveMaintenance
                        incompleteCompleteRequest.requestCategory =
                            Constants.REQUEST_TYPE.COMPLETE_CALL.value
                        incompleteCompleteRequest.status =
                            Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                        incompleteCompleteRequest.equipmentId = equipmentId
                        incompleteCompleteRequest.callStatusCode =
                            currentServiceOrder.statusCode_Code?.trim()
                        if (!binding.signPad.isEmpty && AppAuth.getInstance().technicianUser.isFileAttachmentEnabled) {
                            val signeeName = binding.txtTypeName.text.toString()
                            val signatureBitmap = binding.signPad.signatureBitmap
                            val bos = ByteArrayOutputStream()
                            signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 0, bos)
                            val bytes = bos.toByteArray()
                            incompleteCompleteRequest.fileContentBase64 = Base64Utils.encode(bytes)
                            incompleteCompleteRequest.fileName =
                                UUID.randomUUID().toString() + ".jpg"
                            incompleteCompleteRequest.fileSize = bytes.size
                            incompleteCompleteRequest.signeeName = signeeName
                        }
                        val item =
                            activityCodesListAdapter.getItem(binding.spinActivityCodes.selectedItemPosition)
                        if (item != null) {
                            incompleteCompleteRequest.activityCodeId = item.activityCodeId
                        }
                        realm.insertOrUpdate(incompleteCompleteRequest)
                    }
                }

            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        if (AppAuth.getInstance().isConnected) {
            OfflineManager.retryWorker(this)
        }
        val progress = ProgressDialog.show(this, "", getString(R.string.loading))
        object : CountDownTimer(800, 100) {
            override fun onTick(millisUntilFinished: Long) {/*not used*/
            }

            override fun onFinish() {
                if (progress.isShowing) {
                    progress.dismiss()
                }
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }
        }.start()

    }

    private fun showEmptyEmailListMessage() {
        showMessageBox(
            getString(R.string.empty_email_list_title),
            getString(R.string.empty_email_list_message)
        )
    }

    private fun incompleteOffline() {
        val msg = "Incompleting Action"
        FBAnalyticsConstants.logEvent(
            this,
            FBAnalyticsConstants.CompleteActivity.INCOMPLETE_ACTION,
            msg
        )
        if (binding.emailListSwitch.isChecked) {
            var addedMailSuccessfully = false
            if (binding.emailListEditText.text.toString().isNotEmpty()) {
                addedMailSuccessfully = createEmailList(binding.emailListEditText.text.toString())
                if (!addedMailSuccessfully) {
                    return
                }
            }
            if (emailList.isEmpty()) {
                if (!addedMailSuccessfully) {
                    showEmptyEmailListMessage()
                }
                return
            }
        }
        currentServiceOrder =
            realm.where(ServiceOrder::class.java).equalTo("callNumber_ID", orderId)
                .findFirst()
        currentServiceOrder?.let { serviceOrder ->
            currentIncompleteCode?.let { incompleteCode ->
                incompleteCode.category?.let { category ->
                    val size = DatabaseRepository.getInstance()
                        .getNotDeletedNeededPartsFromEAutomate(serviceOrder.callNumber_ID).size
                    if (size > 0) {
                        showMessageBox(
                            getString(R.string.cant_incomplete),
                            getString(R.string.remove_or_use_parts_in_ea)
                        )
                        return
                    }
                }
            }
        }

        if (!binding.signPad.isEmpty && AppAuth.getInstance().technicianUser.isFileAttachmentEnabled) {
            val signeeName = binding.txtTypeName.text.toString()
            if (signeeName.isEmpty()) {
                showMessageBox(
                    getString(R.string.required_title),
                    getString(R.string.name_is_required)
                )
                return
            }
        }

        currentServiceOrder?.let { currentServiceOrder ->
            if (currentServiceOrder.isValid) {
                val remarks = binding.txtCompleteRemarks.text.toString()
                if (remarks.isEmpty()) {
                    Toast.makeText(this, "Remarks is required", Toast.LENGTH_LONG).show()
                    return
                }
                realm.executeTransaction {
                    currentServiceOrder.completedCall = true
                    AppAuth.getInstance().completedCallId = orderId
                    val incompleteCompleteRequest = IncompleteRequests(UUID.randomUUID().toString())
                    val problemDescription = binding.txtProblemDescription.text.toString()
                    val emailDetail = realm.createObject(EmailDetail::class.java)
                    binding.bccCheckBox.let {
                        if (it.isChecked) {
                            emailDetail.bccAddress =
                                AppAuth.getInstance().technicianUser.email ?: ""
                        }
                    }
                    binding.emailListSwitch.let {
                        if (it.isChecked) {
                            emailDetail.toAddress = emailList.joinToString(";")
                            emailDetail.emailCallNumberId = orderId
                        }
                    }
                    incompleteCompleteRequest.actionTime = currentServiceOrder.departTime
                    incompleteCompleteRequest.callNumberCode = currentServiceOrder.callNumber_Code
                    incompleteCompleteRequest.callId = orderId
                    incompleteCompleteRequest.dateAdded = Calendar.getInstance().time
                    incompleteCompleteRequest.requestType = "IncompleteCall"
                    incompleteCompleteRequest.comments = remarks
                    incompleteCompleteRequest.description = problemDescription
                    incompleteCompleteRequest.completeCallDispatchTime =
                        currentServiceOrder.dispatchTime
                    incompleteCompleteRequest.completeCallArriveTime =
                        currentServiceOrder.arriveTime
                    incompleteCompleteRequest.isAssist = isAssist
                    incompleteCompleteRequest.isPreventiveMaintenance =
                        serviceCallTemporalData.preventiveMaintenance
                    incompleteCompleteRequest.requestCategory =
                        Constants.REQUEST_TYPE.INCOMPLETE_CALL.value
                    incompleteCompleteRequest.status =
                        Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                    incompleteCompleteRequest.equipmentId = equipmentId
                    incompleteCompleteRequest.callStatusCode =
                        currentServiceOrder.statusCode_Code?.trim()
                    if (!binding.signPad.isEmpty && AppAuth.getInstance().technicianUser.isFileAttachmentEnabled) {
                        val signeeName = binding.txtTypeName.text.toString()
                        val signatureBitmap = binding.signPad.signatureBitmap
                        val bos = ByteArrayOutputStream()
                        signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 0, bos)
                        val bytes = bos.toByteArray()
                        incompleteCompleteRequest.fileContentBase64 = Base64Utils.encode(bytes)
                        incompleteCompleteRequest.fileName = UUID.randomUUID().toString() + ".jpg"
                        incompleteCompleteRequest.fileSize = bytes.size
                        incompleteCompleteRequest.signeeName = signeeName
                    }
                    val item =
                        activityCodesListAdapter.getItem(binding.spinActivityCodes.selectedItemPosition)
                    if (item != null) {
                        incompleteCompleteRequest.activityCodeId = item.activityCodeId
                    }
                    currentIncompleteCode?.let { currentIncompleteCode ->
                        currentIncompleteCode.incompleteCodeId?.let {
                            incompleteCompleteRequest.incompleteCodeId = it
                        }
                        currentIncompleteCode.category?.let {
                            incompleteCompleteRequest.incompleteCategory = it
                        }
                    }
                    realm.insertOrUpdate(incompleteCompleteRequest)
                }
                DatabaseRepository.getInstance().changeIncompleteCodeToOriginalState()
            }
        }
        if (AppAuth.getInstance().isConnected) {
            OfflineManager.retryWorker(this)
        }
        val progress = ProgressDialog.show(this, "", getString(R.string.loading))
        object : CountDownTimer(800, 100) {
            override fun onTick(millisUntilFinished: Long) {/*not used*/
            }

            override fun onFinish() {
                if (progress.isShowing) {
                    progress.dismiss()
                }
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }
        }.start()
    }

    override fun hideKeyboard() {
        binding.txtCompleteRemarks.isFocusableInTouchMode = false
        binding.txtCompleteRemarks.isFocusable = false
    }

    override fun updateIncompleteCode(item: IncompleteCode?) {
        if (item != null) {
            currentIncompleteCode = item
            saveLocallyIncompleteCode(item)
        } else {
            realm.executeTransaction {
                serviceCallTemporalData.incompleteCodeId = null
                currentIncompleteCode = null
            }
        }
    }

    /**
     * saveLocallyIncompleteCode(item)
     * will Save the current incomplete code
     *
     * @param item -> Incomplete code selected
     */
    private fun saveLocallyIncompleteCode(item: IncompleteCode?) {
        item?.let {
            it.incompleteCodeId?.let {
                realm.beginTransaction()
                serviceCallTemporalData.incompleteCodeId = it.toString()
                realm.commitTransaction()
            }
        }
    }

    companion object {
        private const val TAG = "CompleteActivity"
        private const val EXCEPTION = "Exception logger"

        private const val PROBLEM_REPAIR_CODE_STEP_1 = 1
        private const val REMARKS_STEP_2 = 2
        private const val USED_PARTS_STEP_3 = 3
        private const val INCOMPLETE_CODE_STEP_4 = 4
        private const val NEED_PARTS_STEP_5 = 5
        private const val DESCRIPTION_STEP_6 = 6
        private const val SUMMARY_STEP_7 = 7
    }


    override fun onEmailDeleted(position: Int) {
        val item: String? = try {
            emailList[position]
        } catch (e: Exception) {
            null
        }
        if (binding.bccCheckBox.isChecked && item != null && item == AppAuth.getInstance().technicianUser.email) {
            Toast.makeText(this, getString(R.string.bcc_checked), Toast.LENGTH_SHORT).show()
        } else {
            try {
                emailList.removeAt(position)
                binding.recEmails.adapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            }
        }
    }

    override fun onUsedProblemCodePressed(item: UsedProblemCode) {
        DatabaseRepository.getInstance().deleteUsedProblemCode(item);
    }

    override fun onUsedRepairCodePressed(item: UsedRepairCode) {
        DatabaseRepository.getInstance().deleteUsedRepairCode(item);
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

}