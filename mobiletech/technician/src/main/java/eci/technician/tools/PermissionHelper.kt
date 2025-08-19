package eci.technician.tools

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

object PermissionHelper {

    fun verifyTrackPermissions(context: Context) : Boolean{
        if (Build.VERSION.SDK_INT < 23) {
            return true
        }
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun verifyLocationBackgroundPermissions(context: Context): Boolean{
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}