package eci.technician.activities.searchWarehouses

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivitySearchWarehousesPartsBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants

class SearchWarehousesPartsActivity : BaseActivity() {

    lateinit var binding: ActivitySearchWarehousesPartsBinding
    val viewModel: SearchWarehousesViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.SEARCH_WAREHOUSES_PARTS_ACTIVITY)
        binding = ActivitySearchWarehousesPartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
                || super.onSupportNavigateUp()
    }
}