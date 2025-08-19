package eci.technician.activities.requestNeededParts

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivityPartsBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.tools.Constants

class PartsActivity : BaseActivity() {
    private lateinit var binding: ActivityPartsBinding
    private val viewModel: PartUseViewModel by viewModels()
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.PARTS_ACTIVITY)
        binding = ActivityPartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        viewModel.callId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        viewModel.holdCodeId = intent.getIntExtra(Constants.EXTRA_HOLD_CODE_ID, 0)
        viewModel.isRequestingPart = intent.getBooleanExtra(Constants.EXTRA_REQUEST, false)
        setupBar()
    }

    private fun setupBar() {
        appBarConfiguration = AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
            onBackPressed()
            true
        }.build()
        binding.appbarIncluded.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
                || super.onSupportNavigateUp()
    }

}
