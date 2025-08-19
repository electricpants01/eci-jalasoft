package eci.technician.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.activities.transfers.TransferFilterHelper
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.repository.TransfersRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var _transferVisibility: MutableLiveData<Boolean> = MutableLiveData()
    val transferVisibility: LiveData<Boolean> = _transferVisibility

    fun checkTransferMenuVisibility() = viewModelScope.launch {
        val transferPermission = AppAuth.getInstance()?.technicianUser?.isAllowWarehousesTransfers ?: false
        if (transferPermission) {
            val withoutDefaultBin = -1
            TransfersRepository.getWarehouses(withoutDefaultBin).collect { value ->
                when (value) {
                    is Resource.Success -> {
                        val companyList =
                            TransferFilterHelper.retrieveCompanyWarehouseList(value.data)
                        _transferVisibility.value = companyList.size >= 2
                    }
                    is Resource.Error -> {
                        _transferVisibility.value = false
                    }
                    is Resource.Loading -> {
                        //TODO not showing a spinner
                    }
                }
            }
        } else {
            _transferVisibility.value = false
        }
    }
}