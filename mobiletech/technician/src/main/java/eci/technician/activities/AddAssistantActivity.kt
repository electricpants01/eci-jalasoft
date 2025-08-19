package eci.technician.activities

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.TechniciansAdapter
import eci.technician.databinding.ActivityAddAssistantBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.TechnicianItem
import eci.technician.repository.DatabaseRepository
import eci.technician.tools.Constants
import eci.technician.viewmodels.ViewAssistantsViewModel
import kotlinx.coroutines.GlobalScope


class AddAssistantActivity : BaseActivity(), DialogInterface.OnClickListener {
    private var searchMenuItem: MenuItem? = null
    private var adapter: TechniciansAdapter? = null
    private lateinit var binding: ActivityAddAssistantBinding
    private var orderId: Int = 0

    val viewModel: ViewAssistantsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_assistant)
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.ADD_ASSISTANT_ACTIVITY)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        orderId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        binding.recItems.layoutManager = LinearLayoutManager(this)
        binding.recItems.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        viewModel.fetchAllTechnicians()
        observeLoading()
        observeNetworkError()
        observeTechniciansList()

    }

    private fun observeTechniciansList() {
        viewModel.technicianList.observe(this) { technicians ->
            var technicianItems = technicians.toMutableList()
            if (!technicianItems.isNullOrEmpty()) {
                val techNameComparator =
                    Comparator { tech1: TechnicianItem, tech2: TechnicianItem ->
                        tech1.firstName.compareTo(tech2.firstName)
                    }
                technicianItems.sortWith(techNameComparator)
            } else {
                technicianItems = mutableListOf()
            }


            val currentAssistants =
                DatabaseRepository.getInstance().getCurrentAssistants(orderId)
                    .map { it.technicianId }.distinct().toMutableList()

            currentAssistants.add(AppAuth.getInstance().technicianUser.technicianNumber)
            adapter = TechniciansAdapter(technicianItems
                .filter { !currentAssistants.contains(it.id) },
                object : TechniciansAdapter.TechnicianClickListener {
                    override fun onItemClick(technician: TechnicianItem) {
                        val msg = "Select Assistance action"
                        FBAnalyticsConstants.logEvent(this@AddAssistantActivity,FBAnalyticsConstants.AddAssistantActivity.SELECT_ASSISTANCE_ACTION, msg)
                        confirmAdd(technician)
                    }
                })
            binding.recItems.adapter = adapter
        }
    }


    private fun observeNetworkError() {
        viewModel.networkError.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { pair ->
                showNetworkErrorDialog(pair, this, supportFragmentManager)
            }
        }
    }

    private fun observeLoading() {
        viewModel.loadingAssistants.observe(this) { showLoading ->
            if (showLoading) {
                showProgressBar()
            } else {
                hideProgressBar()
            }
        }
    }

    private fun confirmAdd(technician: TechnicianItem) {
        val builder = AlertDialog.Builder(this)
        builder.setPositiveButton(R.string.confirm) { _, _ ->
            addAssistantToCall(technician)
        }
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        builder.setTitle(R.string.add_assistant)
        builder.setMessage("Add ${technician.fullName}?")
        builder.show()
    }

    private fun addAssistantToCall(technician: TechnicianItem) {
        showProgressBar()
        val map = HashMap<String, Any>()
        map["CallId"] = orderId
        map["TechnicianId"] = technician.id

        RetrofitRepository.RetrofitRepositoryObject.getInstance().addAssistance(map, this)
            .observe(this) {
                when (it.responseType) {
                    RequestStatus.SUCCESS -> {
                        hideProgressBar()
                        refreshServiceOrder(orderId)
                        finish()
                    }
                    else -> {
                        hideProgressBar()
                        val error = it.onError
                        this.showNetworkErrorDialog(error, this, supportFragmentManager)
                    }
                }
            }
    }

    private fun refreshServiceOrder(serviceOrderCallNumberId: Int) {
        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .getOneServiceCallByCallId(serviceOrderCallNumberId, GlobalScope, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_assistant, menu)
        searchMenuItem = menu?.findItem(R.id.search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                adapter?.setQuery(newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recItems.visibility = View.GONE
        setEnableOptionsMenu(false)
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.recItems.visibility = View.VISIBLE
        setEnableOptionsMenu(true)
    }

    private fun setEnableOptionsMenu(enable: Boolean) {
        searchMenuItem?.let { it.isEnabled = enable }
        for (menuItem in binding.toolbar.menu.iterator()) {
            menuItem.isEnabled = enable
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        finish()
    }
}
