package eci.technician.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import eci.technician.R
import eci.technician.activities.fieldTransfer.FieldTransferActivity
import eci.technician.helpers.notification.NotificationManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationType: String =
            intent?.extras?.getString(NotificationManager.NOTIFICATION_TYPE_KEY, "") ?: ""

        val splashWasDisplayed = savedInstanceState != null
        if (!splashWasDisplayed) {
            installSplashScreen()
            handleNavigation(notificationType)
        } else {
            setTheme(R.style.AppTheme)
            setContentView(R.layout.activity_splash)
            startActivity(Intent(this, LocationPermissionActivity::class.java))
            finish()
        }

    }

    private fun handleNavigation(notificationType: String) {
        when (notificationType) {
            NotificationManager.NOTIFICATION_TYPE_FIELD_REQUEST_TRANSFER -> {
                goToFieldTransferActivity()
            }
            NotificationManager.NOTIFICATION_TYPE_CHAT_MESSAGE -> {
                //do nothing
            }
            NotificationManager.NOTIFICATION_TYPE_NEW_SERVICE_CALL -> {
                goToLocationPermissionActivity()
            }
            else -> {
                goToLocationPermissionActivity()
            }
        }
    }

    private fun goToLocationPermissionActivity() {
        startActivity(Intent(this, LocationPermissionActivity::class.java))
        finish()
    }

    private fun goToFieldTransferActivity() {
        val fieldTransferIntent = Intent(this, FieldTransferActivity::class.java)
        fieldTransferIntent.putExtra(FieldTransferActivity.OPEN_FROM_NOTIFICATION, true)
        startActivity(fieldTransferIntent)
        finish()
    }
}