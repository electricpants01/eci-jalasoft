package eci.technician.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import eci.technician.MainApplication
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.DeviceTokenPostModel
import eci.technician.models.LoginPostModel
import eci.technician.models.ProcessingResult
import eci.technician.models.gps.GPSLocation
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.repository.LastUpdateRepository
import eci.technician.repository.LoginRepository
import eci.technician.repository.TechnicianTimeRepository
import eci.technician.viewmodels.ViewModelUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {


    private var _loading: MutableLiveData<Boolean> = MutableLiveData()
    val loading: LiveData<Boolean> = _loading

    private var _isLoginSuccess: MutableLiveData<ViewModelUtils.Event<Boolean>> = MutableLiveData()
    val isLoginSuccess: LiveData<ViewModelUtils.Event<Boolean>> = _isLoginSuccess

    private var _isClockOutSuccess: MutableLiveData<ViewModelUtils.Event<Boolean>> =
        MutableLiveData()
    val isClockOutSuccess: LiveData<ViewModelUtils.Event<Boolean>> = _isClockOutSuccess

    private var _error: MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> =
        MutableLiveData()
    val error: LiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>> = _error

    private var _handledLicenseError: MutableLiveData<Pair<ErrorType, String?>> = MutableLiveData()
    val handledLicenseError: LiveData<Pair<ErrorType, String?>> = _handledLicenseError

    private var _handledLoginSystemError: MutableLiveData<Pair<ErrorType, String?>> =
        MutableLiveData()
    val handledLoginSystemError: LiveData<Pair<ErrorType, String?>> = _handledLoginSystemError

    private var _successLicense: MutableLiveData<ViewModelUtils.Event<Boolean>> = MutableLiveData()
    val successLicense: LiveData<ViewModelUtils.Event<Boolean>> = _successLicense


    fun performLogin(loginPostModel: LoginPostModel) = viewModelScope.launch {
        LoginRepository.loginMobileTech(loginPostModel).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    updateToken(resource)
                    delay(1000)
                    _loading.value = false
                    _isLoginSuccess.value = ViewModelUtils.Event(true)
                }
                is Resource.Error -> {
                    _loading.value = false
                    val pairError = resource.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _error.value = ViewModelUtils.Event(pairError)
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }

    private fun updateToken(resource: Resource.Success<ProcessingResult>) {
        val token = resource.data?.result ?: ""
        AppAuth.getInstance().token = token
    }

    fun createLoginPostModel(account: String, email: String, password: String): LoginPostModel {
        return LoginPostModel(account, email, password)
    }

    fun clockOutTechnician(odometer: Int?) = viewModelScope.launch {
        val changeStatusModel: ChangeStatusModel = if (odometer != null) {
            ChangeStatusModel(odometer.toDouble())
        } else {
            ChangeStatusModel()
        }

        if (MainApplication.lastLocation != null) {
            changeStatusModel.gpsLocation =
                GPSLocation.fromAndroidLocation(MainApplication.lastLocation)
        }

        TechnicianTimeRepository.clockOutUser(changeStatusModel).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    _isClockOutSuccess.value = ViewModelUtils.Event(true)
                    TechnicianTimeRepository.fetchClockOutActions {}
                }
                is Resource.Error -> {
                    val pairError = resource.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _error.value = ViewModelUtils.Event(pairError)
                }
                is Resource.Loading -> {
                    // do nothing
                }
            }
        }
    }

    fun deleteDataOnLogout() = viewModelScope.launch {
        LastUpdateRepository.deleteLastUpdateCache()
    }


    fun loginLicense(account: String) = viewModelScope.launch {
        LoginRepository.loginLicense(account).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _successLicense.value = ViewModelUtils.Event(true)
                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                    if (pair.first == ErrorType.NOT_SUCCESSFUL && value.message?.contains("404") == true) {
                        _handledLicenseError.value = pair
                    } else {
                        _error.value = ViewModelUtils.Event(pair)
                    }

                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }


    fun performLoginToSystem(account: String, email: String, password: String) =
        viewModelScope.launch {
            val loginPostModel = LoginPostModel(
                eAutomateId = account,
                email = email,
                password = password
            )
            LoginRepository.loginMobileTech(loginPostModel).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        updateToken(resource)
                        delay(100)

                        updateUserInfo()
                    }
                    is Resource.Error -> {
                        _loading.value = false
                        val pairError = resource.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                        if (pairError.first == ErrorType.BACKEND_ERROR) {
                            _handledLoginSystemError.value = pairError
                        } else {
                            _error.value = ViewModelUtils.Event(pairError)
                        }
                    }
                    is Resource.Loading -> {
                        _loading.value = true
                    }
                }
            }
        }

    private fun updateUserInfo() = viewModelScope.launch {
        LoginRepository.getUserInfo().collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    getDeviceToken { token ->
                        sendDeviceToken(token)
                    }
                    _loading.value = false
                    _isLoginSuccess.value = ViewModelUtils.Event(true)
                }
                is Resource.Error -> {
                    _loading.value = false
                    val pairError = resource.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "")
                    _error.value = ViewModelUtils.Event(pairError)
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }

    private fun sendDeviceToken(token: String) = viewModelScope.launch {
        val deviceTokenPostModel = DeviceTokenPostModel(
            userId = AppAuth.getInstance().technicianUser.id ?: "",
            deviceToken = token,
            deviceType = "1"
        )
        LoginRepository.updateDeviceToken(deviceTokenPostModel).collect { }
    }

    private fun getDeviceToken(onSuccess: (token: String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task: Task<String?> ->
                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }
                task.result?.let { token ->
                    onSuccess.invoke(token)
                }
            }
    }

    fun compatibilityLoader(isLoading: Boolean) {
        _loading.value = isLoading
    }


}