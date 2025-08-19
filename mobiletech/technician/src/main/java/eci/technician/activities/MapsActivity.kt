package eci.technician.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.collections.MarkerManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.custom.CustomRepeatedListDialog
import eci.technician.helpers.AppAuth
import eci.technician.helpers.FilterHelper
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.ClusterCustomRenderer
import eci.technician.models.ClusterObject
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.GroupCallsRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.tools.Constants
import eci.technician.viewmodels.MapsViewModel
import eci.technician.viewmodels.OrderFragmentViewModel
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.*


class MapsActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMyLocationButtonClickListener, ClusterManager.OnClusterClickListener<ClusterObject>,
        CustomRepeatedListDialog.CustomDialogListener {

    private val TAG = "MapsActivity"
    private val EXCEPTION = "Exception"
    private lateinit var mMap: GoogleMap
    private var invalidAddress: Int = 0
    private lateinit var progressDialog: ProgressDialog
    private lateinit var mLocationRequest: LocationRequest
    var mLastLocation: Location? = null
    internal var mCurrLocationMarker: Marker? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var mClusterManager: ClusterManager<ClusterObject>
    private var clusterObjects = mutableListOf<ClusterObject>()
    private lateinit var preferences: SharedPreferences
    private lateinit var serviceOrderList: MutableList<ServiceOrder>
    private lateinit var groupServiceOrderList: MutableList<GroupCallServiceOrder>
    private var limitCallCounter = 0
    private var originActivity: String? = null
    private val viewModel by lazy {
        ViewModelProvider(this)[MapsViewModel::class.java]
    }
    private val ordersViewModel: OrderFragmentViewModel by viewModels()

    private var counter = 0
    private var queryFromServiceOrderList = ""
    private var queryFromGroupCallServiceOrderList = ""
    private lateinit var removedServiceCallId: String
    private lateinit var currentMarker: Marker
    private lateinit var currentGroupCallServiceOrder: GroupCallServiceOrder
    private var currentGroupCallServiceOrderCluster: ClusterObject? = null
    private var groupCallsToDelete: String = ""
    private var groupCallsToUpdate: String = ""
    private var ismultipleClustersSelected = false
    private var backFromDetails = false

    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()
                mLastLocation = location
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker?.remove()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        progressDialog = ProgressDialog.show(this, "", getString(R.string.loading))
        object : CountDownTimer(8000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                //do nothing
            }

            override fun onFinish() {
                if (progressDialog.isShowing) {
                    val message = getString(R.string.still_working)
                    progressDialog.setMessage(message)
                }
            }
        }.start()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        super.onStart()
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.MAPS_ACTIVITY)
        preferences = getSharedPreferences("maps", Context.MODE_PRIVATE)
        setSupportActionBar(toolbar)
        title = getString(R.string.service_calls_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        originActivity = intent.getStringExtra(Constants.ORIGIN_MAP_ACTIVITY)
        queryFromServiceOrderList = intent.getStringExtra(Constants.EXTRA_QUERY_SERVICE_CALL_LIST)
                ?: ""
        queryFromGroupCallServiceOrderList =
                intent.getStringExtra(Constants.EXTRA_QUERY_GROUP_CALL_LIST)
                        ?: ""
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onResume() {
        super.onResume()
        if (this::mMap.isInitialized && !backFromDetails) {
            limitCallCounter = 0
            mMap.clear()
            clusterObjects.clear()
            val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        backFromDetails = false
    }

    private class GeolocationTask
    constructor(
            context: MapsActivity,
            var serviceOrder: ServiceOrder?,
            var groupServiceOrder: GroupCallServiceOrder?,
            var serviceOrdersSize: Int
    ) : AsyncTask<Void?, Void?, Void?>() {
        var position: LatLng? = null
        private lateinit var clusterObjectToAdd: ClusterObject
        private val activityReference: WeakReference<MapsActivity> = WeakReference(context)

        override fun doInBackground(vararg p0: Void?): Void? {
            serviceOrder?.let { serviceOrder ->
                position = activityReference.get()
                        ?.getLocationFromAddress(serviceOrder.address + ", " + serviceOrder.city + ", " + serviceOrder.state + ", " + serviceOrder.zip)
            }
            groupServiceOrder?.let { groupCallServiceOrder ->
                position = activityReference.get()
                        ?.getLocationFromAddress(groupCallServiceOrder.address + ", " + groupCallServiceOrder.city + ", " + groupCallServiceOrder.state + ", " + groupCallServiceOrder.zip)
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            activityReference.get()?.let {
                it.counter += 1
                if (position != null) {
                    serviceOrder?.let { serviceOrder ->
                        val callPriority = serviceOrder?.callPriority?.let {
                            ServiceOrderRepository.getServiceOrderPriorityById(
                                it
                            )
                        }
                        clusterObjectToAdd = ClusterObject(
                                position,
                                serviceOrder.callNumber_Code,
                                serviceOrder.statusCode_Code?.trim() ?: "",
                                serviceOrder.statusCode,
                                serviceOrder.callNumber_ID,
                                serviceOrder.id,
                                callPriority?.color?:0.0

                        )
                    }
                    groupServiceOrder?.let { groupCallServiceOrder ->
                        val callPriority = groupCallServiceOrder?.callPriority?.let {
                            ServiceOrderRepository.getServiceOrderPriorityById(
                                it
                            )
                        }
                        clusterObjectToAdd = ClusterObject(
                                position,
                                groupCallServiceOrder.callNumber_Code,
                                groupCallServiceOrder.statusCode_Code?.trim(),
                                groupCallServiceOrder.statusCode,
                                groupCallServiceOrder.callNumber_ID,
                                groupCallServiceOrder.id,
                            callPriority?.color?:0.0
                        )
                    }
                    if (!it.clusterObjects.contains(clusterObjectToAdd)) {
                        activityReference.get()?.mClusterManager?.addItem(clusterObjectToAdd)
                        activityReference.get()?.mClusterManager?.cluster()
                        it.clusterObjects.add(clusterObjectToAdd)
                    }

                }
                if (activityReference.get()?.counter == serviceOrdersSize) {
                    val bounds = LatLngBounds.builder()
                    if (it.clusterObjects.isNotEmpty()) {
                        it.clusterObjects.forEach { clusterObject ->
                            bounds.include(clusterObject.position)
                        }
                        it.mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 20))
                    }
                    it.stopShowingLoadingDialog()
                    if (it.invalidAddress > 0) {
                        Snackbar.make(
                                it.findViewById(R.id.dummy_layout_for_snackbar),
                                it.getString(R.string.map_warning, it.invalidAddress.toString()),
                                Snackbar.LENGTH_INDEFINITE
                        )
                                .show()
                    }
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mLocationRequest = LocationRequest()
        mLocationRequest.interval = Constants.TWO_MINUTES_IN_MILLIS
        mLocationRequest.fastestInterval = Constants.TWO_MINUTES_IN_MILLIS
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        mClusterManager = ClusterManager(this, mMap, object : MarkerManager(mMap) {
            override fun onMarkerClick(marker: Marker): Boolean {
                val selectedCall: ClusterObject? = marker?.tag as ClusterObject?
                marker?.let {
                    currentMarker = it
                }
                ismultipleClustersSelected = false

                currentGroupCallServiceOrderCluster = selectedCall
                selectServiceCall()
                return super.onMarkerClick(marker)
            }

        })

        mClusterManager.renderer = ClusterCustomRenderer(this, mMap, mClusterManager)

        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mFusedLocationClient?.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback,
                        Looper.getMainLooper()
                )
                mMap.isMyLocationEnabled = true
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            }
        } else {
            if (isLocationEnabled()) {
                try {
                    mFusedLocationClient?.requestLocationUpdates(
                            mLocationRequest,
                            mLocationCallback,
                            Looper.getMainLooper()
                    )
                    mMap.isMyLocationEnabled = true
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                }
            }
        }

        when (originActivity) {
            Constants.MY_SERVICE_CALLS -> {


                viewModel.getServiceOrder().observe(this, {
                    val realm = Realm.getDefaultInstance()
                    serviceOrderList = try {
                        realm.copyFromRealm(it)
                    } catch (e: Exception) {
                        Log.e(TAG, EXCEPTION, e)
                        mutableListOf()
                    } finally {
                        realm.close()
                    }

                    serviceOrderList = if (AppAuth.getInstance().technicianUser.isRestrictCallOrder) {
                        FilterHelper.filterByQuantity(serviceOrderList)
                    } else {
                        ordersViewModel.applyServiceCallFilters(serviceOrderList)
                    }

                    serviceOrderList = FilterHelper.filterServiceOrderByQueryText(
                        queryFromServiceOrderList,
                        serviceOrderList
                    )

                    limitCallCounter += serviceOrderList.size
                    if (serviceOrderList.isNotEmpty()) {
                        val filteredServiceOrderList =
                                serviceOrderList.distinctBy { filter -> filter.callNumber_ID }
                        filteredServiceOrderList.forEach { serviceOrder ->
                            try {
                                if (limitCallCounter <= serviceOrderList.size) {
                                    GeolocationTask(
                                            this@MapsActivity,
                                            serviceOrder,
                                            null,
                                            filteredServiceOrderList.size
                                    ).execute()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, EXCEPTION, e)
                                stopShowingLoadingDialog()
                            }
                        }
                        mMap.setOnCameraIdleListener(mClusterManager)
                    } else {
                        stopShowingLoadingDialog()
                        moveCameraToLastLocation()
                    }
                    mClusterManager.setOnClusterClickListener(this@MapsActivity)
                    mMap.setOnMapLoadedCallback(this@MapsActivity)

                })

            }

            Constants.GROUP_CALLS -> {

                viewModel.getGroupServiceOrder().observe(this, {
                    val realm = Realm.getDefaultInstance()
                    groupServiceOrderList = try {
                        realm.copyFromRealm(it)
                    } catch (e: Exception) {
                        mutableListOf()
                    } finally {
                        realm.close()
                    }

                    groupServiceOrderList = ordersViewModel.applyGroupServiceCallFilter(groupServiceOrderList)
                    groupServiceOrderList = FilterHelper.filterGroupServiceOrderByQueryText(queryFromGroupCallServiceOrderList, groupServiceOrderList)

                    limitCallCounter += groupServiceOrderList.size
                    if (groupServiceOrderList.isNotEmpty()) {
                        val filteredGroupServiceOrderList =
                                groupServiceOrderList.distinctBy { filter -> filter.callNumber_ID }
                        filteredGroupServiceOrderList.forEach { groupCallServiceOrder ->
                            try {
                                if (limitCallCounter <= groupServiceOrderList.size) {
                                    GeolocationTask(
                                            this,
                                            null,
                                            groupCallServiceOrder,
                                            filteredGroupServiceOrderList.size
                                    ).execute()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, EXCEPTION, e)
                                stopShowingLoadingDialog()
                            }
                        }
                        mMap.setOnCameraIdleListener(mClusterManager)
                    } else {
                        stopShowingLoadingDialog()
                        moveCameraToLastLocation()
                    }
                    mClusterManager.setOnClusterClickListener(this)
                    mMap.setOnMapLoadedCallback(this)
                })
            }
        }
    }

    private fun stopShowingLoadingDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun moveCameraToLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            // request permission
        } else {
            mFusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10f)
                mMap.animateCamera(cameraUpdate)
            }
        }
    }

    private fun getLocationFromAddress(address: String): LatLng? {
        val geoCoder = Geocoder(this, Locale.getDefault())
        var res = LatLng(0.0, 0.0)
        var storedLatLng = preferences.getString(address, "")
        if (storedLatLng != null && storedLatLng.isNotBlank()) {
            storedLatLng = storedLatLng.replace("[()]".toRegex(), "")
            storedLatLng = storedLatLng.replace("lat/lng:", "")
            val stringLatLng = storedLatLng.split(",")
            val lat = stringLatLng[0].toDouble()
            val lng = stringLatLng[1].toDouble()
            res = LatLng(lat, lng)
            return res
        } else {
            try {
                val addresses: List<Address> = geoCoder.getFromLocationName(address, 1)
                if (addresses.isNotEmpty()) {
                    res = LatLng(addresses[0].latitude, addresses[0].longitude)
                    preferences.edit().putString(address, res.toString()).apply()
                } else {
                    invalidAddress += 1
                    return null
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            }
        }
        return res
    }

    fun selectServiceCall() {
        var serviceOrder = currentGroupCallServiceOrderCluster
        serviceOrder?.let {
            var title = serviceOrder.callNumber_Code
            if (serviceOrder.status_StatusCode == "H" || serviceOrder.status_StatusCode == "S") {
                title = "${serviceOrder.callNumber_Code} (${serviceOrder.statusCode})"
            }
            showQuestionBox(
                    title,
                    getString(R.string.sc_map_question, serviceOrder.callNumber_Code)
            ) { _, _ ->

                when (originActivity) {
                    Constants.MY_SERVICE_CALLS -> {
                        val intent = Intent(this, OrderDetailActivity::class.java)
                        intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrder.id)
                        intent.putExtra(Constants.EXTRA_CALL_NUMBER_ID, serviceOrder.callNumber_ID)
                        startActivity(intent)
                    }

                    Constants.GROUP_CALLS -> {
                        tryReAssignCall(serviceOrder)
                    }
                }
            }
        }
    }

    private fun tryReAssignCall(serviceOrder: ClusterObject) {
        serviceOrder.callNumber_ID?.let { serviceOrderCallNumberId ->
            RetrofitRepository.RetrofitRepositoryObject
                    .getInstance()
                    .getOneGroupCallServiceByCallId(serviceOrderCallNumberId, lifecycleScope, this)
                    .observe(this, {
                        manageOneServiceCallResponse(it, serviceOrder)
                    })
        }
    }


    private fun manageOneServiceCallResponse(
            genericDataResponse: GenericDataResponse<MutableList<GroupCallServiceOrder>>,
            groupCallServiceOrder: ClusterObject
    ) {
        when (genericDataResponse.responseType) {
            RequestStatus.SUCCESS -> {
                genericDataResponse.data?.let {
                    currentGroupCallServiceOrderCluster = groupCallServiceOrder
                    if (it.isEmpty()) {
                        showAffirmationBox(
                                getString(R.string.warning),
                                getString(
                                        R.string.the_service_call_was_canceled_or_completed,
                                        groupCallServiceOrder.callNumber_Code
                                )
                        )
                        { _, _: Int ->
                            removeMarker(groupCallServiceOrder)
                            removeCurrentGroupCallServiceOrder()
                        }
                        return
                    }
                    currentGroupCallServiceOrder = it.first()
                    updateGroupCallServiceOrder(currentGroupCallServiceOrder)
                    val intent = Intent(this@MapsActivity, GroupCallDetailsActivity::class.java)
                    intent.putExtra(
                            Constants.SERVICE_ORDER_ID,
                            groupCallServiceOrder.callNumber_ID
                    )
                    startActivityForResult(intent, Constants.ACTIVITY_GROUP_CALLS_DETAIL)
                }
            }
            RequestStatus.ERROR -> {
                showMessageBox(
                        resources.getString(R.string.something_went_wrong_connection),
                        "The service Call was " + groupCallServiceOrder.statusCode.toString()
                )
            }
        }
    }

    private fun updateGroupCallServiceOrder(newGroupCallServiceOrder: GroupCallServiceOrder) {
        groupCallsToUpdate += "${newGroupCallServiceOrder.callNumber_Code}$"
        lifecycleScope.launch(Dispatchers.IO) {
            currentGroupCallServiceOrderCluster?.let {
                GroupCallsRepository
                        .updateReassignedGroupCallServiceOrder(newGroupCallServiceOrder)
            }
        }
    }

    private fun removeCurrentGroupCallServiceOrder() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentGroupCallServiceOrderCluster?.let {
                GroupCallsRepository
                        .deleteGroupCallServiceOrderByID(it.callNumber_ID)
            }
        }
    }

    private fun removeMarker(
            groupCallServiceOrder: ClusterObject
    ) {
        groupCallsToDelete += "${groupCallServiceOrder.callNumber_Code}$"
        if (ismultipleClustersSelected) {
            mClusterManager.removeItem(groupCallServiceOrder)
        } else {
            currentMarker.remove()
        }
        mClusterManager.cluster()
    }

    override fun onMapLoaded() {
        //do nothing
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED && (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) ==
                                PackageManager.PERMISSION_GRANTED)
                ) {
                    try {
                        mFusedLocationClient?.requestLocationUpdates(
                                mLocationRequest,
                                mLocationCallback,
                                Looper.getMainLooper()
                        )
                        mMap.isMyLocationEnabled = true

                    } catch (e: Exception) {
                        Log.e(TAG, EXCEPTION, e)
                    }
                }
                return
            }
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onClusterClick(cluster: Cluster<ClusterObject>?): Boolean {
        val zoom: Float = mMap.cameraPosition.zoom
        if (zoom < Constants.MAX_ZOOM) {
            val builder = LatLngBounds.builder()
            val filteredClusterItems = cluster?.items?.distinctBy { it.callNumber_ID }
            filteredClusterItems?.forEach {
                builder.include(it.position)
            }
            val bounds = builder.build()
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250))

        } else {
            cluster?.let {
                val clusterItems = cluster.items.distinctBy { it.callNumber_ID }
                val clusterItemsToCompareTo = cluster.items.distinctBy { it.callNumber_ID }
                val sameLocationMarkers = mutableListOf<ClusterObject>()
                clusterItems.forEach { fromNewList ->
                    val sameLocationCounter = clusterItemsToCompareTo.count {
                        it.position.latitude == fromNewList.position.latitude && it.position.longitude == fromNewList.position.longitude
                    }
                    if (sameLocationCounter >= 2) {
                        sameLocationMarkers.add(fromNewList)
                    }
                }

                if (sameLocationMarkers.isNotEmpty()) {
                    val filteredMarkers = sameLocationMarkers.distinctBy { it.callNumber_ID }
                    val customBuilder: AlertDialog =
                            CustomRepeatedListDialog(this, filteredMarkers.toMutableList(), this)
                    customBuilder.show()
                }
            }
        }
        return true
    }

    override fun onSelectItem(item: ClusterObject) {
        ismultipleClustersSelected = true
        currentGroupCallServiceOrderCluster = item
        selectServiceCall()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Constants.ACTIVITY_GROUP_CALLS_DETAIL_DELETE) {
            currentGroupCallServiceOrderCluster?.let { removeMarker(it) }
            removeCurrentGroupCallServiceOrder()
            mClusterManager.cluster()
            backFromDetails = true
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun finish() {
        val intent = Intent()
        intent.putExtra(Constants.ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_DELETE, groupCallsToDelete)
        intent.putExtra(Constants.ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_UPDATE, groupCallsToUpdate)
        setResult(Constants.ACTIVITY_GROUP_CALLS_MAP_LIST_DELETE, intent)
        super.finish()
    }

}

