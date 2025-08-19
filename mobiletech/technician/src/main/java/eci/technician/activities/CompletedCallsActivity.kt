package eci.technician.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.CompletedCallsAdapter
import eci.technician.adapters.CompletedCallsAdapter.CompletedCallListener
import eci.technician.databinding.ActivityCompletedCallsBinding
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository.RetrofitRepositoryObject.getInstance
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.order.CompletedServiceOrder
import eci.technician.repository.CompletedCallsRepository
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompletedCallsActivity : BaseActivity(), CompletedCallListener {
    lateinit var binding: ActivityCompletedCallsBinding

    companion object {
        const val TAG = "CompletedCallsActivity"
        const val EXCEPTION = "Exception"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_completed_calls)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.COMPLETED_CALLS_ACTIVITY)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loadCompleted()
    }

    private fun loadCompleted() {
        showProgressBar()
        getInstance().getTechnicianCompetedServiceCalls(this, lifecycleScope).observe(this, {
            manageDataOnResponse(it)
        })
    }

    private fun manageDataOnResponse(genericDataResponse: GenericDataResponse<MutableList<CompletedServiceOrder>>) {
        when (genericDataResponse.responseType) {
            RequestStatus.SUCCESS -> {
                initItems()
            }
            RequestStatus.ERROR -> {
                showNetworkErrorDialog(genericDataResponse.onError, this, supportFragmentManager)
                showEmptyList()
            }
            else -> {
                initItems()
            }
        }
        hideProgressBar()
    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recCompletedCalls.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.recCompletedCalls.visibility = View.VISIBLE
    }

    private fun showEmptyList() {
        binding.recCompletedCalls.visibility = View.GONE
        binding.progressBarContainer.visibility = View.GONE
        binding.emptyList.visibility = View.VISIBLE
    }

    private fun initItems() {
        lifecycleScope.launch(Dispatchers.IO){
            val completedServiceOrderList = CompletedCallsRepository.getCompletedCalls()
            val adapter = CompletedCallsAdapter(completedServiceOrderList, this@CompletedCallsActivity)
            withContext(Dispatchers.Main){
                binding.recCompletedCalls.layoutManager = SafeLinearLayoutManager(this@CompletedCallsActivity)
                binding.recCompletedCalls.adapter = adapter
                binding.emptyList.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCompletedCallTap(completedServiceOrder: CompletedServiceOrder?) {
        if (completedServiceOrder != null && completedServiceOrder.isValid) {
            val intent = Intent(this, CompletedCallsDetailActivity::class.java)
            intent.putExtra(Constants.EXTRA_COMPLETED_CALL_NUMBER_CODE, completedServiceOrder.callNumber_Code)
            intent.putExtra(CompletedCallsDetailActivity.EXTRA_COMPLETED_CALL_ID, completedServiceOrder.id)
            startActivity(intent)
        } else {
            initItems()
        }
    }
}