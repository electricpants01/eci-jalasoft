package eci.technician.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import eci.technician.R

open class SimpleAlertDialog : DialogFragment(){

    companion object{

        const val TITLE_PARAMETER = "title_parameter"
        const val BOOLEAN_PARAMETER = "boolean_parameter"
        const val MESSAGE_PARAMETER = "message_parameter"
        const val POSITIVE_BUTTON_NAME_PARAMETER = "positive_button_name"
        const val JUST_ONE_BUTTON = "just_one_button"

        /**
         * Generate a new instance of the Dialog fragment handling
         * the message content at the fragment bundle
         * @param message the message content
         * @return SimpleAlertDialog instance generated
        */
        fun createInstance(title:String = "",message: String, positiveName:String, justOneButton:Boolean): SimpleAlertDialog{
            val dialogInstance = SimpleAlertDialog()
            val argumentsToSend = Bundle()
            argumentsToSend.putString(TITLE_PARAMETER, title)
            argumentsToSend.putString(MESSAGE_PARAMETER, message)
            argumentsToSend.putString(POSITIVE_BUTTON_NAME_PARAMETER, positiveName)
            argumentsToSend.putBoolean(JUST_ONE_BUTTON, justOneButton)

            dialogInstance.arguments = argumentsToSend
            return dialogInstance
        }
    }

    interface DialogConfirmationListener{
        fun onPositiveButtonPress()
        fun onNegativeButtonPress()
    }

    var listener : DialogConfirmationListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {activityContainer ->

            // default values
            var title = ""
            var oneButton = false
            var message = ""
            var positiveButtonName = getString(R.string.confirm)

            arguments?.let {
                title = it.getString(TITLE_PARAMETER, "")
                oneButton = it.getBoolean(JUST_ONE_BUTTON, false)
                message = it.getString(MESSAGE_PARAMETER, "")
                positiveButtonName = it.getString(POSITIVE_BUTTON_NAME_PARAMETER, getString(R.string.confirm))
            }

            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(activityContainer)
            // set the title from the arguments
            if (title.isNotEmpty()) {
                builder.setTitle(title)
            }
            // set the message content from the arguments
            builder.setMessage(message)
                    .setPositiveButton(positiveButtonName)
                    { _, _ ->
                        listener?.onPositiveButtonPress()
                    }

            if(!oneButton){
                builder.setNegativeButton(R.string.cancel
                ) { _, _ ->
                    listener?.onNegativeButtonPress()
                }
            }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity container cannot be null")
    }

    /**
     *  Use the show default method but also setup the listener for callbacks to handle at the
     *  DialogManager
     *  @param dialogListener the listener implementation to handle the dialog callbacks
    */
    fun showDialog(manager: FragmentManager?, dialogListener : DialogConfirmationListener?){
        isCancelable = false
        manager?.let { show(it,"simple_alert_dialog") }
        listener = dialogListener
    }

}