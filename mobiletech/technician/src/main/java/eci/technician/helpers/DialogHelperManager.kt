package eci.technician.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog

object DialogHelperManager {

    fun displayMessage(title: String, message: String, positiveButton: String, negativeButton: String, context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton) { dialog, which ->

                    dialog.dismiss()
                }
                .setNegativeButton(negativeButton, null)
                .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.d("Error3", e.toString())
        }
    }
    fun displayOkMessage(title: String, message: String, positiveButton: String, context: Context, task: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { dialog, _ ->
                task.invoke()
                dialog.dismiss()
            }
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.d("Error3", e.toString())
        }
    }
}