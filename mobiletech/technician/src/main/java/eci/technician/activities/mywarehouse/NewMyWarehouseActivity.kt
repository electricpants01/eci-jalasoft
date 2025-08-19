package eci.technician.activities.mywarehouse

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivityNewMyWarehouseBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants

class NewMyWarehouseActivity : BaseActivity() {
    lateinit var binding:ActivityNewMyWarehouseBinding
    val viewModel: NewMyWarehouseViewModel by viewModels()
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.NEW_MY_WAREHOUSE_ACTIVITY)
        binding = ActivityNewMyWarehouseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        setupBar()

    }

    private fun setupBar(){
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