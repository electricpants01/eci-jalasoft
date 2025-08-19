package eci.technician.fragments.createCall

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.FragmentCreateCallBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.KeyboardHelper
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.CreateCallInterface
import eci.technician.models.order.ServiceOrder
import eci.technician.tools.Settings
import eci.technician.viewmodels.CreateCallViewModel
import kotlinx.android.synthetic.main.fragment_create_call.*
import kotlin.Exception


class CreateCallFragment : Fragment(), View.OnClickListener {
    private final val TAG = "CreateCallFragment"
    private final val EXCEPTION = "Exception"
    private val viewModel: CreateCallViewModel by activityViewModels()
    private var _binding: FragmentCreateCallBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            (activity as CreateCallInterface).finishActivity()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        _binding = FragmentCreateCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.create_call_title)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        binding.callTypeSelectorLinearLayout.setOnClickListener(this)
        binding.equipmentSearchLayoutArea.setOnClickListener(this)
        binding.locationSelectorCardView.setOnClickListener(this)
        binding.createcallBtn.setOnClickListener(this)
        binding.assignToMeCheckBox.isChecked = viewModel.assignToMeField
        binding.callerNameTxtField.doOnTextChanged { text, _, _, _ ->
            viewModel.callerField = text.toString()
            validateCreateCallButton()

        }
        binding.descriptionTxtField.doOnTextChanged { text, _, _, _ ->
            viewModel.descriptionField = text.toString()
            validateCreateCallButton()
        }
        binding.assignToMeCheckBox.setOnCheckedChangeListener { _, value ->
            viewModel.assignToMeField = value
        }
        binding.remarksTxtField.doOnTextChanged { text, start, count, after ->
            viewModel.remarksField = text.toString()
        }
        KeyboardHelper.hideSoftKeyboard(binding.root)
    }

    override fun onResume() {
        super.onResume()
        updateEquipmentWithCustomerData()
        refreshCallTypeSelector()
        refreshCallEquipmentSelector()
        refreshCustomerLocationSelector()
        refreshCallerText()
        refreshDescriptionText()
        validateCreateCallButton()
    }

    private fun validateCreateCallButton() {
        binding.createcallBtn.isEnabled = viewModel.validData()
    }

    private fun refreshDescriptionText() {
        if (viewModel.descriptionField.isNotEmpty()) {
            binding.descriptionTxtField.setText(viewModel.descriptionField)
        }
    }

    private fun refreshCallerText() {
        if (viewModel.callerField.isNotEmpty()) {
            binding.callerNameTxtField.setText(viewModel.callerField)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.locationSelectorCardView -> {
                onTapLocationSelector()
            }
            R.id.callTypeSelectorLinearLayout -> {
                onTapCallTypeSelector()
            }
            R.id.equipmentSearchLayoutArea -> {
                onTapEquipmentSelector()
            }
            R.id.createcallBtn -> {
                sendDataToCreateSC()
            }
        }
    }

    private fun onTapEquipmentSelector() {
        KeyboardHelper.hideSoftKeyboard(binding.root)
        (activity as CreateCallInterface).goToEquipmentSearch()
    }

    private fun onTapCallTypeSelector() {
        viewModel.getAllCallTypes()
        (activity as CreateCallInterface).goToCallTypes()
    }

    private fun onTapLocationSelector() {
        KeyboardHelper.hideSoftKeyboard(binding.root)
        (activity as CreateCallInterface).goToCustomerLocations()
    }

    private fun refreshCustomerLocationSelector() {
        if (viewModel.customerLocationSelected == null) {
            binding.customerSecondaryTitle.visibility = View.GONE
            binding.customerNumberCode.visibility = View.GONE
        } else {
            viewModel.customerLocationSelected?.let {
                binding.customerMainTitle.text = it.customerName
                binding.customerSecondaryTitle.text = it.getLocationJoined()
                binding.customerNumberCode.text = getString(R.string.customerNumberCode, it.customerNumberCode)
                it.equipments?.let { equipmentList ->
                    viewModel.equipmentListFromCustomerSelected = equipmentList
                }
            }
        }
    }

    private fun updateEquipmentWithCustomerData() {
        if (viewModel.equipmentItemSelected == null) {
            refreshCallEquipmentSelector()
        } else {
            if (checkEquipmentBelongsToCustomer() || checkCustomerBelongsToEquipment()) {
                //do nothing
            } else {
                viewModel.equipmentItemSelected = null
                refreshCallEquipmentSelector()
            }
        }
    }

    private fun checkCustomerBelongsToEquipment(): Boolean {
        var res = false
        try {
            viewModel.customerLocationSelected?.let { customerItem ->
                viewModel.equipmentItemSelected?.let { equipmentItem ->
                    res = equipmentItem.customer?.customerNumberID == customerItem.customerNumberID
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        return res
    }

    private fun checkEquipmentBelongsToCustomer(): Boolean {
        var res = false
        try {
            viewModel.customerLocationSelected?.let { customerItem ->
                viewModel.equipmentItemSelected?.let { equipmentItem ->
                    customerItem.equipments?.let {
                        res = it.contains(equipmentItem)
                    }
                    customerItem.equipments?.let {
                        val existEquipment = it.find { customerEquipment ->
                            customerEquipment.equipmentNumberCode == equipmentItem.equipmentNumberCode
                        }
                        res = existEquipment != null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        return res
    }

    private fun refreshCallTypeSelector() {
        viewModel.callTypeSelected?.let {
            binding.callTypeDescription.text = it.callTypeCode
            binding.callTypeTitle.text = it.callTypeDescription
        }
    }

    private fun refreshCallEquipmentSelector() {
        if (viewModel.equipmentItemSelected == null) {
            binding.mainTitleEquipment.text = getString(R.string.select_equipment)
            binding.secondaryText1Equipment.visibility = View.GONE
            binding.secondaryText2Equipment.visibility = View.GONE
            binding.remarksTxtField.isEnabled = false
        } else {
            viewModel.equipmentItemSelected?.let {
                binding.mainTitleEquipment.text = it.equipmentNumberCode
                binding.secondaryText1Equipment.text = getString(R.string.equipment_two_values, it.modelNumberCode, it.modelDescription)
                binding.secondaryText2Equipment.text = getString(R.string.equipment_serial_number, it.serialNumber)
                binding.remarksTxtField.setText(viewModel.remarksField)
                binding.remarksTxtField.isEnabled = true
                binding.secondaryText1Equipment.visibility = View.VISIBLE
                binding.secondaryText2Equipment.visibility = View.VISIBLE
            }
        }
    }

    private fun sendDataToCreateSC() {
        FBAnalyticsConstants.logEvent(requireContext(), FBAnalyticsConstants.CreateCallActivity.CREATE_CALL_ACTION)
        if (!AppAuth.getInstance().isConnected){
            if (requireActivity() is BaseActivity){
                (requireActivity() as BaseActivity).showUnavailableWhenOfflineMessage()
                return
            }
        }
        progressDialog = ProgressDialog.show(requireContext(), "", getString(R.string.loading))
        viewModel.createServiceCallModel()?.let { create ->
            viewModel.createServiceCall(create).observe(this, androidx.lifecycle.Observer {
                if (!it.isHasError) {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    val serviceOrder = Settings.createGson().fromJson(it.result, ServiceOrder::class.java)
                    Toast.makeText(context, getString(R.string.service_call_created, serviceOrder.callNumber_Code), Toast.LENGTH_SHORT).show()
                    (activity as CreateCallInterface).finishActivity()
                } else {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    showErrorDialogue(it.formattedErrors)
                }
            })
        }
    }

    private fun showErrorDialogue(msg: String) {
        context?.let { context ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setMessage(msg).setCancelable(false).setTitle(R.string.somethingWentWrong)
                    .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
            builder.create().show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}