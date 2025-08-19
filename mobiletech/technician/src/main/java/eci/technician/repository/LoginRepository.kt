package eci.technician.repository

import android.util.Log
import eci.technician.BuildConfig
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ApiUtils
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.*
import eci.technician.tools.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

object LoginRepository {

    const val TAG = "LoginRepository"
    const val EXCEPTION = "Exception"

    suspend fun loginMobileTech(
        loginPostModel: LoginPostModel
    ): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.loginMobileTech(loginPostModel) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    RetrofitApiHelper.setApiToNull()
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    val token = resource.data.result ?: ""
                    AppAuth.getInstance().token = token
                    emit(resource)
                }
            } else {
                RetrofitApiHelper.setApiToNull()
                emit(resource)
            }
        }.catch { emit(Resource.getGenericError()) }.flowOn(Dispatchers.IO)
    }

    suspend fun loginLicense(account: String): Flow<Resource<LicenseLoginResponse>> {
        return flow {
            val baseURL = BuildConfig.LicenseServer
            val userName = BuildConfig.LicenseServerUsername
            val password = BuildConfig.LicenseServerPassword
            val api = RetrofitApiHelper.getLoginApi(baseURL, userName, password) ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.licenseLogin(account) }
            if (resource is Resource.Success) {
                val data = resource.data
                AppAuth.getInstance().saveLicenseLoginData(
                    data?._links?.host?.href ?: "",
                    data?._links?.chat?.href ?: "",
                    data?._links?.chat?.enabled.toBoolean(),
                    data?._links?.gps?.href ?: "",
                    data?._links?.gps?.gpsPrefix ?: ""
                )

                emit(resource)
            } else {
                RetrofitApiHelper.setApiToNull()
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }


    suspend fun updateDeviceToken(deviceTokenPostModel: DeviceTokenPostModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.sendDeviceToken(deviceTokenPostModel) }
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

    fun getUserInfoAux() {
        CoroutineScope(Dispatchers.IO).launch {
            getUserInfo().collect { }
        }
    }

    suspend fun getUserInfo(): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.updateUser2() }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    AppAuth.getInstance().token = null
                    AppAuth.getInstance().technicianUser = null
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    val data = response.result ?: ""
                    val technician = createTechnicianFromResponse(data)
                    AppAuth.getInstance().technicianUser = technician
                    AppAuth.getInstance().sendGpsStatus()
                    emit(resource)
                }
            } else {
                val error = resource.error
                if (error?.first == ErrorType.NOT_SUCCESSFUL && error.second?.contains("401") == true) {
                    AppAuth.getInstance().token = null
                    AppAuth.getInstance().technicianUser = null
                }
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericError()) }

    }


    private fun createTechnicianFromResponse(responseBody: String): TechnicianUser? {
        return try {
            Settings.createGson().fromJson(
                responseBody,
                TechnicianUser::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            null
        }
    }

}