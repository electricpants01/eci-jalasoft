package eci.technician.helpers.versionManager

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import eci.technician.BuildConfig
import eci.technician.R
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.eci_host.EciHostVersion
import eci.technician.repository.HostVersionRepository
import eci.technician.repository.TransfersRepository
import eci.technician.viewmodels.ViewModelUtils
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class CompatibilityManager(private val context: Context) {

    companion object {
        const val COMPATIBILITY_OK: String = "Ok"
        const val ECi_HOST_BUILD_COMPATIBLE: String = BuildConfig.EciHostVersion
        lateinit var MESSAGE_OLD_MOBILE: String
        lateinit var MESSAGE_OLD_HOST: String
        lateinit var MESSAGE_UNKNOWN_HOST: String
    }

    init {
        MESSAGE_OLD_MOBILE = context.resources.getString(R.string.old_version_mobile)
        MESSAGE_OLD_HOST = context.resources.getString(R.string.old_version_host)
        MESSAGE_UNKNOWN_HOST = context.resources.getString(R.string.unknown_host_version)
    }

    private fun isCompatible(eciHostVersionActual: String): CompatibilityStatus {
        return when (CompareVersion.compareVersions(
            eciHostVersionActual,
            ECi_HOST_BUILD_COMPATIBLE
        )) {
            0 -> CompatibilityStatus.IS_COMPATIBLE
            1 -> CompatibilityStatus.OLD_MOBILE_VERSION
            else -> CompatibilityStatus.OLD_HOST_VERSION
        }
    }

    private fun getErrorMessage(eciHostVersion: String?): String {
        if (eciHostVersion == RetrofitRepository.NOT_FOUND_CODE.toString() || eciHostVersion == null)
            return MESSAGE_UNKNOWN_HOST
        return when (isCompatible(eciHostVersion)) {
            CompatibilityStatus.IS_COMPATIBLE -> COMPATIBILITY_OK
            CompatibilityStatus.OLD_MOBILE_VERSION -> MESSAGE_OLD_MOBILE
            else -> MESSAGE_OLD_HOST
        }
    }

    @Synchronized
    fun checkCompatibility(
        lifecycleScope: Lifecycle,
        onSuccess: (response: String) -> Unit,
        onError: (s1: String, s2: String) -> Unit,
        onLoading: () -> Unit
    ) =
        lifecycleScope.coroutineScope.launch {
            HostVersionRepository.getEciHostVersion().collect { value ->
                when (value) {
                    is Resource.Success -> {
                        val eciHostVersion = HostVersionRepository.createEciHostVersionFromJSON(
                            value.data?.result ?: ""
                        ).eciHostVersion
                        onSuccess(getErrorMessage(eciHostVersion))
                    }
                    is Resource.Error -> {
                        val pairError = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        when (pairError.first) {
                            ErrorType.SOCKET_TIMEOUT_EXCEPTION,
                            ErrorType.CONNECTION_EXCEPTION,
                            ErrorType.IO_EXCEPTION,
                            ErrorType.NOT_SUCCESSFUL,
                            ErrorType.BACKEND_ERROR,
                            ErrorType.HTTP_EXCEPTION,
                            ErrorType.SOMETHING_WENT_WRONG -> {
                                if (pairError.second?.contains("404") == true)
                                    onSuccess(MESSAGE_UNKNOWN_HOST)
                                else
                                    onError(
                                        context.getString(R.string.somethingWentWrong),
                                        pairError.second ?: ""
                                    )
                            }
                        }
                    }
                    is Resource.Loading -> {
                        onLoading()
                    }
                }
            }
        }


}