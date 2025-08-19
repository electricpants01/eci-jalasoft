package eci.technician.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import eci.technician.R
import eci.technician.custom.CustomAddPartDialog
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants

/**
 *
 * This object has the purpose to manage all the possible dialogs on the application and using
 * completion return the user choice.
 *
 */
object DialogManager {

    /**
     * This method generate a new instance of the SimpleAlertDialog and returns the user choice at the completion
     * @param message Dialog Message
     * @param positiveButtonName the name of the positive action button
     * @param fragmentManager SupportFragmentManager
     * @param justOneButton define if we just need one button for the dialog
     * @param completion will handle the result
     */
    fun showSimpleAlertDialog(
        title: String = "",
        message: String,
        positiveButtonName: String,
        fragmentManager: FragmentManager,
        justOneButton: Boolean = false,
        completion: (Boolean) -> Unit
    ) {
        val dialog =
            SimpleAlertDialog.createInstance(title, message, positiveButtonName, justOneButton)
        dialog.showDialog(fragmentManager, object : SimpleAlertDialog.DialogConfirmationListener {
            override fun onPositiveButtonPress() {
                completion(true)
            }

            override fun onNegativeButtonPress() {
                completion(false)
            }
        })
    }

    fun showClockOutDialog(
        title: String, fragmentManager: FragmentManager,
        completion: (Int) -> Unit
    ) {
        val dialog = ClockOutDialog.createInstance(title, false)
        dialog.showClockOutDialog(fragmentManager, object : ClockOutDialog.ClockOutValueListener {
            override fun onValidClockOutValue(value: Int) {
                completion(value)
            }

            override fun onInvalidClockOutValue() {
                completion(-1)
            }

            override fun onCanceledClick() {
                // TODO("Implement on loading state for TimeCardsFragments")
            }

        })

    }

    fun showClockInDialog(
        title: String, fragmentManager: FragmentManager,
        completion: (Int) -> Unit,
        onCancel:() -> Unit
    ) {
        val dialog = ClockOutDialog.createInstance(title, true)
        dialog.showClockOutDialog(fragmentManager, object : ClockOutDialog.ClockOutValueListener {
            override fun onValidClockOutValue(value: Int) {
                completion(value)
            }

            override fun onInvalidClockOutValue() {
                completion(-1)
            }

            override fun onCanceledClick() {
                onCancel.invoke()
            }

        })

    }

    fun showAddPartDialog(
        binName: String,
        binAvailableQuantityUI: Double,
        serialNumber: String,
        context: Context,
        childFragmentManager: FragmentManager,
        onConfirm: (quantity: Double) -> Unit,
        onDeny: () -> Unit
    ) {
        CustomAddPartDialog(binName, binAvailableQuantityUI, serialNumber, { quantity ->
            if (quantity > binAvailableQuantityUI) {
                showSimpleAlertDialog(
                    context.getString(R.string.warning),
                    context.getString(R.string.negative_quantity),
                    context.getString(R.string.ok),
                    childFragmentManager,
                    false
                ) { isConfirmed ->
                    if (isConfirmed) {
                        onConfirm.invoke(quantity)
                    } else {
                        onDeny.invoke()
                    }
                }
            } else {
                onConfirm.invoke(quantity)
            }

        }, {
            onDeny.invoke()
        }).show(childFragmentManager, "AddPart")
    }

    fun createErrorDialog(
        title: String,
        description: String,
        context: Context,
        onPositive: () -> Unit,
        onDismiss: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(description)
            .setPositiveButton(android.R.string.ok) { _, _ -> onPositive.invoke() }
            .setOnDismissListener { onDismiss.invoke() }
            .setCancelable(false)
            .create()
    }
}