package eci.technician.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.JsonElement
import eci.signalr.ConnectionListener
import eci.signalr.event.SignalREventListener
import eci.technician.MainApplication
import eci.technician.helpers.AppAuth
import eci.technician.helpers.AppAuth.AuthStateListener
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository.RetrofitRepositoryObject.getInstance
import eci.technician.models.TechnicianUser
import eci.technician.models.field_transfer.PartRequestTransfer
import eci.technician.models.gps.CarInfo
import eci.technician.models.order.ServiceCallLabor
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.*
import eci.technician.repository.PartsRepository.getWarehousePartsById
import eci.technician.repository.ServiceOrderRepository.saveServiceOrderFromResponse
import eci.technician.service.TrackService.Companion.startTrackingService
import eci.technician.service.TrackService.Companion.stopTrackingService
import eci.technician.tools.Constants
import eci.technician.tools.PermissionHelper.verifyTrackPermissions
import eci.technician.tools.Settings
import eci.technician.workers.OfflineManager.retryAttachmentWorker
import eci.technician.workers.OfflineManager.retryWorker
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import microsoft.aspnet.signalr.client.ConnectionState

class TechnicianService : Service(), SignalREventListener,
    ConnectionListener, AuthStateListener {
    private lateinit var realm: Realm
    private var car: CarInfo? = null
    private var waitingForCar = false
    private val CLOCK_OUT_STATE = 2
    private val unsyncRequested = false
    private val checkRequestsCountRunnable: Runnable = object : Runnable {
        override fun run() {
            getInstance().getPartsRequestsFromMe()
                .observeForever { partRequestsFromMe: List<PartRequestTransfer?>? ->
                    AppAuth.getInstance().processRequestPartsRequestsFromMe(partRequestsFromMe)
                }
            Handler(mainLooper).postDelayed(this, 1000L * 60 * 2)
        }
    }
    private val checkUnSyncData: Runnable = object : Runnable {
        override fun run() {
            val serviceOrderRequestsList =
                DatabaseRepository.getInstance().incompleteRequestList.size
            val attachmentsList =
                DatabaseRepository.getInstance().attachmentIncompleteRequestList.size
            if (attachmentsList + serviceOrderRequestsList > 0) {
                retryAttachmentWorker(applicationContext)
                retryWorker(applicationContext)
                val counter = AppAuth.getInstance().syncCounter
                AppAuth.getInstance().syncCounter = counter + 1
                if (AppAuth.getInstance().syncCounter > 15) {
                    Handler(mainLooper).postDelayed(this, 1000L * 60 * 60)
                } else if (AppAuth.getInstance().syncCounter > 5) {
                    Handler(mainLooper).postDelayed(this, 1000L * 60 * 5)
                } else {
                    Handler(mainLooper).postDelayed(this, 1000L * 60 * 2)
                }
            } else {
                AppAuth.getInstance().syncCounter = 0
                Handler(mainLooper).postDelayed(this, 1000L * 60 * 2)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Started")
        try {
            MainApplication.connection?.let { myConnection ->
                myConnection.addEventListener(this)
                myConnection.addConnectionListener(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        AppAuth.getInstance().addUserUpdateListener(this)
        realm = Realm.getDefaultInstance()
        checkUnSyncData.run()
        loadServiceData()
        checkCar()
    }

    private fun loadServiceData() {
        loadAllCallPriorities()
        loadAllEquipmentMeters()
        loadShifts()
        loadProblemCodes()
        loadRepairCodes()
        loadHoldCodes()
        loadCancelCodes()
        loadActivityCallTypes()
        loadAllPartsForTechnicianWarehouse()
        loadTechnicians()
        loadAvailablePartsForNeededParts()
        loadIncompleteCodes()
        loadAllCallTypes()
        loadAllNoteTypes()
    }

    private fun loadAllCallPriorities() {
        CoroutineScope(Dispatchers.IO).launch {
            ServiceOrderRepository.getAllCallPriorities().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadAllNoteTypes() {
        CoroutineScope(Dispatchers.IO).launch {
            ServiceCallNotesRepository.getServiceCallNotesTypes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadAllCallTypes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getAllCallTypes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadIncompleteCodes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getAllIncompleteCodes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadAllEquipmentMeters() {
        CoroutineScope(Dispatchers.IO).launch {
            PartsRepository.getAllEquipmentMeters().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadAvailablePartsForNeededParts() {
        CoroutineScope(Dispatchers.IO).launch {
            PartsRepository.getAllPartsFromAllWarehouses().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    /**
     * GetAvailablePartsForOffline
     */
    private fun loadAllPartsForTechnicianWarehouse() {
        CoroutineScope(Dispatchers.IO).launch {
            PartsRepository.getAvailablePartsByWarehouseForOffline(forceUpdate = false)
                .collect { response ->
                    when (response) {
                        is Resource.Error ->
                            Resource.logError(response.error, TAG, EXCEPTION)
                        is Resource.Loading -> {/* Not needed in background tasks */
                        }
                        is Resource.Success -> {/* Not needed  */
                        }
                    }
                }
        }
    }

    private fun loadTechnicians() {
        CoroutineScope(Dispatchers.IO).launch {
            AssistantRepository.getAllAssistanceList().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadHoldCodes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getHoldCodes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadActivityCallTypes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getAllActivityCallTypes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadShifts() {
        CoroutineScope(Dispatchers.IO).launch {
            TechnicianTimeRepository.fetchShifts().collect {
                if (it is Resource.Error){
                    Resource.logError(it.error, TAG, EXCEPTION)
                }
            }
        }
    }

    private fun loadCancelCodes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getCancelCodes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadRepairCodes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getServiceCallRepairCodes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun loadProblemCodes() {
        CoroutineScope(Dispatchers.IO).launch {
            CodesRepository.getServiceCallProblemCodes().collect { response ->
                when (response) {
                    is Resource.Error ->
                        Resource.logError(response.error, TAG, EXCEPTION)
                    is Resource.Loading -> {/* Not needed in background tasks */
                    }
                    is Resource.Success -> {/* Not needed  */
                    }
                }
            }
        }
    }

    private fun checkCar() {
        if (!AppAuth.getInstance().isLoggedIn || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            !AppAuth.getInstance().technicianUser.isCanUseGps || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        Log.d(TAG, "Checking car")
        getInstance().checkCar { genericDataResponse: GenericDataResponse<CarInfo>? ->
            if (genericDataResponse != null) {
                when (genericDataResponse.responseType) {
                    RequestStatus.SUCCESS -> {
                        car = genericDataResponse.data
                        if (waitingForCar) {
                            waitingForCar = false
                            car?.let { car ->
                                startTrackingService(
                                    this@TechnicianService,
                                    car.id,
                                    car.updateInterval
                                )
                            }
                        }
                    }
                    else -> {
                        if (genericDataResponse.responseType === RequestStatus.ERROR) {
                            Log.d(
                                TAG,
                                EXCEPTION
                            )
                        }
                        scheduleCheckCar()
                    }
                }
            }
            null
        }
    }

    private fun scheduleCheckCar() {
        Handler(Looper.getMainLooper()).postDelayed({ checkCar() }, 1000L * 60 * 5)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Stopped")
        try {
            MainApplication.connection?.let { myConnection ->
                myConnection.removeEventListener(this)
                myConnection.removeConnectionListener(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        AppAuth.getInstance().removeUserUpdateListener(this)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(Constants.MESSAGES_NOTIFICATION_ID)
        realm.close()
    }

    override fun serviceCallUpdated(jsonElement: JsonElement?) {
        CoroutineScope(Dispatchers.IO).launch {
            updateServiceOrder(jsonElement)
        }
    }

    override fun newServiceCall(jsonElement: JsonElement) {
        if (AppAuth.getInstance().technicianUser.isRestrictCallOrder) {
            CoroutineScope(Dispatchers.IO).launch {
                getInstance().getTechnicianActiveServiceCallsFlow(forceUpdate = true).collect { }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                updateServiceOrder(jsonElement)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val serviceOrder = Settings.createGson().fromJson(
                jsonElement,
                ServiceOrder::class.java
            )
            if (serviceOrder != null) {
                if (serviceOrder.equipmentId > 0) {
                    getInstance().getEquipmentMetersByEquipmentId(serviceOrder.equipmentId)
                }
            }
        }
    }

    suspend fun updateServiceOrder(jsonElement: JsonElement?) {
        if (jsonElement == null) return
        val realm1 = Realm.getDefaultInstance()
        try {
            val serviceOrder = Settings.createGson().fromJson(
                jsonElement,
                ServiceOrder::class.java
            ) ?: return
            val dbServiceOrder = realm1.where(ServiceOrder::class.java)
                .equalTo(ServiceOrder.CALL_NUMBER_ID, serviceOrder.callNumber_ID).findFirst()
            when (serviceOrder.statusCode_Code?.trim { it <= ' ' } ?: "") {
                "P", "D", "S", "H" -> {
                    if (serviceOrder.labors == null) {
                        serviceOrder.labors = ArrayList()
                    }
                    if (serviceOrder.parts == null) {
                        serviceOrder.parts = ArrayList()
                    }
                    if (dbServiceOrder == null) {// It is a new service call
                        getWarehousePartsById(serviceOrder.customerWarehouseId, true)
                        if (AppAuth.getInstance().technicianUser.isRestrictCallOrder) {
                            getInstance().getTechnicianActiveServiceCallsFlow(forceUpdate = true)
                                .collect { }
                        } else {
                            saveServiceOrderFromResponse(serviceOrder, false)
                        }
                    } else {
                        saveServiceOrderFromResponse(serviceOrder, false)
                    }
                }
                /**
                 * else includes "X"  status
                 */
                else -> {
                    if (dbServiceOrder != null) {
                        DatabaseRepository.getInstance().deleteUsedParts(serviceOrder.callNumber_ID)
                        realm1.executeTransaction { realm: Realm ->
                            val labors =
                                realm.where(
                                    ServiceCallLabor::class.java
                                ).equalTo(
                                    ServiceCallLabor.CALL_ID,
                                    dbServiceOrder.callNumber_ID
                                )
                                    .findAll()
                            labors.deleteAllFromRealm()
                            dbServiceOrder.completedCall = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm1.close()
        }
    }

    override fun serviceCallsUpdated() {
        // do nothing
    }

    override fun userStatusChanged(status: String) {
        AppAuth.getInstance().changeUserStatus(status)
    }

    override fun connected() {
        CoroutineScope(Dispatchers.IO).launch {
            LoginRepository.getUserInfo().collect { }
        }
    }

    override fun disconnected() {
        //do nothing
    }

    override fun stateChanged(oldState: ConnectionState, newState: ConnectionState) {
        // do nothing
    }

    override fun reconnecting() {
        // do nothing
    }

    override fun authStateChanged(appAuth: AppAuth) {
        if (!appAuth.isLoggedIn) {
            stopSelf()
        } else {
            checkRequestsCountRunnable.run()
        }
    }

    override fun userUpdated(technicianUser: TechnicianUser) {
        val appAuth = AppAuth.getInstance()
        waitingForCar = false
        if (appAuth.isLoggedIn && technicianUser.state != CLOCK_OUT_STATE && verifyTrackPermissions(
                this
            )
        ) {
            if (car == null) {
                waitingForCar = true
            } else {
                car?.let { car ->
                    startTrackingService(this, car.id, car.updateInterval)
                }
            }
        } else {
            stopTrackingService(this)
        }
    }

    override fun gpsStateChanged(state: Boolean) {
        // do nothing
    }

    override fun requestsChanged(count: Int) {
        // do nothing
    }

    companion object {
        private const val TAG = "TechnicianService"
        private const val EXCEPTION = "Exception logger"
    }
}