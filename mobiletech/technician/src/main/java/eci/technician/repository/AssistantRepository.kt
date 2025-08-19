package eci.technician.repository

import android.util.Log
import eci.technician.helpers.api.retroapi.ApiUtils
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.TechnicianItem
import eci.technician.tools.Settings
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object AssistantRepository {
    const val TAG = "AssistantRepository"
    const val EXCEPTION = "Exception"


    suspend fun getAllTechnicians(): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getAllTechnicians() }
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


    suspend fun getAllAssistanceList(): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getAllAssistanceList() }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    response.result?.let { result ->
                        val list = createTechniciansListFromResponse(result)
                        saveAssistanceList(list)
                    }
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }
    }


    fun createTechniciansListFromResponse(resultBody: String): MutableList<TechnicianItem> {
        var list = mutableListOf<TechnicianItem>()
        try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<TechnicianItem>::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return list
        }
        return list
    }

    private fun saveAssistanceList(list: List<TechnicianItem>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(TechnicianItem::class.java)
                realm.insertOrUpdate(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

}