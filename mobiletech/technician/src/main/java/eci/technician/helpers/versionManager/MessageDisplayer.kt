package eci.technician.helpers.versionManager

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import eci.technician.R

object MessageDisplayer {

    const val TAG = "MessageDisplayer"
    const val EXCEPTION = "Exception"

    fun displayMessage(message: String, context: Context, task: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.warning))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.update)) { dialog, _ ->
                val appPackageName: String = context.packageName
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$appPackageName")
                        )
                    )
                } catch (anfe: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                        )
                    )
                }
                dialog.dismiss()
            }
//          Uncomment if is needed to bring back the OK button
//            .setNegativeButton(context.getString(R.string.ok)) { _, _ ->
//                task.invoke()
//            }
            .setCancelable(false)
        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }
}