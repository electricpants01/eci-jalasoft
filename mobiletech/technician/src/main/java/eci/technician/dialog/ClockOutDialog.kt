package eci.technician.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ClockOutHelper
import java.lang.Exception
import java.util.*

class ClockOutDialog() : SimpleAlertDialog() {

    interface ClockOutValueListener {
        fun onValidClockOutValue(value: Int)
        fun onInvalidClockOutValue()
        fun onCanceledClick()
    }

    companion object {

        /**
         * Generate a new instance of the Dialog fragment handling
         * the title at the fragment bundle
         * @param title dialog
         * @return ClockOutDialog instance generated
         */
        fun createInstance(title: String, isClockIn: Boolean): ClockOutDialog {
            val dialogInstance = ClockOutDialog()
            val argumentsToSend = Bundle()
            argumentsToSend.putString(TITLE_PARAMETER, title)
            argumentsToSend.putBoolean(BOOLEAN_PARAMETER, isClockIn)

            dialogInstance.arguments = argumentsToSend
            return dialogInstance
        }
    }

    var clockOutListener: ClockOutValueListener? = null

    private lateinit var dialogAlert: AlertDialog

    private var positiveButton: Button? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activityContainer ->

            val title = arguments?.getString(TITLE_PARAMETER, "")
            val isClockIn = arguments?.getBoolean(BOOLEAN_PARAMETER) ?: false

            val message = ""
            val positiveButtonName = if (isClockIn) getString(R.string.clock_in) else getString(R.string.clock_out)

            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activityContainer)

            val dialogView = LayoutInflater.from(activity).inflate(R.layout.odometer_dialog, null)

            builder.setView(dialogView)

            val txtOdometer = dialogView.findViewById(R.id.txtOdometer) as EditText
            val lastOdometerReading: Int = AppAuth.getInstance().lastOdometer.toInt()
            txtOdometer.setText(lastOdometerReading.toString())

            txtOdometer.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    positiveButton?.let { positiveButton ->
                        positiveButton.isEnabled = !s.isNullOrEmpty()
                        context?.let {

                            if (s.isNullOrEmpty()) {
                                positiveButton.setTextColor(ContextCompat.getColor(it, R.color.disabled_button))
                            } else {
                                positiveButton.setTextColor(ContextCompat.getColor(it, R.color.colorAccent))
                            }
                        }
                    }
                }
            })

            val lastOdometerValue = dialogView.findViewById(R.id.lastOdometerValue) as TextView
            lastOdometerValue.text = String.format(Locale.getDefault(), "Last odometer: %d", AppAuth.getInstance().lastOdometer)

            // set the title from the arguments
            if (!title.isNullOrEmpty()) {
                builder.setTitle(title)
            }
            if (message.isNotEmpty()) {
                builder.setMessage(message)
            }

            builder.setNegativeButton(getString(R.string.cancel)) { _,_ ->
                clockOutListener?.onCanceledClick()
            }
            // set the message content from the arguments
            builder.setPositiveButton(positiveButtonName)
            { _, _ ->
                val clockOutValue = txtOdometer.text.toString()
                val clockOut = ClockOutHelper.clockOut(clockOutValue)
                clockOutListener?.onValidClockOutValue(Integer.parseInt(clockOutValue))
            }

            // Create the AlertDialog object and return it
            dialogAlert = builder.create()
            dialogAlert.window?.let {
                it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                txtOdometer.requestFocus()
                txtOdometer.setSelection(txtOdometer.getText().length)
            }

            return dialogAlert
        } ?: throw IllegalStateException("Activity container cannot be null")
    }


    fun showClockOutDialog(manager: FragmentManager?, clockOutValueListener: ClockOutValueListener?) {
        manager?.let {
            showNow(it, "simple_alert_dialog").let {
                isCancelable = false
                clockOutListener = clockOutValueListener
                //Disable positive button until the user change the editText content
                positiveButton = dialogAlert.getButton(AlertDialog.BUTTON_POSITIVE)
            }
        }
    }

}