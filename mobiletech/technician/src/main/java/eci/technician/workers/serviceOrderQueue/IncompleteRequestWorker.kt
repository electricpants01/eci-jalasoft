package eci.technician.workers.serviceOrderQueue

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.order.IncompleteRequests
import eci.technician.tools.Constants
import eci.technician.workers.serviceOrderQueue.ServiceOrderOfflineUtils.retrieveUserData
import io.realm.Realm
import io.realm.Sort
import kotlinx.coroutines.flow.collect

class IncompleteRequestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val realm = Realm.getDefaultInstance()
        realm.refresh()
        val incompleteRequests = realm
            .where(IncompleteRequests::class.java)
            .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value)
            .or().equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value)
            .or().equalTo(
                IncompleteRequests.STATUS,
                Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
            )
            .sort(IncompleteRequests.DATE_ADDED, Sort.ASCENDING)
            .findAll()

        if (incompleteRequests.isEmpty() && AppAuth.getInstance().offlineCounter > 1) {
            AppAuth.getInstance().offlineCounter = 0
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .getTechnicianActiveServiceCallsFlow().collect {  }
            retrieveUserData()
            return Result.success()
        } else {
            if (incompleteRequests.size > 1) {
                AppAuth.getInstance().offlineCounter += 1
            }
            if (incompleteRequests.isNullOrEmpty()) return Result.success()
            val firstIncomplete = incompleteRequests.first()
            if (firstIncomplete != null) {
                when (firstIncomplete.requestCategory) {

                    Constants.REQUEST_TYPE.SERVICE_CALLS.value -> {
                        OfflineManagerRefactor.performServiceCallMainActions(
                            firstIncomplete,
                            applicationContext
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.ACTIONS.value -> {
                        OfflineManagerRefactor.performClockActionsOffline(
                            firstIncomplete,
                            applicationContext
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.ON_HOLD_CALLS.value -> {
                        OfflineManagerRefactor.performOnHoldOffline(
                            firstIncomplete,
                            applicationContext
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.UPDATE_DETAILS.value -> {
                        OfflineManagerRefactor.performUpdateDetailsOffline(
                            firstIncomplete,
                            applicationContext
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.SCHEDULE_CALL.value -> {
                        OfflineManagerRefactor.performScheduleCallOffline(
                            firstIncomplete,
                            applicationContext
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.RELEASE_CALL.value -> {
                        OfflineManagerRefactor.performReleaseCallOffline(
                            firstIncomplete,
                            applicationContext
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.COMPLETE_CALL.value -> {
                        OfflineManagerRefactor.performDepartCallOffline(
                            firstIncomplete,
                            applicationContext,
                            false
                        ) { Result.failure() }
                    }
                    Constants.REQUEST_TYPE.INCOMPLETE_CALL.value -> {
                        OfflineManagerRefactor.performDepartCallOffline(
                            firstIncomplete,
                            applicationContext,
                            true
                        ) { Result.failure() }
                    }
                    else -> {
                        return Result.failure()
                    }
                }
            } else {
                return Result.success()
            }
            return Result.failure()
        }
    }
}