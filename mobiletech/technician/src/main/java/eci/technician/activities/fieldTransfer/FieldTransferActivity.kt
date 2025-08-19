package eci.technician.activities.fieldTransfer

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.BaseActivity
import eci.technician.MainActivity
import eci.technician.R
import eci.technician.databinding.ActivityFieldTransferBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants

class FieldTransferActivity : BaseActivity() {

    lateinit var binding: ActivityFieldTransferBinding
    val viewModel: FieldTransferViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController


    companion object {
        const val TAG = "FieldTransferActivity"
        const val EXCEPTION = "Exception"
        const val OPEN_FROM_NOTIFICATION = "openFromNotification"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFieldTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.hasBeenOpenedFromNotification =
            intent.getBooleanExtra(OPEN_FROM_NOTIFICATION, false)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.FIELD_TRANSFER_ACTIVITY)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_field_transfer) as NavHostFragment
        navController = navHostFragment.navController
        setupBar()
    }

    private fun setupBar() {
        appBarConfiguration = AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
            onBackPressed()
            true
        }.build()
        binding.appbarIncluded.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onBackPressed() {
        if (viewModel.hasBeenOpenedFromNotification){
            openMainActivity()
        }else {
            super.onBackPressed()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
