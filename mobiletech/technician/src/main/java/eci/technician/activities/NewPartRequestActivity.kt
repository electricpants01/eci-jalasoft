package eci.technician.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import eci.technician.BaseActivity
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.adapters.RequestTransferAdapter
import eci.technician.custom.CustomSearchPartDialog
import eci.technician.databinding.ActivityNewPartRequestBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.PhoneHelper
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.order.Part
import eci.technician.models.order.RequestPartTransferItem
import eci.technician.tools.Constants
import eci.technician.viewmodels.NewPartRequestViewModel
import java.util.function.Predicate


class NewPartRequestActivity : BaseActivity(), RequestTransferAdapter.RequestTransferListener,
    CustomSearchPartDialog.CustomDialogListener {

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding:ActivityNewPartRequestBinding
    private val viewModel: NewPartRequestViewModel by viewModels()
    private var progressDialog: ProgressDialog? = null

    companion object {
        const val TAG = "NewPartRequestActivity"
        const val EXCEPTION = "Exception logger"
        const val REQUEST_CODE_LOCATION = 1

    }

    override fun onSelectItem(item: Part) {
        searchForItem(item.itemId, item.item, item.description)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewPartRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.NEW_PART_REQUEST_ACTIVITY)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 60000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    if (location != null) {
                        MainApplication.lastLocation = location
                    }
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                MainApplication.lastLocation = location
            }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.txtPart.requestFocus()

        binding.btnSearch.setOnClickListener {
            val text = binding.txtPart.text.toString()
            viewModel.getOnlyTechWarehousePartsForFieldTransfer(text)
        }

        binding.recItems.layoutManager = LinearLayoutManager(this)

        //checkPermission and request if not
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }

        observeLoading()
        observeGetOnlyTechWarehousePartsSuccess()
        observeNetworkError()
        observeGetPartsInTechniciansWarehousesSuccess()
        observePutTransferOrderSuccess()

    }

    private fun observePutTransferOrderSuccess() {
        viewModel.onPutTransferOrderSuccess.observe(this) {
            it?.getContentIfNotHandledOrReturnNull()?.let { success ->
                if (success) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun observeGetPartsInTechniciansWarehousesSuccess() {
        viewModel.onPartsInTechWarehousesSuccess.observe(this) {
            it?.getContentIfNotHandledOrReturnNull()?.let { list ->
                val filtered = prepareList(list.toMutableList())
                binding.recItems.adapter = RequestTransferAdapter(filtered, this)
                if (filtered.isEmpty()) {
                    showMessageForEmptyList(viewModel.itemName, viewModel.itemDescription)
                } else {
                    updateUIForList()
                }
            }
        }
    }

    private fun observeNetworkError() {
        viewModel.networkError.observe(this) {
            it?.getContentIfNotHandledOrReturnNull()?.let { pair ->
                showNetworkErrorDialog(pair, this, supportFragmentManager)
            }
        }
    }

    private fun observeGetOnlyTechWarehousePartsSuccess() {
        viewModel.onTechWarehousePartsSuccess.observe(this) {
            it?.getContentIfNotHandledOrReturnNull()?.let { list ->
                hideKeyboard(this)
                val result = list.toMutableList()
                val nameComparator = Comparator { part1: Part, part2: Part ->
                    val part1Item = part1.item ?: ""
                    val part2Item = part2.item ?: ""
                    part1Item.removePrefix("-").compareTo(part2Item.removePrefix("-"))
                }
                result.sortWith(nameComparator)

                val customBuilder: AlertDialog =
                    CustomSearchPartDialog(this, result.toMutableList(), this)
                customBuilder.show()
            }
        }
    }

    private fun observeLoading() {
        viewModel.loading.observe(this) { showProgress ->
            if (showProgress) {
                if (progressDialog?.isShowing == true) {
                    // nothing
                } else {
                    progressDialog = ProgressDialog.show(this, "", getString(R.string.loading))
                }
            } else {
                progressDialog?.dismiss()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                val permissionGranted = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED && permissionGranted
                ) {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
                return
            }
        }
    }

    override fun onPartRequestedClick(item: RequestPartTransferItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.request_part))
        val dialogView = LayoutInflater.from(this).inflate(R.layout.request_transfer_dialog, null)
        builder.setView(dialogView)
        val txtQuantity = dialogView.findViewById<EditText>(R.id.txtQuantity)
        txtQuantity.setText("1")

        builder.setPositiveButton(getString(R.string.ok), DialogInterface.OnClickListener { _, _ ->
            val quantity: Float
            try {
                quantity = java.lang.Float.parseFloat(txtQuantity.text.toString())
            } catch (e: NumberFormatException) {
                Log.e(TAG, EXCEPTION, e)
                showMessageBox(getString(R.string.error), getString(R.string.quantity_error))
                return@OnClickListener
            }

            when {
                quantity <= 0 -> showMessageBox(
                    getString(R.string.error),
                    getString(R.string.quantity_error)
                )
                quantity > item.availableQty -> showMessageBox(
                    getString(R.string.error),
                    getString(R.string.quantity_greater_than_have)
                )
                else -> requestPart(item, quantity)
            }
        })
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()
        if (dialog.window != null) {
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        dialog.show()
    }

    override fun onChatClick(item: RequestPartTransferItem) {
        val chatIdentity = item.chatIdent
        if (!chatIdentity.isNullOrBlank()) {
            val intent = Intent(this, MessengerActivity::class.java)
            intent.putExtra(Constants.EXTRA_IDENT, chatIdentity)
            intent.putExtra(Constants.EXTRA_TECH_NAME_FOR_REQUEST_PART_CHAT, item.technicianName)
            startActivity(intent)
        }
    }

    override fun onPhoneNumberClick(item: RequestPartTransferItem) {
        var phoneNumber = item.getTechnicianPhoneFixed()
        if (!phoneNumber.isNullOrBlank()) {
            phoneNumber = PhoneHelper.getOnlyNumbersBeforeDialing(phoneNumber)
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        }
    }

    private fun requestPart(item: RequestPartTransferItem, quantity: Float) {
        val map = HashMap<String, Any>()
        map["ItemId"] = item.itemId
        map["Quantity"] = quantity
        map["Description"] =
            "MobileTech Field Transfer Request from ${AppAuth.getInstance().technicianUser.technicianName} to ${item.technicianName}"
        map["DestinationTechnicianId"] = item.technicianID
        viewModel.putTransferOrder(map)
    }

    private fun searchForItem(s: Int?, itemName: String?, itemDescription: String?) {
        viewModel.itemName = itemName
        viewModel.itemDescription = itemDescription
        viewModel.getPartsInTechnicianWarehouses(s?.toString() ?: "")
    }

    private fun prepareList(filtered: MutableList<RequestPartTransferItem>): MutableList<RequestPartTransferItem> {
        val lastLocation = MainApplication.lastLocation
        for (item in filtered) {
            item.distance = item.computeDistance(lastLocation)
        }
        val itemsToDelete =
            Predicate { item: RequestPartTransferItem -> !item.isTechnicianActive || !item.isTechnicianInGroup || !item.isTechnicianRegistered }
        remove(filtered, itemsToDelete)
        return orderList(filtered)
    }

    private fun orderList(filtered: MutableList<RequestPartTransferItem>): MutableList<RequestPartTransferItem> {
        filtered.sortWith(compareBy({ !it.isValidLocation() }, { it.distance }))
        return filtered
    }

    private fun updateUIForList() {
        binding.recItems.visibility = View.VISIBLE
        binding.textViewEmptyResult.visibility = View.GONE
    }

    private fun showMessageForEmptyList(itemName: String?, itemDescription: String?) {
        binding.recItems.visibility = View.GONE
        binding.textViewEmptyResult.visibility = View.VISIBLE
        binding.textViewEmptyResult.text =
            getString(R.string.message_for_empty_results_field_transfer, itemName, itemDescription)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun <T> remove(list: MutableList<T>, predicate: Predicate<T>) {
        list.filter { predicate.test(it) }.forEach { list.remove(it) }
    }
}
