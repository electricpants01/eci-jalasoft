package eci.technician

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import eci.technician.dialog.DialogManager
import eci.technician.dialog.LoginDialog
import eci.technician.helpers.ErrorHelper.RequestError
import eci.technician.helpers.api.retroapi.ErrorType

abstract class BaseActivity : AppCompatActivity() {

    var loginDialog: LoginDialog? = null
    var baseErrorDialog: AlertDialog? = null

    fun showMessageBox(title: String?, message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    fun showMessageBox(
        title: String?,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, listener)
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    protected fun showQuestionBox(
        title: String?,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, listener)
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    protected fun showQuestionBoxWithCustomButtons(
        title: String?,
        message: String?,
        positiveButton: String?,
        negativeButton: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton, listener)
            .setNegativeButton(negativeButton, null)
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    protected fun showQuestionBoxWithCustomButtonsTwoListeners(
        title: String?,
        message: String?,
        positiveButton: String?,
        negativeButton: String?,
        listenerYes: DialogInterface.OnClickListener?,
        listenerNo: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton, listenerYes)
            .setNegativeButton(negativeButton, listenerNo)
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    fun showNetworkErrorDialog(
        error: RequestError?,
        context: Context,
        fragmentManager: FragmentManager
    ) {
        if (baseErrorDialog != null) return
        if (baseErrorDialog?.isShowing == true) return
        val title = getString(R.string.somethingWentWrong)
        val body = error?.description ?: "Error"

        if (error?.description?.contains("401") == true) {
            showWarningUnAuthorizedDialog(context, fragmentManager)
        } else {
            baseErrorDialog = DialogManager.createErrorDialog(title, body, this,
                onPositive = { baseErrorDialog = null },
                onDismiss = { baseErrorDialog = null })
            baseErrorDialog?.show()
        }
    }


    fun showNetworkErrorDialog(
        error: Pair<ErrorType, String?>?,
        context: Context,
        fragmentManager: FragmentManager,
        onPressOk: (() -> Unit)? = null
    ) {
        if (loginDialog != null) return
        if (baseErrorDialog != null) return
        if (baseErrorDialog?.isShowing == true) return
        val title = getString(R.string.somethingWentWrong)
        val body = error?.second ?: getString(R.string.error)

        if (error?.isUnAuthorized() == true) {
            showWarningUnAuthorizedDialog(context, fragmentManager)
        } else {
            baseErrorDialog = DialogManager.createErrorDialog(title, body, this,
                onPositive = {
                    baseErrorDialog = null
                    onPressOk?.invoke()
                },
                onDismiss = { baseErrorDialog = null })
            baseErrorDialog?.show()
        }
    }

    private fun Pair<ErrorType, String?>.isUnAuthorized(): Boolean {
        return first == ErrorType.NOT_SUCCESSFUL && second?.contains("401") == true
    }

    protected fun showAffirmationBox(
        title: String?,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, listener)
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    fun showUnavailableWhenOfflineMessage() {
        showMessageBox("", getString(R.string.offline_warning))
    }


    private fun showWarningUnAuthorizedDialog(context: Context, fragmentManager: FragmentManager) {
        val title = getString(R.string.warning)
        val body = getString(R.string.your_session_has_expired)

        baseErrorDialog?.dismiss()
        baseErrorDialog = null
        baseErrorDialog = DialogManager.createErrorDialog(title, body, context,
            onPositive = {
                baseErrorDialog = null
                showLoginDialog(fragmentManager)
            }, onDismiss = { baseErrorDialog = null })
        baseErrorDialog?.show()
    }


    private fun showLoginDialog(fragmentManager: FragmentManager) {
        if (loginDialog != null) return
        loginDialog = LoginDialog.newInstance(object : LoginDialog.ILoginListener {
            override fun onDismissLoginDialog() {
                loginDialog = null
            }
        })
        loginDialog?.show(fragmentManager, "LoginDialogTag")

    }


    companion object {
        const val TAG = "BaseActivity"
        private const val EXCEPTION = "Exception logger"

        @JvmStatic
        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            var view = activity.currentFocus
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun showKeyboard(activity: Activity) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }
}