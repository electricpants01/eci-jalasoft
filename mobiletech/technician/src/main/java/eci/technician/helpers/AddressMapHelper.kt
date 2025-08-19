package eci.technician.helpers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import eci.technician.R
import eci.technician.broadcast.ShareBroadCastReceiver
import eci.technician.tools.Constants
import java.io.IOException
import java.util.*

object AddressMapHelper {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun openNavigation(customerFullAddress: String?, context: Context) {
        val navigationAppName = context.getSharedPreferences(Constants.PREFERENCE_NAVIGATION, Context.MODE_PRIVATE)
            .getString(Constants.PREFERENCE_NAVIGATION_APP, "")
        val geocoder = Geocoder(context, Locale.getDefault())
        val gmmIntentUri: Uri
        try {
            val addresses = geocoder.getFromLocationName(customerFullAddress, 1)
            if (addresses.isNotEmpty()) {
                customerFullAddress?.let { customerFullAddress ->
                    gmmIntentUri =
                        if (navigationAppName != null && navigationAppName == Constants.GOOGLE_MAPS_PACKAGE_NAME) {
                            Uri.parse("google.navigation:q=$customerFullAddress")
                        } else {
                            Uri.parse("geo:" + addresses[0].latitude + "," + addresses[0].longitude + "?q=" + customerFullAddress)
                        }
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    val receiverIntent = Intent(context, ShareBroadCastReceiver::class.java)
                    val pendingIntent = getChooserPendingIntent(context, receiverIntent)
                    val chooser = Intent.createChooser(
                        mapIntent,
                        context.resources.getString(R.string.openAppChooserTitle),
                        pendingIntent.intentSender
                    )
                    if (navigationAppName != null && navigationAppName.isNotEmpty()) {
                        if (isPackageInstalled(navigationAppName, context.packageManager)) {
                            mapIntent.setPackage(navigationAppName)
                        }
                    }
                    context.startActivity(chooser)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getChooserPendingIntent(context: Context, receiver: Intent): PendingIntent {
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                receiver,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(context, 0, receiver, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return pendingIntent
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}