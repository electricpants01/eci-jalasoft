package com.jumptech.ui.customDialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.jumptech.jumppod.R

class LocationDialog(
) : DialogFragment() {

    lateinit var listener: LocationDialogListener

    companion object{
        const val IS_ANDROID_Q_AND_ABOVE = "isAndroidQAndAbove"
    }

    interface LocationDialogListener {
        fun onAccept()
        fun onReject()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as LocationDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val isAndroidQAndAbove = arguments?.getBoolean(IS_ANDROID_Q_AND_ABOVE)
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val locationView = inflater.inflate(R.layout.container_location_disclosure, null)
            locationView.findViewById<TextView>(R.id.allowAllTheTimeTextView).visibility =
                if (isAndroidQAndAbove == true) View.VISIBLE else View.GONE
            if (Build.VERSION.SDK_INT >= 31) {
                locationView.findViewById<TextView>(R.id.allowAllTheTimeTextAndroid12View) .visibility = View.VISIBLE
                locationView.findViewById<TextView>(R.id.allowAllTheTimeTextView).visibility =View.GONE
            }
            builder.setView(locationView)
                // Add action buttons
                .setPositiveButton(
                    getString(R.string.turn_on)
                ) { _, _ ->
                    listener.onAccept()
                }
                .setNeutralButton(
                    getString(R.string.no_thanks)
                ) { dialog, _ ->
                    listener.onReject()
                    dialog.dismiss()
                }
                .setCancelable(false)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}