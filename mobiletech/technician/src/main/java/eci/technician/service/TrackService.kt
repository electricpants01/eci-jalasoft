package eci.technician.service

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.MainActivity
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.notification.NotificationHelper
import eci.technician.models.gps.UpdatePosition
import eci.technician.workers.serviceOrderQueue.ServiceOrderOfflineUtils.retrieveUserData
import java.util.*


class TrackService() : Service() {

    companion object {

        private const val CAR_ID_PARAM = "car_id"
        private const val UPDATE_INTERVAL_PARAM = "update_interval"
        private const val TAG = "TRACK_SERVICE"
        private const val EXCEPTION = "EXCEPTION"

        private const val GPS_ERROR_CODE = 5035

        fun startTrackingService(context: Context, carId: UUID, updateInterval: Int) {
            val serviceIntent = Intent(context, TrackService::class.java)
            serviceIntent.putExtra(UPDATE_INTERVAL_PARAM, updateInterval)
            serviceIntent.putExtra(CAR_ID_PARAM, carId)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        fun stopTrackingService(context: Context) {
            val serviceIntent = Intent(context, TrackService::class.java)
            context.stopService(serviceIntent)
        }

    }

    internal var mFusedLocationClient: FusedLocationProviderClient? = null

    lateinit var mLocationRequest: LocationRequest

    private var isCoordinatesSending = false

    private lateinit var locationManager: LocationManager

    var mLastLocation: Location? = null

    private var carId: UUID? = null

    private var updateInterval: Int = 0

    internal var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()
                mLastLocation = location
                MainApplication.lastLocation = location
                MainApplication.locations.add(
                    UpdatePosition(
                        carId,
                        location.latitude,
                        location.longitude,
                        location.speed.toDouble(),
                        location.altitude,
                        Date()
                    )
                )
                sendCoordinates()
            } else {
                getLastLocation()
            }
        }
    }

    private fun getLastLocation() {
        try {
            mFusedLocationClient?.lastLocation?.addOnCompleteListener { p0 ->
                if (p0.isSuccessful && p0.result != null) {
                    val lastLocation = p0.result;
                    MainApplication.lastLocation = lastLocation
                    MainApplication.locations.add(
                        UpdatePosition(
                            carId,
                            lastLocation.latitude,
                            lastLocation.longitude,
                            lastLocation.speed.toDouble(),
                            lastLocation.altitude,
                            Date()
                        )
                    )
                    sendCoordinates()
                } else {
                    Log.w("TrackService", "Failed to get location.");
                }
            }
        } catch (unlikely: SecurityException) {
            Log.e("TrackService", "Lost location permission.$unlikely")
        }
    }


    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            MainApplication.lastLocation = location
            MainApplication.locations.add(
                UpdatePosition(
                    carId,
                    location.latitude,
                    location.longitude,
                    location.speed.toDouble(),
                    location.altitude,
                    Date()
                )
            )

            sendCoordinates()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {

        }
    }

    private fun sendCoordinates() {
        Thread(sendCoordinatesRunnable).start()
    }

    private val sendCoordinatesRunnable = Runnable {
        if (isCoordinatesSending) {
            return@Runnable
        }
        isCoordinatesSending = true
        while (MainApplication.locations.isNotEmpty()) {
            val location = MainApplication.locations[0]
            try {
                updateGPS(location)
                MainApplication.locations.remove(location)
            } catch (e: Exception) {
                e.printStackTrace()
                scheduleSendCoordinates()
                break
            }
        }
        isCoordinatesSending = false
    }

    private fun updateGPS(updatePosition: UpdatePosition) {
        val technicianUser = AppAuth.getInstance().technicianUser
        val techId = technicianUser.technicianNumber
        updatePosition.setTechnicianId(techId)
        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .updateGpsLocation(updatePosition) {
                Log.d(TAG,"Updating location gps "+ updatePosition.latitude + " "+updatePosition.longitude)
                if(it?.errors?.size?:0 > 0){
                    val error = it?.errors?.get(0)
                    error?.let {
                        if (error.errorCode == GPS_ERROR_CODE) {
                            Log.d(TAG, error.errorText)
                            val analyticsBundle = Bundle()
                            analyticsBundle.putString(TAG, "Tech ID:"+techId+" error:"+error.errorText)
                            FirebaseAnalytics.getInstance(applicationContext)
                                .logEvent(TAG, analyticsBundle)
                        }
                    }

                }
            }
    }

    private fun scheduleSendCoordinates() {
        Handler(mainLooper).postDelayed(Runnable { sendCoordinates() }, (1000 * 60 * 5).toLong())
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        carId = intent?.getSerializableExtra(CAR_ID_PARAM) as? UUID
        updateInterval = intent?.getIntExtra(UPDATE_INTERVAL_PARAM, 60 * 5) ?: 60 * 5

        mLocationRequest = LocationRequest()
        mLocationRequest.interval = (updateInterval * 1000).toLong()
        mLocationRequest.fastestInterval = (updateInterval * 1000).toLong()
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper.registerChannel(this)
        val notification = NotificationHelper.generateNotification(
            this,
            pendingIntent,
            getString(R.string.notification_gps_title),
            getString(R.string.notification_gps_title),
            getString(R.string.notification_gps_description),
            R.drawable.ic_new_notification,
            false
        )


        startForeground(
            NotificationHelper.getTrackUserNotificationId(),
            notification
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {
                mFusedLocationClient?.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.getMainLooper()
                )
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        MainApplication.locations.clear()
        mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        locationManager.removeUpdates(locationListener)
    }
}