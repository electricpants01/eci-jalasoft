package eci.technician.activities.transfers

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.notes.NotesActivity
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.transfers.Warehouse
import kotlinx.android.synthetic.main.activity_about.*

class TransfersActivity : BaseActivity() {

    companion object {
        const val TRANSFER_TYPE = "transfer_type"
        const val CUSTOMER_WAREHOUSE_ID = "customer_warehouse_id"
    }

    val viewModel: TransferViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.TRANSFER_ACTIVITY)
        setContentView(R.layout.activity_transfers)
        viewModel.transferType = intent.getIntExtra(TRANSFER_TYPE, Warehouse.COMPANY_TYPE)
        viewModel.customerWarehouseId = intent.getIntExtra(CUSTOMER_WAREHOUSE_ID, 0)
        setupBar()
    }

    private fun setupBar(){
        navController = findNavController(R.id.mainTransferFragment)
        appBarConfiguration = AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
            onBackPressed()
            true
        }.build()
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.mainTransferFragment).navigateUp()
                || super.onSupportNavigateUp()
    }
}
