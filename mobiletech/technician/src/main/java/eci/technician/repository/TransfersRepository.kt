package eci.technician.repository

import android.util.Log
import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.transfers.Warehouse
import eci.technician.tools.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object TransfersRepository {
    const val TAG = "TransfersRepository"
    const val EXCEPTION = "Exception"

    @Synchronized
    suspend fun getWarehouses(partId: Int): Flow<Resource<ProcessingResult>> {
        return flow<Resource<ProcessingResult>> {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall {
                api.getAllWarehouses2(partId)
            }
            if (resource is Resource.Success) {
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
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO)
    }
    fun createWarehouseListFromJSON(resultBody: String): List<Warehouse> {
        var list = listOf<Warehouse>()
        try {
            list = listOf(
                *Settings.createGson()
                    .fromJson(resultBody, Array<Warehouse>::class.java)
            )
        } catch (e: Exception) {
            Log.e(PartsRepository.TAG, PartsRepository.EXCEPTION, e)
            return list
        }
        return list
    }

}