package eci.technician.repository

import eci.technician.helpers.api.retroapi.ApiUtils.safeCall
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.order.StatusChangeModel
import eci.technician.models.time_cards.ChangeStatusModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.util.*

object OfflineRequestRepository {

    suspend fun dispatchArriveUnDispatchAction(
        action: String,
        map: HashMap<String, Any>
    ): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.requestCallAction2(action, map) }
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
        }.catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestClockActions(
        action: String,
        changeStatusModel: ChangeStatusModel
    ): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.requestClockStuff2(action, changeStatusModel) }
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
        }.catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestOnHoldAction(changeStatusModel: StatusChangeModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.requestOnHoldAction2(changeStatusModel) }
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
        }.catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestUpdateEquipmentDetails(
        action: String,
        map: HashMap<String, Any>
    ): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.updateEquipmentDetails2(action, map) }
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
        }.catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestScheduleCall(statusChangeModel: StatusChangeModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.requestScheduleCall2(statusChangeModel) }
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
        }.catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestHoldRelease(statusChangeModel: StatusChangeModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.requestHoldRelease2(statusChangeModel) }
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
        }.catch { emit(Resource.getGenericError()) }
    }

    suspend fun requestCompleteCall(
        action: String,
        statusChangeModel: StatusChangeModel
    ): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = safeCall { api.completeCall2(action, statusChangeModel) }
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
        }.catch { emit(Resource.getGenericError()) }
    }


}