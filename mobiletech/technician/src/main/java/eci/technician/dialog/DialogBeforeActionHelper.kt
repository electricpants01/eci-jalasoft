package eci.technician.dialog

import android.util.Log
import androidx.fragment.app.FragmentManager
import eci.technician.R
import eci.technician.dialog.DialogManager.showSimpleAlertDialog
import eci.technician.helpers.AppAuth
import eci.technician.models.order.ServiceCallLabor
import eci.technician.models.order.ServiceOrder
import io.realm.Realm

object DialogBeforeActionHelper {

    fun checkDispatchedCallsBeforeDispatch(fragmentManager: FragmentManager): Boolean {
        val dispatchedCall = hasDispatchedCalls()
        return if (dispatchedCall != null) {
            showSimpleAlertDialog(
                    AppAuth.getInstance().context.resources.getString(R.string.dispatch), AppAuth.getInstance().context.resources.getString(R.string.dispatchWarning, dispatchedCall.callNumber_Code), AppAuth.getInstance().context.resources.getString(R.string.ok),
                    fragmentManager, true) { aBoolean: Boolean? -> }
            true
        } else {
            false
        }
    }

    fun checkDispatchedCallsBeforeClockOut(fragmentManager: FragmentManager): Boolean {
        val dispatchedCall = hasDispatchedCalls()
        return if (dispatchedCall != null) {
            showSimpleAlertDialog(
                    AppAuth.getInstance().context.resources.getString(R.string.warning), AppAuth.getInstance().context.resources.getString(R.string.clockoutWarning, dispatchedCall.callNumber_Code), AppAuth.getInstance().context.resources.getString(R.string.ok),
                    fragmentManager, true) { aBoolean: Boolean? -> }
            true
        } else {
            false
        }
    }

    private fun hasDispatchedCalls(): ServiceOrder? {
        val realm = Realm.getDefaultInstance()
        try {
            val dispatchedCalls = realm.where(ServiceOrder::class.java).findAll()
            var dispatchedCallsCount: Long = 0
            var dispatchedCall: ServiceOrder? = null
            for (serviceOrder in dispatchedCalls) {
                if (serviceOrder.isAssist()) {
                    val assistCall = realm.where(ServiceCallLabor::class.java)
                            .equalTo("callId", serviceOrder.callNumber_ID)
                            .and()
                            .equalTo(ServiceCallLabor.TECHNICIAN_ID, AppAuth.getInstance().technicianUser.technicianNumber)
                            .findFirst()
                    assistCall?.let {
                        if (assistCall.dispatchTime != null && assistCall.departureTime == null) {
                            dispatchedCallsCount = dispatchedCallsCount + 1
                            dispatchedCall = serviceOrder
                        }
                    }
                } else {
                    if (serviceOrder.statusCode_Code == "D  " && !serviceOrder.completedCall) {
                        dispatchedCallsCount = dispatchedCallsCount + 1
                        dispatchedCall = serviceOrder
                    }
                }
            }
            return dispatchedCall
        } catch (e: Exception) {
            Log.e("hasDispatchedCalls", "Exception", e)
            return null
        } finally {
            realm.close()
        }
    }
}