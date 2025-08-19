package eci.technician.helpers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.activities.SyncActivity
import eci.technician.dialog.DialogManager
import eci.technician.helpers.DialogHelperManager.displayOkMessage
import eci.technician.helpers.api.ApiHelperBuilder
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.versionManager.CompatibilityManager
import eci.technician.helpers.versionManager.MessageDisplayer.displayMessage
import eci.technician.models.ProcessingResult
import eci.technician.models.gps.GPSLocation
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.TechnicianTimeRepository
import eci.technician.service.TechnicianService
import eci.technician.tools.Constants

object ClockOutHelper {

    interface ActionListener {
        fun onActionSuccess()
        fun onActionFailed(errorMessage: String)
    }

    // Try the clock out action
    fun clockOut(odometerTextValue: String): Boolean {
        val odometerValue: Int
        try {
            odometerValue = Integer.parseInt(odometerTextValue)
            // Validation odometer value shouldn't less than dispatch (last odometer value)
            if (odometerValue < AppAuth.getInstance().lastOdometer) {
                return false
            }
            AppAuth.getInstance().lastOdometer = odometerValue
            return true
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return false
    }

    fun performRequestClockOutBeforeLogOut(odometer: Int?, listener: ActionListener) {

        val changeStatusModel: ChangeStatusModel
        if (odometer != null) {
            changeStatusModel = ChangeStatusModel(odometer.toDouble())
        } else {
            changeStatusModel = ChangeStatusModel()
        }

        if (MainApplication.lastLocation != null) {
            changeStatusModel.gpsLocation = GPSLocation.fromAndroidLocation(MainApplication.lastLocation)
        }

        val apiHelper = ApiHelperBuilder(ProcessingResult::class.java)
                .addPath("Technician")
                .addPath(Constants.STATUS_SIGNED_OUT)
                .setMethodPost(changeStatusModel)
                .setAuthorized(true)
                .build()

        apiHelper.runAsync { success, result, errorCode, errorMessage ->
            if (success) {
                if (!result.isHasError) {
                    AppAuth.getInstance().changeUserStatus(Constants.STATUS_SIGNED_OUT)
                    listener.onActionSuccess()
                } else {
                    listener.onActionFailed(result.formattedErrors)
                }
            } else {
                listener.onActionFailed(errorMessage)
            }
        }

        changeStatusModel.actionType = Constants.STATUS_SIGNED_OUT
    }

    fun unSyncDataClockOutValidation(onFailedValidation: () -> Unit) {
        val incompleteRequestList = DatabaseRepository.getInstance().incompleteRequestList
        val attachmentIncompleteRequestList =
            DatabaseRepository.getInstance().attachmentIncompleteRequestList

        if (incompleteRequestList.isNotEmpty() || attachmentIncompleteRequestList.isNotEmpty()) {
            onFailedValidation.invoke()
            return
        } else return
    }

    fun performClockIn(
        title: String,
        supportFragmentManager: FragmentManager,
        lifecycleOwner: LifecycleOwner,
        baseContext: Context,
        onSuccess: () -> Unit,
        onError: (title: String, message: String, pair:Pair<ErrorType, String?>?) -> Unit,
        onDisconnected: () -> Unit,
        onLoading: () -> Unit,
        onCancel: () -> Unit
    ) {

        if (AppAuth.getInstance().isConnected) {
            val compatibilityManager = CompatibilityManager(baseContext)
            compatibilityManager.checkCompatibility(
                lifecycleScope = lifecycleOwner.lifecycle,
                onSuccess = { compatibilityMessage: String? ->
                    run {
                        if (compatibilityMessage != CompatibilityManager.COMPATIBILITY_OK) {
                            if (compatibilityMessage == CompatibilityManager.MESSAGE_OLD_MOBILE)
                                displayMessage(
                                    compatibilityMessage,
                                    baseContext
                                ) {
                                    performClockInRequest(
                                        title, supportFragmentManager, lifecycleOwner,
                                        baseContext, onSuccess, onError, onDisconnected, onCancel
                                    )
                                }
                            if (compatibilityMessage == CompatibilityManager.MESSAGE_OLD_HOST ||
                                compatibilityMessage == CompatibilityManager.MESSAGE_UNKNOWN_HOST
                            ) displayOkMessage(
                                baseContext.getString(R.string.warning),
                                compatibilityMessage,
                                baseContext.getString(R.string.ok),
                                baseContext
                            ) {
                                performClockInRequest(
                                    title, supportFragmentManager, lifecycleOwner,
                                    baseContext, onSuccess, onError, onDisconnected, onCancel
                                )
                            }
                        } else {
                            performClockInRequest(
                                title, supportFragmentManager, lifecycleOwner,
                                baseContext, onSuccess, onError, onDisconnected, onCancel
                            )
                        }
                    }
                },
                onError = { title: String, message: String ->
                    onError.invoke(title, message, null)
                },
                onLoading = {
                    onLoading.invoke()
                })
        } else {
            onDisconnected.invoke()
        }
    }

    private fun performClockInRequest(
        title: String,
        supportFragmentManager: FragmentManager,
        lifecycleOwner: LifecycleOwner,
        baseContext: Context,
        onSuccess: () -> Unit,
        onError: (title: String, message: String, pair:Pair<ErrorType, String?>?) -> Unit,
        onDisconnected: () -> Unit,
        onCancel: () -> Unit
    ) {
        DialogManager.showClockInDialog(title, supportFragmentManager, { odometerValue: Int ->

            val changeStatusModel = ChangeStatusModel(odometerValue)
            if (MainApplication.lastLocation != null) {
                changeStatusModel.gpsLocation =
                    GPSLocation.fromAndroidLocation(MainApplication.lastLocation)
            }
            TechnicianTimeHelper(baseContext).clockIn(changeStatusModel, lifecycleOwner.lifecycle, {
                AppAuth.getInstance().lastOdometer = odometerValue
                AppAuth.getInstance().changeUserStatus(Constants.STATUS_SIGNED_IN)
                if (!ServiceTools.isServiceRunning(
                        TechnicianService::class.java.name,
                        baseContext
                    )
                ) {
                    baseContext.startService(Intent(baseContext, TechnicianService::class.java))
                }
                onSuccess.invoke()
            }, onError)
        }, onCancel)
    }

    fun showFailedSyncDialogue(context: Context) {
        val builder = context.let { AlertDialog.Builder(it) }
        builder.setTitle(context.getString(R.string.unsynced_data))
            ?.setMessage(context.getString(R.string.unsynced_data_text))?.setCancelable(true)
            ?.setNegativeButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
            ?.setPositiveButton(R.string.show_unsynced_data) { _: DialogInterface?, _: Int ->
                val aboutIntent = Intent(context, SyncActivity::class.java)
                context.startActivity(aboutIntent)
            }
        builder.create().show()
    }
}