package eci.technician.activities

import android.R.attr.data
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivityGroupCallDetailsBinding
import eci.technician.helpers.AddressMapHelper
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.PhoneHelper
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.api.retroapi.RetrofitRepository.RetrofitRepositoryObject.getInstance
import eci.technician.models.ProcessingResult
import eci.technician.models.data.ReassignResponse
import eci.technician.models.data.UnavailableParts
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.repository.GroupCallsRepository
import eci.technician.tools.Constants
import eci.technician.viewmodels.GroupCallsViewModel
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.activity_about.toolbar
import kotlinx.android.synthetic.main.activity_order_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GroupCallDetailsActivity : BaseActivity() {

    companion object {
        const val TAG = "GroupCallDetailsActivity"
        const val EXCEPTION = "Exception"
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[GroupCallsViewModel::class.java]
    }

    private val connection by lazy {
        NetworkConnection(this)
    }

    private var isConnected = true
    private lateinit var binding: ActivityGroupCallDetailsBinding
    private lateinit var detailedGroupCallServiceOrder: GroupCallServiceOrder
    private var serviceOrderCallNumberId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_group_call_details)
        serviceOrderCallNumberId = intent.getIntExtra(Constants.SERVICE_ORDER_ID, 0)
        binding.lifecycleOwner = this
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        lifecycleScope.launch(Dispatchers.IO) {
            GroupCallsRepository.getGroupCallServiceOrder(serviceOrderCallNumberId)?.let {
                detailedGroupCallServiceOrder = it
            }
        }
        viewModel.getDetailedGroupLiveData(serviceOrderCallNumberId)
            .observe(this) { serviceOrders: RealmResults<GroupCallServiceOrder?> ->
                if (!serviceOrders.isEmpty()) {
                    val firstServiceOrder = serviceOrders.first()
                    if (firstServiceOrder != null && firstServiceOrder.isValid) {
                        binding.item = firstServiceOrder
                        detailedGroupCallServiceOrder = firstServiceOrder
                        title = detailedGroupCallServiceOrder.callNumber_Code
                        observePhoneNumber()
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            }

        binding.assignToMeBtn.setOnClickListener {
            if (AppAuth.getInstance().technicianUser.isAllowReassignment) {
                tryReAssignCall()
            }
        }
        binding.txtCustomerAddress.setOnClickListener {
            onCustomerAddressClick()
        }
        binding.txtCustomerAddress.setOnLongClickListener {
            copyAddressToClipBoard()
            return@setOnLongClickListener true
        }
        binding.btnContactEmail.setOnClickListener {
            sendContactEmail()
        }

        binding.btnContactEmail.setOnLongClickListener {
            copyContactEmail()
            return@setOnLongClickListener true
        }

        binding.btnContactPhone.setOnClickListener {
            callContact()
        }

        binding.btnContactPhone.setOnLongClickListener {
            copyContactPhone()
            return@setOnLongClickListener true
        }

        binding.btnEquipmentId.setOnClickListener {
            onEquipmentIdClick()
        }

        binding.btnEquipmentId.setOnLongClickListener {
            copyEquipmentIdToClipboard()
            return@setOnLongClickListener true
        }
        connection.observe(this) { t ->
            t?.let {
                isConnected = it
            }
        }
    }

    private fun onEquipmentIdClick() {
        detailedGroupCallServiceOrder.equipmentNumber?.let { equipmentNumber ->
            if (isConnected) {
                if (!TextUtils.isEmpty(equipmentNumber)) {
                    showEquipmentHistory()
                }
            } else {
                showUnavailableWhenOfflineMessage()
            }
        }
    }

    private fun showEquipmentHistory() {
        detailedGroupCallServiceOrder?.let { serviceOrder ->
            val intent = Intent(this, EquipmentHistoryActivity::class.java)
            intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, serviceOrder.equipmentId)
            startActivity(intent)
        }
    }

    private fun tryReAssignCall() {
        detailedGroupCallServiceOrder.callNumber_ID?.let { serviceOrderCallNumberId ->
            RetrofitRepository.RetrofitRepositoryObject
                .getInstance()
                .getOneGroupCallServiceByCallId(serviceOrderCallNumberId, lifecycleScope, this)
                .observe(this, Observer {
                    manageOneServiceCallResponse(it, detailedGroupCallServiceOrder)
                })
        }
    }

    private fun reAssignCall() {
        val progressDialog = ProgressDialog.show(this, "", getString(R.string.loading))
        val reAssignMap: HashMap<String, Any> = HashMap()
        reAssignMap["CallId"] = detailedGroupCallServiceOrder.callNumber_ID
        reAssignMap["TechnicianId"] = AppAuth.getInstance().technicianUser.technicianNumber
        viewModel.reassignCall(reAssignMap).observe(this, Observer {
            progressDialog.dismiss()
            it?.let {
                if (it.isHasError) {
                    onReassignError(it, detailedGroupCallServiceOrder)
                } else {
                    val realm = Realm.getDefaultInstance()
                    try {
                        realm.executeTransaction {
                            AppAuth.getInstance().technicianUser.technicianCode?.let{
                                detailedGroupCallServiceOrder.technicianNumber = it
                            }
                            detailedGroupCallServiceOrder.technicianName =
                                AppAuth.getInstance().technicianUser.technicianName
                            showAffirmationBox(
                                resources.getString(R.string.reassignSuccessfulTitle),
                                resources.getString(
                                    R.string.reassignSuccessfulMessage,
                                    detailedGroupCallServiceOrder.callNumber_Code
                                )
                            ) { _, _ ->
                                binding.assignToMeBtn.visibility = View.GONE
                                val intent = Intent(this, GroupCallsActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                intent.putExtra(
                                    Constants.FROM_DETAILS,
                                    detailedGroupCallServiceOrder.callNumber_ID
                                )
                                setResult(Constants.ACTIVITY_GROUP_CALLS_DETAIL, intent)
                                viewModel.fetchTechnicianActiveServiceCalls( forceUpdate = true)
                                finish()
                                hideKeyboard(this)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
                    } finally {
                        realm.close()
                    }
                }
            }
        })
    }


    private fun onReassignError(
        processingResult: ProcessingResult,
        groupCallServiceOrder: GroupCallServiceOrder
    ) {
        val intent = Intent(this, ReassignMissingPartsActivity::class.java)
        if (processingResult.result == null) {
            val message =
                processingResult.errors[0].errorText + " " + resources.getString(R.string.server_error)
            showMessageBox(resources.getString(R.string.couldnt_reassign_call), message)
            return
        }
        val reassignResponseFormatted: ReassignResponse =
            Gson().fromJson(processingResult.result, ReassignResponse::class.java)
        if (reassignResponseFormatted.unavailableParts != null) {
            val unavailableParts: UnavailableParts = reassignResponseFormatted.unavailableParts
            intent.putExtra(Constants.REASSIGN_UNAVAILABLE_PARTS, unavailableParts)
        }
        if (reassignResponseFormatted.neededParts != null) {
            val neededParts: UnavailableParts = reassignResponseFormatted.neededParts
            intent.putExtra(Constants.REASSIGN_NEEDED_PARTS, neededParts)
        }
        intent.putExtra(Constants.SERVICE_ORDER_ID, groupCallServiceOrder.callNumber_Code)
        startActivity(intent)
    }


    private fun manageOneServiceCallResponse(
        genericDataResponse: GenericDataResponse<MutableList<GroupCallServiceOrder>>,
        groupCallServiceOrder: GroupCallServiceOrder
    ) {
        when (genericDataResponse.responseType) {
            RequestStatus.SUCCESS -> {
                genericDataResponse.data?.let {
                    if (it.isEmpty()) {
                        showAffirmationBox(
                            getString(R.string.warning),
                            getString(
                                R.string.the_service_call_was_canceled_or_completed,
                                detailedGroupCallServiceOrder.callNumber_Code
                            )
                        )
                        { _, _: Int ->
                            val intent = Intent()
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent.putExtra(
                                Constants.ACTIVITY_GROUP_CALLS_DETAIL_CODE,
                                groupCallServiceOrder.callNumber_Code
                            )
                            intent.putExtra(
                                Constants.ACTIVITY_GROUP_CALLS_DETAIL_ID,
                                groupCallServiceOrder.callNumber_ID
                            )
                            setResult(Constants.ACTIVITY_GROUP_CALLS_DETAIL_DELETE, intent)
                            finish()
                        }
                        return
                    }
                    val currentServiceOrder = it.first()
                    if (currentServiceOrder.canReassignChecker()) {
                        reAssignCall()
                    } else {
                        showMessageBox(resources.getString(R.string.somethingWentWrong),
                            getString(R.string.the_service_call_was) + currentServiceOrder.statusCode,
                            DialogInterface.OnClickListener { _, _ ->
                                val intent = Intent(this, GroupCallsActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                intent.putExtra(
                                    Constants.ACTIVITY_GROUP_CALLS_DETAIL_ID,
                                    groupCallServiceOrder.callNumber_ID.toString()
                                )
                                setResult(Constants.ACTIVITY_GROUP_CALLS_DETAIL_ERROR, intent)
                                finish()
                                return@OnClickListener
                            })
                    }

                }
            }
            RequestStatus.ERROR -> {
                val error = genericDataResponse.onError
                showNetworkErrorDialog(error, this, supportFragmentManager)
            }
        }
    }

    fun copyAddressToClipBoard() {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData =
            ClipData.newPlainText("", detailedGroupCallServiceOrder.customerFullAddress)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
    }

    fun copyEquipmentIdToClipboard() {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData =
            ClipData.newPlainText("", detailedGroupCallServiceOrder.equipmentNumber)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
    }

    fun copyContactPhone() {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", detailedGroupCallServiceOrder.contactPhone)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
    }

    private fun copyContactEmail() {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", detailedGroupCallServiceOrder.contactEmail)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
    }

    private fun callContact() {
        var phoneToCall = ""
        detailedGroupCallServiceOrder.contactPhone?.let {
            phoneToCall = PhoneHelper.getOnlyNumbersBeforeDialing(it)
        }
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneToCall")
        startActivity(intent)
    }

    private fun sendContactEmail() {
        detailedGroupCallServiceOrder.contactEmail?.let { contactEmail ->
            if (contactEmail.isNotEmpty()) {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "message/rfc822"
                i.putExtra(Intent.EXTRA_EMAIL, arrayOf(detailedGroupCallServiceOrder.contactEmail))
                i.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Call# " + detailedGroupCallServiceOrder.callNumber_Code
                )
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_email_title)))
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(this, getString(R.string.no_email_clients), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    fun onCustomerAddressClick() {
        AddressMapHelper.openNavigation(detailedGroupCallServiceOrder.customerFullAddress, this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observePhoneNumber(){
        val contactPhoneNumber = binding.item?.contactPhone
        lifecycleScope.launch(Dispatchers.IO){
            val contactPhone = async {
                PhoneHelper.getFormattedCustomerPhoneNumber(
                    contactPhoneNumber,
                    applicationContext
                )
            }.await()
            withContext(Dispatchers.Main){
                contactPhone?.let { btnContactPhone.text = contactPhone.toString() }
            }
        }
    }

}