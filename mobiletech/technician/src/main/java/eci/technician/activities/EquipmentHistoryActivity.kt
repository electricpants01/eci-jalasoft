package eci.technician.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.EquipmentHistoryAdapter
import eci.technician.databinding.ActivityEquipmentHistoryBinding
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.EquipmentHistoryModel
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager

class EquipmentHistoryActivity : BaseActivity() {
    lateinit var binding: ActivityEquipmentHistoryBinding
    private var adapter: EquipmentHistoryAdapter? = null
    companion object{
        const val TAG = "EquipmentHistoryActivity"
        const val EXCEPTION = "Exception"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_equipment_history)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.EQUIPMENT_HISTORY_ACTIVITY)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        adapter = EquipmentHistoryAdapter()
        binding.recHistory.layoutManager = SafeLinearLayoutManager(this)
        binding.recHistory.adapter = adapter
        val historyFilterAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.history_filter_items))
        binding.spinFilter.adapter = historyFilterAdapter
        binding.spinFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                adapter?.setFilterTypeAdapter(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "onNothingSelected")
            }
        }
        updateFromServer(false)
    }


    private fun updateFromServer(updateData: Boolean) {
        showProgressBar()
        if (updateData) {
            getEquipmentHistory()
        } else {
            updateEquipmentHistory()
        }
    }

    private fun updateEquipmentHistory() {
        RetrofitRepository
                .RetrofitRepositoryObject
                .getInstance()
                .updateEquipmentHistoryByEquipmentId(intent.getIntExtra(Constants.EXTRA_EQUIPMENT_ID, 0).toString(), this)
                .observe(this, {
                    manageDataOnResponse(it)
                })
    }

    private fun getEquipmentHistory() {
        RetrofitRepository
                .RetrofitRepositoryObject
                .getInstance()
                .getEquipmentHistoryByEquipmentId(intent.getIntExtra(Constants.EXTRA_EQUIPMENT_ID, 0).toString(), this)
                .observe(this, {
                    manageDataOnResponse(it)
                })
    }

    private fun manageDataOnResponse(genericDataResponse: GenericDataResponse<MutableList<EquipmentHistoryModel>>) {
        when (genericDataResponse.responseType) {
            RequestStatus.SUCCESS -> {
                genericDataResponse.data?.let { equipmentHistoryList ->
                    fillItems(equipmentHistoryList)
                }
            }
            RequestStatus.ERROR -> {
                fillItems(mutableListOf())
                this.showNetworkErrorDialog(genericDataResponse.onError, this, supportFragmentManager)
            }
            else -> {
                fillItems(mutableListOf())
            }
        }
    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.containerEquipmentView.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.containerEquipmentView.visibility = View.VISIBLE
    }

    private fun fillItems(elements: MutableList<EquipmentHistoryModel>) {
        hideProgressBar()
        adapter?.setElementsList(elements)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_equipment_history, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.refresh) {
            updateFromServer(true)
        }
        return super.onOptionsItemSelected(item)
    }
}