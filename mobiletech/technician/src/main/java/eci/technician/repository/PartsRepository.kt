@file:Suppress("RedundantSuspendModifier")

package eci.technician.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ErrorHelper.RequestCodeHandler
import eci.technician.helpers.api.UpdateManager
import eci.technician.helpers.api.retroapi.*
import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.models.ProcessingResult
import eci.technician.models.RequestPart
import eci.technician.models.data.UsedPart
import eci.technician.models.field_transfer.PartRequestTransfer
import eci.technician.models.field_transfer.PostCancelOrderModel
import eci.technician.models.order.*
import eci.technician.repository.LastUpdateRepository.LastUpdateKeys
import eci.technician.repository.LastUpdateRepository.cacheHasExpired
import eci.technician.tools.Settings
import io.realm.Realm
import io.realm.RealmList
import io.realm.Sort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

object PartsRepository {
    const val TAG = "PartsRepository"
    const val EXCEPTION = "Exception"

    /**
     * Used to get the list of parts fot the FieldTransfer -> NewRequest -> New Transfer Request
     */
    suspend fun getOnlyTechWarehousePartsForFieldTransfer(partCode: String): Flow<Resource<List<Part>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall { api.getOnlyTechWarehouseParts(partCode = partCode) })
        }.flowOn(Dispatchers.IO)
            .catch { emit(Resource.Error("", null, Pair(ErrorType.SOMETHING_WENT_WRONG, ""))) }
    }

    /**
     * Used to get the list of technicians that have the sent itemId
     * @return A list of technicians with the item available quantity, the distance, and the chat id
     */
    suspend fun getPartsInTechnicianWarehouses(itemId: String): Flow<Resource<List<RequestPartTransferItem>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall { api.getPartsInTechniciansWarehouses(itemId = itemId) })
        }.flowOn(Dispatchers.IO)
            .catch { emit(Resource.Error("", null, Pair(ErrorType.SOMETHING_WENT_WRONG, ""))) }
    }

    suspend fun putTransferOrder(map: HashMap<String, Any>): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())

            val resource = safeCall { api.putTransferOrder(map) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
    }


    /**
     * Used for Request Parts Screen and Needed Parts Screen
     * ServiceCall/GetAvailablePartsFullList
     */
    @Synchronized
    suspend fun getAllPartsFromAllWarehouses(
        partCode: String = "",
        available: Boolean = false,
        forceUpdate: Boolean = false
    ): Flow<Resource<ProcessingResult>> {
        if (forceUpdate || cacheHasExpired(LastUpdateKeys.neededPartsConfiguration())) {
            return flow {
                val api = RetrofitApiHelper.getApi() ?: return@flow
                emit(Resource.Loading())
                val resource = safeCall { api.getAllPartsFromAllWarehouses(partCode, available) }
                if (resource is Resource.Success) {
                    val response = resource.data ?: return@flow
                    if (response.isHasError) {
                        emit(Resource.getProcessingResultError<ProcessingResult>(response))
                    } else {
                        val list = createPartListFromJSON(response.result)
                        updateAllPartsFromRequest(list.toMutableList())
                        LastUpdateRepository.updateCacheDate(LastUpdateKeys.neededPartsConfiguration())
                        emit(resource)
                    }
                } else {
                    emit(resource)
                }
            }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
        } else {
            return flow { }
        }


    }

    /**
     * Used for Search Warehouses screen
     */
    suspend fun getAllAvailablePartsFromAllWarehouses(partCode: String = ""): Flow<Resource<List<Part>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall {
                api.getAllAvailablePartsFromAllWarehouses(partCode = partCode)
            })
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Used to load all parts for offline
     */
    suspend fun getAvailablePartsByWarehouseForOffline(forceUpdate: Boolean): Flow<Resource<ProcessingResult>> {
        val configuration = LastUpdateKeys.myWarehousePartsConfiguration()
        return getAvailablePartsByWarehouse(
            warehouseId = 0,
            availableQuantity = true,
            availableInTechWarehouse = false,
            availableInLinked = true,
            forceUpdate = forceUpdate,
            configuration
        )
    }

    suspend fun getAvailablePartsByWarehouseForTech(
        forceUpdate: Boolean,
    ): Flow<Resource<ProcessingResult>> {
        val currentTechWarehouse = AppAuth.getInstance().technicianUser.warehouseId
        val configuration =
            LastUpdateKeys.myWarehousePartsConfigurationWithId(currentTechWarehouse)
        return getAvailablePartsByWarehouse(
            warehouseId = currentTechWarehouse,
            availableQuantity = true,
            availableInTechWarehouse = false,
            availableInLinked = false,
            forceUpdate = forceUpdate,
            configuration
        )
    }

    private fun getAvailablePartsByWarehouse(
        warehouseId: Int,
        availableQuantity: Boolean,
        availableInTechWarehouse: Boolean,
        availableInLinked: Boolean,
        forceUpdate: Boolean = true,
        configuration: LastUpdateRepository.DBCacheConfiguration
    ): Flow<Resource<ProcessingResult>> {
        val hasExpired = cacheHasExpired(configuration)
        var lastUpdate =
            if (hasExpired) LastUpdateRepository.getLastUpdateString(configuration) else ""
        lastUpdate = if (forceUpdate) "" else lastUpdate
        /**
         * If we send a lastUpdate value, we can receive parts with availableQuantity = false,
         * because we will receive updates for parts that are with quantity 0
         */
        val newAvailableQuantity = if (lastUpdate.isEmpty()) availableQuantity else false
        if (forceUpdate || hasExpired) {
            return flow {
                val api = RetrofitApiHelper.getApi() ?: return@flow
                emit(Resource.Loading())
                val resource = safeCall {
                    api.getAvailablePartsByWarehouseForCurrentTech2(
                        warehouseId = warehouseId,
                        availableQtty = newAvailableQuantity,
                        availableInTechWarehouse = availableInTechWarehouse,
                        availableInLinked = availableInLinked,
                        lastUpdate = lastUpdate
                    )
                }
                if (resource is Resource.Success) {
                    val response = resource.data ?: return@flow
                    if (response.isHasError) {
                        emit(Resource.getProcessingResultError<ProcessingResult>(response))
                    } else {
                        val resultBody = response.result
                        val listOfParts = createTechnicianWarehousePartListFromJSON(resultBody)
                        updateTechnicianWarehousePartFromRequest(
                            listOfParts,
                            warehouseId,
                            configuration,
                            lastUpdate
                        )
                        emit(resource)
                    }
                } else {
                    emit(resource)
                }
            }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
        } else {
            return flow { }
        }

    }

    @Synchronized
    suspend fun cancelTransferOrder(cancelModel: PostCancelOrderModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall {
                api.cancelTransferOrder(cancelModel)
            }.also {
                if (it is Resource.Success) {
                    val response = it.data ?: return@also
                    if (response.isHasError) {
                        emit(
                            Resource.Error(
                                "",
                                pair = Pair(
                                    ErrorType.BACKEND_ERROR,
                                    "${response.formattedErrors} (Server Error)"
                                )
                            )
                        )
                    }
                }
            })
        }.flowOn(Dispatchers.IO)
    }

    @Synchronized
    suspend fun postTransferRequest(value: Int): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall {
                api.postTransferOrder(value)
            }.also {
                if (it is Resource.Success) {
                    val response = it.data ?: return@also
                    if (response.isHasError) {
                        emit(
                            Resource.Error(
                                "",
                                pair = Pair(
                                    ErrorType.BACKEND_ERROR,
                                    "${response.formattedErrors} (Server Error)"
                                )
                            )
                        )
                    }
                }
            })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getMyPartRequests(): Flow<Resource<List<PartRequestTransfer>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall {
                api.getMyPartRequest()
            })
        }.flowOn(Dispatchers.IO)
    }

    @Synchronized
    suspend fun getAllEquipmentMeters(): Flow<Resource<List<EquipmentRealmMeter>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource =
                safeCall { api.getAllEquipmentMeters2(AppAuth.getInstance().technicianUser.technicianNumber) }
            if (resource is Resource.Success) {
                resource.data?.let { allEquipmentMeters ->
                    updateEquipmentMeters(allEquipmentMeters)
                }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun updateEquipmentMeters(allEquipmentMeters: List<EquipmentRealmMeter>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(EquipmentRealmMeter::class.java)
                realm.insertOrUpdate(allEquipmentMeters)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun getPartRequestFromMe(): Flow<Resource<List<PartRequestTransfer>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            emit(safeCall {
                api.getPartsRequestsFromMe2()
            })
        }.flowOn(Dispatchers.IO)
    }

    @Synchronized
    fun getWarehousePartsById(
        warehouseId: Int,
        savePartInDB: Boolean
    ): MutableLiveData<GenericDataResponse<ProcessingResult>> {
        val mGenericDataResponse: MutableLiveData<GenericDataResponse<ProcessingResult>> =
            MutableLiveData()
        RetrofitApiHelper.getApi()?.getWarehousePartsById(warehouseId.toString())
            ?.enqueue(object : Callback<ProcessingResult> {
                override fun onResponse(
                    call: Call<ProcessingResult>,
                    response: Response<ProcessingResult>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { processingResult ->

                            if (savePartInDB && !processingResult.isHasError && processingResult.result != null) {
                                val list =
                                    createTechnicianWarehousePartListFromJSON(processingResult.result)
                                list.forEach { it.generateCustomId() }
                                saveTechnicianWarehouseParts(list)
                            }

                            mGenericDataResponse.value = GenericDataResponse(
                                RequestStatus.SUCCESS,
                                processingResult,
                                null
                            )
                        }
                    } else {
                        mGenericDataResponse.value = GenericDataResponse(
                            RequestStatus.ERROR,
                            null,
                            RequestCodeHandler.getMessageErrorFromResponse(response, null)
                        )
                    }
                }

                override fun onFailure(call: Call<ProcessingResult>, t: Throwable) {
                    mGenericDataResponse.value = GenericDataResponse(
                        RequestStatus.ERROR,
                        null,
                        RequestCodeHandler.getMessageErrorOnFailure(t)
                    )
                }
            })
        return mGenericDataResponse
    }

    suspend fun getUsedPartsByCallIdFromServer(callId: Int): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getUsedPartsByCallId(callId) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    val list = createUsedPartListFromJSON(response.result)
                    UpdateManager.updatePartsForOneCall(callId, list)
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestPartsMaterial(list: MutableList<RequestPart>): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.addRequestMaterial(list) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
    }

    /**
     * @param resultBody is the jsonString that comes from the ProcessingResult class,
     * and contains the list of Parts
     */
    private fun createPartListFromJSON(resultBody: String): MutableList<Part> {
        var list = mutableListOf<Part>()
        try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<Part>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    /**
     * @param resultBody is the jsonString that comes from the ProcessingResult class,
     * and contains the list of UsedParts
     */
    private fun createUsedPartListFromJSON(resultBody: String): MutableList<UsedPart> {
        var list = mutableListOf<UsedPart>()
        try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<UsedPart>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    /**
     * @param resultBody is the jsonString that comes from the ProcessingResult class,
     * and contains the list of TechnicianWarehousePart
     */
    fun createTechnicianWarehousePartListFromJSON(resultBody: String): MutableList<TechnicianWarehousePart> {
        var list = mutableListOf<TechnicianWarehousePart>()
        try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<TechnicianWarehousePart>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    private suspend fun updateTechnicianWarehousePartFromRequest(
        technicianWarehouseParts: MutableList<TechnicianWarehousePart>,
        warehouseId: Int,
        configuration: LastUpdateRepository.DBCacheConfiguration,
        lastUpdate: String
    ) {
        technicianWarehouseParts.forEach { it.generateCustomId() }

        /**
         * If the lastUpdate value is empty it means this receive all the Parts as usual
         * if it isn't , It just receive the updated parts since the last update (It shouldn't delete parts in this scenario
         * because it only receives updated parts, and it does not know which parts have been deleted)
         */
        if (lastUpdate.isEmpty()) { // receive all parts
            if (warehouseId == 0) {
                deleteTechnicianWarehouseParts(technicianWarehouseParts)
            } else {
                deleteTechnicianWarehousePartsFromCurrentTech(technicianWarehouseParts, warehouseId)
            }
        }
        saveTechnicianWarehouseParts(technicianWarehouseParts)
        LastUpdateRepository.updateCacheDate(configuration)
    }

    /**
     * Delete parts that are deleted/removed/used in EA and does not come in the response
     */
    private fun deleteTechnicianWarehousePartsFromCurrentTech(
        technicianWarehouseParts: MutableList<TechnicianWarehousePart>,
        warehouseId: Int
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val partsToDelete = realm.where(TechnicianWarehousePart::class.java)
                    .equalTo(TechnicianWarehousePart.COLUMNS.WAREHOUSE_ID, warehouseId)
                    .not()
                    .`in`(
                        TechnicianWarehousePart.COLUMNS.CUSTOM_ID,
                        technicianWarehouseParts.map { it.customId }.toTypedArray()
                    )
                    .findAll()
                for (technicianPart in partsToDelete) {
                    technicianPart?.deleteFromRealm()
                }
            }
        } catch (e: Exception) {
            Log.d(UpdateManager.TAG, UpdateManager.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    /**
     * Delete parts that are deleted/removed/used in EA and does not come in the response
     */
    private fun deleteTechnicianWarehouseParts(technicianWarehouseParts: MutableList<TechnicianWarehousePart>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val partsToDelete = realm.where(TechnicianWarehousePart::class.java)
                    .not()
                    .`in`(
                        TechnicianWarehousePart.COLUMNS.CUSTOM_ID,
                        technicianWarehouseParts.map { it.customId }.toTypedArray()
                    )
                    .findAll()
                for (technicianPart in partsToDelete) {
                    technicianPart?.deleteFromRealm()
                }
            }
        } catch (e: Exception) {
            Log.d(UpdateManager.TAG, UpdateManager.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    /**
     * Use saveTechnicianWarehouseParts to directly save the items
     */
    private fun saveTechnicianWarehouseParts(technicianWarehouseParts: MutableList<TechnicianWarehousePart>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(technicianWarehouseParts)
            }
        } catch (e: Exception) {
            Log.d(UpdateManager.TAG, UpdateManager.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    private suspend fun updateAllPartsFromRequest(partList: MutableList<Part>) {
        partList.forEach { it.generateCustomId() }
        deleteParts()
        saveParts(partList)
    }

    private suspend fun saveParts(partList: MutableList<Part>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(partList)
            }
        } catch (e: Exception) {
            Log.d(UpdateManager.TAG, UpdateManager.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    private fun deleteParts() {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(Part::class.java)
            }
        } catch (e: Exception) {
            Log.d(UpdateManager.TAG, UpdateManager.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getNotSentPartsByOrderId(orderId: Int): RealmList<UsedPart> {
        val realm = Realm.getDefaultInstance()
        val parts = RealmList<UsedPart>()
        try {
            val realmParts =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, orderId)
                    .equalTo(UsedPart.SENT, false)
                    .findAll()
            parts.addAll(realmParts)
            return parts
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return parts
        } finally {
            realm.close()
        }
    }

    fun deleteAllPartsByOrderId(orderId: Int, scope: CoroutineScope) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val realm = Realm.getDefaultInstance()
                try {
                    realm.executeTransaction { realm1 ->
                        realm1.where(UsedPart::class.java).equalTo(UsedPart.CALL_ID, orderId)
                            .findAll().deleteAllFromRealm()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                } finally {
                    realm.close()
                }
            }
        }
    }

    /**
     * All pending parts
     * All used parts
     * Only needed parts that comes from the api
     */
    suspend fun getAllPartsByOrderId(orderId: Int): List<UsedPart> {
        val realm = Realm.getDefaultInstance()
        var parts = mutableListOf<UsedPart>()
        try {
            val realmParts =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, orderId)
                    .findAll()
            parts = realm.copyFromRealm(realmParts)
            parts.removeIf { it.isAddedLocally && it.localUsageStatusId == UsedPart.NEEDED_STATUS_CODE }
            return parts
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return parts
        } finally {
            realm.close()
        }
    }


    suspend fun getAllPartsByOrderIdForAssist(
        orderId: Int,
        techWarehouse: Int,
        customerWarehouse: Int
    ): List<UsedPart> {
        val realm = Realm.getDefaultInstance()
        var parts = mutableListOf<UsedPart>()
        try {
            val realmParts =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, orderId)
                    .equalTo(UsedPart.USAGE_STATUS_ID, UsedPart.USED_STATUS_CODE)
                    .or()
                    .equalTo(UsedPart.USAGE_STATUS_ID, UsedPart.USED_STATUS_CODE)
                    .equalTo(UsedPart.ADDED_LOCALLY, true)
                    .findAll()
            parts = realm.copyFromRealm(realmParts)
            val partsFiltered = parts.filter {
                it.isFromTechWarehouse(techWarehouse) || it.isFromTechWarehouse(customerWarehouse)
            }
            parts = partsFiltered.toMutableList()
            return parts
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return parts
        } finally {
            realm.close()
        }
    }

    suspend fun getAllPartsForNeededParts(): List<Part> {
        var resParts = listOf<Part>()
        val realm = Realm.getDefaultInstance()
        try {
            val realmParts = realm.where(Part::class.java)
                .distinct(Part.ITEM_ID)
                .sort(Part.ITEM, Sort.ASCENDING)
                .findAll()
            resParts = realm.copyFromRealm(realmParts)
            return resParts
        } catch (e: Exception) {
            return resParts
        } finally {
            realm.close()
        }
    }

    suspend fun getAllNeededPartsByOrderId(orderId: Int): List<UsedPart> {
        val realm = Realm.getDefaultInstance()
        var parts = mutableListOf<UsedPart>()
        try {
            val realmParts =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, orderId)
                    .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.NEEDED_STATUS_CODE)
                    .findAll()
            parts = realm.copyFromRealm(realmParts)
            return parts
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return parts
        } finally {
            realm.close()
        }
    }

    suspend fun getAllSentNeededPartsByOrderId(orderId: Int): List<UsedPart> {
        val allNeeded = getAllNeededPartsByOrderId(orderId)
        return allNeeded.filter { it.isSent }
    }

    suspend fun getAllLocalNeededPartsByOrderId(orderId: Int): List<UsedPart> {
        val allNeeded = getAllNeededPartsByOrderId(orderId)
        return allNeeded.filter { it.isAddedLocally }
    }

    suspend fun getAllPendingPartsByOrderId(orderId: Int): List<UsedPart> {
        val realm = Realm.getDefaultInstance()
        var parts = mutableListOf<UsedPart>()
        try {
            val realmParts =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CALL_ID, orderId)
                    .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.PENDING_STATUS_CODE)
                    .findAll()
            parts = realm.copyFromRealm(realmParts)
            return parts
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return parts
        } finally {
            realm.close()
        }
    }

    suspend fun getPartByCustomPartId(customPartId: String): UsedPart? {
        val realm = Realm.getDefaultInstance()
        var part: UsedPart? = null
        try {
            val realmPart =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CUSTOM_USED_PART_ID, customPartId)
                    .findFirst()
            realmPart?.let {
                part = realm.copyFromRealm(realmPart)
            }
            return part
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return part
        } finally {
            realm.close()
        }
    }

    suspend fun updatePart(usedPart: UsedPart) {
        val realm = Realm.getDefaultInstance()
        var part: UsedPart? = null
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(usedPart)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun saveNewPart(usedPart: UsedPart) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(usedPart)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    suspend fun deletePart(customPartId: String) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val part = realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.CUSTOM_USED_PART_ID, customPartId)
                    .findFirst()
                part?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun markPartAsUsed(partCustomId: String) {
        val part = getPartByCustomPartId(partCustomId) ?: return
        part.localUsageStatusId = UsedPart.USED_STATUS_CODE
        part.actionType = if (part.isAddedLocally) "insert" else "update"
        part.isDeleteWhenRefreshing = false
        part.isHasBeenChangedLocally = true
        part.deletable = false
        updatePart(part)
    }

    suspend fun markPartAsPending(partCustomId: String) {
        val part = getPartByCustomPartId(partCustomId) ?: return
        part.localUsageStatusId = UsedPart.PENDING_STATUS_CODE
        part.actionType = if (part.isAddedLocally) "insert" else "update"
        part.isDeleteWhenRefreshing = false
        part.isHasBeenChangedLocally = true
        updatePart(part)
    }

    suspend fun deletePendingPart(partCustomId: String) {
        val part = getPartByCustomPartId(partCustomId) ?: return
        if (!part.isSent) {
            deletePart(partCustomId)
            return
        }
        part.deletable = !part.deletable
        part.actionType = if (part.deletable) "delete" else "update"
        updatePart(part)
    }


    suspend fun deleteNeededPart(partCustomId: String) {
        val part = getPartByCustomPartId(partCustomId) ?: return
        if (!part.isSent) {
            deletePart(partCustomId)
            return
        }
        part.deletable = !part.deletable
        part.actionType = if (part.deletable) "delete" else "update"
        updatePart(part)
    }

    suspend fun deleteSentUsedPartLocally(customPartId: String) {
        val part = getPartByCustomPartId(customPartId) ?: return
        part.deletable = !part.deletable
        part.actionType = if (part.deletable) "delete" else "update"
        if (part.isHasBeenChangedLocally) {
            part.localUsageStatusId = UsedPart.PENDING_STATUS_CODE
        }
        updatePart(part)
    }

    suspend fun canPassPartsStepBeforeComplete(orderId: Int): Boolean {
        val canPass = false
        val realm = Realm.getDefaultInstance()
        try {
            val neededParts = getAllSentNeededPartsByOrderId(orderId)
            val pendingParts = getAllPendingPartsByOrderId(orderId)
            val countPending = pendingParts.filter { !it.deletable }.size
            val countNeededParts = neededParts.filter { !it.deletable }.size
            val count = countNeededParts + countPending
            return count == 0

        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return canPass
    }


    fun getStringOfListOfPartsWithChanges(): String {
        var res = ""
        val realm = Realm.getDefaultInstance()
        try {
            val partWithChanges = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .findAll()
            val listOfSCNumberCodes = partWithChanges.map {
                ServiceOrderRepository.getServiceOrderByCallId(it.callId)?.callNumber_Code ?: ""
            }
            val setOfSCNumberCodes = listOfSCNumberCodes.toSet()
            res = setOfSCNumberCodes.joinToString(", ")
            return res
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return ""
        } finally {
            realm.close()
        }
    }

    fun deleteNeededPartsAddedLocallyWithHolId(callId: Int, holdCodeId: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            val parts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, callId)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .equalTo(UsedPart.SENT, false)
                .equalTo(UsedPart.USAGE_STATUS_ID, UsedPart.NEEDED_STATUS_CODE)
                .equalTo(UsedPart.HOLD_CODE_ID, holdCodeId)
                .findAll()
            realm.executeTransaction {
                parts.deleteAllFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun updatePartsDeletableByCallId(callId: Int, isDeletable: Boolean) {
        val realm = Realm.getDefaultInstance()
        try {
            val parts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, callId)
                .equalTo(UsedPart.SENT, true)
                .findAll()
            realm.executeTransaction {
                parts.forEach {
                    it.deletable = isDeletable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getUsedQuantityParts(itemId: Int, warehouseId: Int): Double {
        val quantityUsed = 0.0
        val realm = Realm.getDefaultInstance()
        try {
            val localParts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.ITEM_ID, itemId)
                .equalTo(UsedPart.WAREHOUSE_ID, warehouseId)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .findAll()
            val localPartsQuantity = localParts.map { it.quantity }.sum()
            val sentFromEAParts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.ITEM_ID, itemId)
                .equalTo(UsedPart.WAREHOUSE_ID, warehouseId)
                .equalTo(UsedPart.SENT, true)
                .equalTo(UsedPart.DELETABLE, true)
                .findAll()
            val pendingOrUsedParts =
                sentFromEAParts.filter { it.localUsageStatusId != UsedPart.NEEDED_STATUS_CODE }
            val subtractSentQuantity = pendingOrUsedParts.map { it.quantity }.sum()
            return localPartsQuantity - subtractSentQuantity
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return quantityUsed
        } finally {
            realm.close()
        }

    }

    suspend fun getTechnicianPartById(customId: String): TechnicianWarehousePart? {
        val realm = Realm.getDefaultInstance()
        var part: TechnicianWarehousePart? = null
        try {
            val realmPart =
                realm.where(TechnicianWarehousePart::class.java)
                    .equalTo(TechnicianWarehousePart.COLUMNS.CUSTOM_ID, customId)
                    .findFirst()
            realmPart?.let {
                part = realm.copyFromRealm(realmPart)
            }
            return part
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return part
        } finally {
            realm.close()
        }
    }

    fun getUsedQuantityBins(
        binId: Int,
        partItemId: Int,
        warehouseId: Int,
        serialNumber: String
    ): Double {
        val realm = Realm.getDefaultInstance()
        try {
            val localTechnicianParts =
                realm.where(UsedPart::class.java)
                    .equalTo(UsedPart.ITEM_ID, partItemId)
                    .equalTo(UsedPart.BIN_ID, binId)
                    .equalTo(UsedPart.WAREHOUSE_ID, warehouseId)
                    .equalTo(UsedPart.ADDED_LOCALLY, true)
                    .equalTo(UsedPart.SERIAL_NUMBER, serialNumber)
                    .findAll()
            val localBinsQuantity = localTechnicianParts.map { it.quantity }.sum()

            val sentFromEAParts = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.ITEM_ID, partItemId)
                .equalTo(UsedPart.BIN_ID, binId)
                .equalTo(UsedPart.WAREHOUSE_ID, warehouseId)
                .equalTo(UsedPart.SENT, true)
                .equalTo(UsedPart.SERIAL_NUMBER, serialNumber)
                .equalTo(UsedPart.DELETABLE, true)
                .findAll()

            val pendingOrUsedParts =
                sentFromEAParts.filter { it.localUsageStatusId != UsedPart.NEEDED_STATUS_CODE }
            val subtractSentQuantity = pendingOrUsedParts.map { it.quantity }.sum()

            return localBinsQuantity - subtractSentQuantity
        } catch (e: Exception) {
            return 0.0
        } finally {
            realm.close()
        }
    }

    suspend fun getPartByCustomPartId2(customPartId: String): Part? {
        val realm = Realm.getDefaultInstance()
        var part: Part? = null
        try {
            val realmPart =
                realm.where(Part::class.java)
                    .equalTo(Part.CUSTOM_ID, customPartId)
                    .findFirst()
            realmPart?.let {
                part = realm.copyFromRealm(realmPart)
            }
            return part
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return part
        } finally {
            realm.close()
        }
    }

    fun getUsedPartData(orderId: Int): List<UsedPart>? {
        val fieldNames = arrayOf(UsedPart.ADDED_LOCALLY, UsedPart.WAREHOUSE_NAME)
        val sortArray = arrayOf(Sort.DESCENDING, Sort.DESCENDING)
        val realm = Realm.getDefaultInstance()
        var usedPartData: List<UsedPart>? = null
        try {
            val realmUsedPart = realm.where(UsedPart::class.java)
                .equalTo(UsedPart.CALL_ID, orderId).and()
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.USED_STATUS_CODE)
                .equalTo(UsedPart.DELETABLE, false)
                .sort(fieldNames, sortArray)
                .findAll()
            realmUsedPart?.let {
                usedPartData = realm.copyFromRealm(realmUsedPart)
            }
            return usedPartData
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return usedPartData
        } finally {
            realm.close()
        }
    }

}