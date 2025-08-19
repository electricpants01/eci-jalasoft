package eci.technician.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import eci.technician.R
import eci.technician.databinding.ActivityCompletedCallsDetailBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.order.CompletedServiceOrder
import eci.technician.repository.CompletedCallsRepository
import eci.technician.repository.DatabaseRepository
import eci.technician.tools.Constants
import io.realm.OrderedRealmCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompletedCallsDetailActivity : AppCompatActivity() {

    lateinit var binding: ActivityCompletedCallsDetailBinding

    companion object {
        const val EXTRA_COMPLETED_CALL_ID = "extra_completed_call_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.COMPLETED_CALLS_DETAIL_ACTIVITY)
        val completedCallCode: String =
            intent?.extras?.getString(Constants.EXTRA_COMPLETED_CALL_NUMBER_CODE)
                ?: ""
        val completedCallId: String = intent?.extras?.getString(EXTRA_COMPLETED_CALL_ID) ?: ""
        binding = DataBindingUtil.setContentView(this, R.layout.activity_completed_calls_detail)
        setSupportActionBar(binding.toolbar)
        title = getString(R.string.completed_call_detail_title, completedCallCode)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUIForOneCompletedCall(completedCallId)
    }

    private fun setUIForOneCompletedCall(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val completedCall = CompletedCallsRepository.getCompletedCallById(id)
            withContext(Dispatchers.Main) {
                completedCall?.let {
                    binding.item = it
                } ?: kotlin.run {
                    finish()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}