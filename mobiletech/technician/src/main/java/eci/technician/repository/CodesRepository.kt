package eci.technician.repository

import android.util.Log
import com.google.gson.reflect.TypeToken
import eci.technician.activities.problemCodes.ProblemCodesSearchFragment
import eci.technician.activities.repairCode.RepairCodeSearchFragment
import eci.technician.helpers.api.retroapi.*
import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.models.ProcessingResult
import eci.technician.models.create_call.CallType
import eci.technician.models.data.UsedProblemCode
import eci.technician.models.data.UsedRepairCode
import eci.technician.models.order.*
import eci.technician.tools.Settings
import io.realm.Realm
import io.realm.kotlin.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

object CodesRepository {
    const val TAG = "ServiceCallRepository"
    const val EXCEPTION = "Exception"


    @Synchronized
    suspend fun getAllCallTypes(): Flow<Resource<List<CallType>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getAllCallTypes2() }
            if (resource is Resource.Success) {
                resource.data?.let { callTypeList -> saveCallTypeList(callTypeList) }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    private fun saveCallTypeList(callTypeList: List<CallType>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction { realm.insertOrUpdate(callTypeList) }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    @Synchronized
    suspend fun getAllIncompleteCodes(): Flow<Resource<List<IncompleteCode>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getAllIncompleteCodes2() }
            if (resource is Resource.Success) {
                resource.data?.let { incompleteCodeList -> saveIncompleteCodeList(incompleteCodeList) }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    private fun saveIncompleteCodeList(incompleteCodeList: List<IncompleteCode>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val resultsIncomplete = realm.where(IncompleteCode::class.java)
                    .equalTo(IncompleteCode.COLUMNS.IS_CHECKED, true).findAll()
                incompleteCodeList.forEach { incomplete ->
                    resultsIncomplete.forEach { incompleteFirst ->
                        if (incomplete.incompleteCodeId == incompleteFirst.incompleteCodeId) {
                            incomplete.isChecked = true
                        }
                    }
                }
                realm.insertOrUpdate(incompleteCodeList)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    @Synchronized
    suspend fun getAllActivityCallTypes(): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getAllActivityCallTypes2() }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    response.result?.let { result ->
                        val list = createActivityCallTypeList(result)
                        saveActivityCallTypeList(list)
                    }
                    emit(resource)
                }
            } else {
                emit(resource)
            }

        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
    }

    private fun createActivityCallTypeList(responseBody: String): List<ActivityCode> {
        var list = listOf<ActivityCode>()
        return try {
            val listOfActivityCallTypes = object : TypeToken<MutableList<ActivityCode>>() {}.type
            list = Settings.createGson().fromJson(responseBody, listOfActivityCallTypes)
            list
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            list
        }
    }

    private fun saveActivityCallTypeList(list: List<ActivityCode>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(ActivityCode::class.java)
                realm.insertOrUpdate(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    @Synchronized
    suspend fun getServiceCallProblemCodes(): Flow<Resource<List<ProblemCode>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getServiceCalProblemCodes2() }
            if (resource is Resource.Success) {
                resource.data?.let { problemCodeList ->
                    updateServiceCallProblemCodes(problemCodeList)
                }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun updateServiceCallProblemCodes(problemCodeList: List<ProblemCode>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(ProblemCode::class.java)
                realm.insertOrUpdate(problemCodeList)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    @Synchronized
    suspend fun getServiceCallRepairCodes(): Flow<Resource<List<RepairCode>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getServiceCallRepairCodes2() }
            if (resource is Resource.Success) {
                resource.data?.let { repairCodeList ->
                    updateRepairCodes(repairCodeList)
                }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun updateRepairCodes(repairCodeList: List<RepairCode>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(RepairCode::class.java)
                realm.insertOrUpdate(repairCodeList)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    @Synchronized
    suspend fun getHoldCodes(): Flow<Resource<List<HoldCode>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getHoldCodes2() }
            if (resource is Resource.Success) {
                resource.data?.let { holdCodes ->
                    updateHoldCodes(holdCodes)
                }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun updateHoldCodes(holdCodes: List<HoldCode>) {
        val holdCodesFromApi = mutableListOf<Int>()

        val realm = Realm.getDefaultInstance()
        try {
            for (holdCode in holdCodes) {
                realm.executeTransaction {
                    holdCodesFromApi.add(holdCode.onHoldCodeId)
                    it.insertOrUpdate(holdCode)
                }
            }
            val holdCodesToDelete = realm.where(HoldCode::class.java)
                .not()
                .`in`(HoldCode.ON_HOLD_CODE_ID, holdCodesFromApi.toTypedArray())
                .findAll()
            holdCodesToDelete.deleteAllFromRealm()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    @Synchronized
    suspend fun getCancelCodes(): Flow<Resource<List<CancelCode>>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.getCancelCodes2() }
            if (resource is Resource.Success) {
                resource.data?.let { cancelCodeList ->
                    updateCancelCodes(cancelCodeList)
                }
                emit(resource)
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun updateCancelCodes(cancelCodeList: List<CancelCode>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(CancelCode::class.java)
                realm.insertOrUpdate(cancelCodeList)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getAllProblemCodes(): Flow<List<ProblemCode>> {
        val realm = Realm.getDefaultInstance()
        return try {
            realm.where(ProblemCode::class.java)
                .sort(ProblemCode.PROBLEM_CODE_NAME_QUERY).findAll().toFlow()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            flow {  }
        } finally {
            realm.close()
        }
    }

    fun isProblemCodeRepeated(problemCodeName: String,orderId: Int): Boolean{
        val isRepeated = false
        val realm = Realm.getDefaultInstance()
        try {
            val items = realm.where(UsedProblemCode::class.java).equalTo(UsedProblemCode.CALL_ID, orderId).findAll()
            items.forEach {
                if (problemCodeName == it.problemCodeName) {
                    return true
                }
            }
            return isRepeated
        }catch (e:Exception){
            Log.e(TAG, EXCEPTION, e)
            return false
        }finally {
            realm.close()
        }
    }

    fun saveProblemCodeToDB(problemCodeId: Int, problemCodeName: String, problemCodeDescription: String,orderId: Int, onCompleteSave: () -> Unit) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val problemCode = realm.createObject(UsedProblemCode::class.java)
                problemCode.callId = orderId
                problemCode.problemCodeId = problemCodeId
                problemCode.problemCodeName = problemCodeName
                problemCode.description = problemCodeDescription
            }
            onCompleteSave()
        }catch (e:Exception){
            Log.e(TAG, EXCEPTION, e)
        }finally {
            realm.close()
        }
    }

    fun getAllRepairCodes(): Flow<List<RepairCode>> {
        val realm = Realm.getDefaultInstance()
        return try {
            realm.where(RepairCode::class.java)
                .sort(RepairCode.REPAIR_CODE_NAME_QUERY).findAll().toFlow()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            flow {  }
        } finally {
            realm.close()
        }
    }

    fun isRepairCodeRepeated(orderId: Int, repairCodeName: String): Boolean{
        var isRepeated = false
        val realm = Realm.getDefaultInstance()
        try {
            val items = realm
                .where(UsedRepairCode::class.java)
                .equalTo(UsedRepairCode.CALL_ID, orderId)
                .findAll()
            items.forEach {
                if (repairCodeName == it.repairCodeName) {
                    return true
                }
            }
            return isRepeated
        }catch (e: java.lang.Exception){
            Log.e(TAG, EXCEPTION, e)
            return false
        }finally {
            realm.close()
        }
    }

    fun saveRepairCodeToDB(orderId: Int,
                           repairCodeId: Int,
                           repairCodeName: String,
                           description: String,
                           onCompleteSave: () -> Unit){
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val repairCode = realm.createObject(UsedRepairCode::class.java)
                repairCode.callId = orderId
                repairCode.repairCodeId = repairCodeId
                repairCode.repairCodeName = repairCodeName
                repairCode.description = description
            }
            onCompleteSave()
        }catch (e:Exception){
            Log.e(TAG, EXCEPTION, e)
        }finally {
            realm.close()
        }
    }
}