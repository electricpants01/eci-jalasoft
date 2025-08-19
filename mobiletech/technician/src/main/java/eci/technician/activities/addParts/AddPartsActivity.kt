package eci.technician.activities.addParts

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivityAddPartsBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.tools.Constants

class AddPartsActivity:  BaseActivity() {
    lateinit var binding: ActivityAddPartsBinding
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration
    val viewModel: AddPartViewModel by viewModels()
    private var orderId: Int = 0
    private var addPartAsPending = false
    private var customerWarehouseId = 0

    companion object {
        const val EXTRA_PENDING_PART = "extra_pending_part"
        const val EXTRA_CUSTOMER_WAREHOUSE_ID = "customer_warehouse_id"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.ADD_PARTS_ACTIVITY)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        setupBar()
        orderId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        customerWarehouseId = intent.getIntExtra(EXTRA_CUSTOMER_WAREHOUSE_ID, 0)
        viewModel.callId = orderId
        viewModel.customerWarehouseId = customerWarehouseId
        addPartAsPending = intent.getBooleanExtra(EXTRA_PENDING_PART, false)
        viewModel.addPartAsPending = addPartAsPending
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