package eci.technician.repository

import android.util.Log
import com.google.gson.reflect.TypeToken
import eci.technician.helpers.api.retroapi.*
import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.models.ProcessingResult
import eci.technician.models.eci_host.EciHostVersion
import eci.technician.tools.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object HostVersionRepository {

    const val TAG = "HostVersionRepository"
    const val EXCEPTION = "Exception"

    @Synchronized
    suspend fun getEciHostVersion(): Flow<Resource<ProcessingResult>> {
        return flow<Resource<ProcessingResult>> {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall {
                api.getEciHostVersion()
            }
            if(resource is Resource.Success) {
                val response = resource.data ?: return@flow
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
                } else {
                    emit(Resource.Success(response))
                }
            } else{
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * @param resultBody is the jsonString that comes from the ProcessingResult class,
     * and contains the version
     */
    fun createEciHostVersionFromJSON(resultBody: String): EciHostVersion {
        var eciHostVersion = EciHostVersion()
        val hostVersionType =
            object : TypeToken<EciHostVersion>() {}.type
        try {
            val hostVersion: EciHostVersion =
                Settings.createGson()
                    .fromJson(resultBody, hostVersionType)
            eciHostVersion = hostVersion
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return eciHostVersion
        }
        return eciHostVersion
    }
}