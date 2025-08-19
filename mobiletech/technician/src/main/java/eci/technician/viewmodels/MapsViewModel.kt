package eci.technician.viewmodels

import androidx.lifecycle.ViewModel
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.RealmLiveData

class MapsViewModel : ViewModel() {

    fun getServiceOrder(): RealmLiveData<ServiceOrder> {
        return DatabaseRepository.getInstance().orderedServiceOrderWithDispatchFirstSync
    }
    fun getGroupServiceOrder(): RealmLiveData<GroupCallServiceOrder>{
        return DatabaseRepository.getInstance().groupCalls
    }

}