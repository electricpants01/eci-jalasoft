package eci.technician.broadcast

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import eci.technician.tools.Constants

class ShareBroadCastReceiver : BroadcastReceiver() {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        if (!intent.hasExtra(Intent.EXTRA_CHOSEN_COMPONENT)) return
        val appComponentName = intent.extras?.get(Intent.EXTRA_CHOSEN_COMPONENT) as ComponentName?
        if (appComponentName != null) {
            val name = appComponentName.packageName
            context?.let {
                context.getSharedPreferences(Constants.PREFERENCE_NAVIGATION, Context.MODE_PRIVATE)
                    .edit()
                    .putString(Constants.PREFERENCE_NAVIGATION_APP, name).apply()
            }
        }
    }
}

