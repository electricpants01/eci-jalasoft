package eci.technician.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.AssistantsAdapter
import eci.technician.databinding.ActivityViewAssistantsBinding
import eci.technician.helpers.AppAuth
import eci.technician.models.TechnicianItem
import eci.technician.models.order.ServiceCallLabor
import eci.technician.tools.Constants
import eci.technician.tools.Settings
import eci.technician.viewmodels.ViewAssistantsViewModel
import io.realm.Realm

class ViewAssistantsActivity : BaseActivity() {

    companion object {
        const val TAG = "ViewAssistantsActivity"
        const val EXCEPTION = "Exception"
    }

    private var serviceOrderId: Int = 0
    private var isAssist: Boolean = true
    private var isFromResume: Boolean = false

    private val viewModel by lazy {
        ViewModelProvider(this)[ViewAssistantsViewModel::class.java]
    }
    lateinit var binding: ActivityViewAssistantsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_view_assistants)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        serviceOrderId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        isAssist = intent.getBooleanExtra(Constants.EXTRA_IS_ASSIST, true)

        if (serviceOrderId == 0) {
            finish()
            return
        }

        binding.recAssistants.layoutManager = LinearLayoutManager(this)
        binding.recAssistants.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        setUpRecycler(mutableListOf(), mutableListOf())

        viewModel.fetchAllTechnicians()
        observeTechniciansList()
        observeLoading()
        observeNetworkError()
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

    private fun observeTechniciansList() {
        viewModel.technicianList.observe(this) { technicians ->
            val technicianItems = technicians.toMutableList()
            viewModel.getAllLaborsForServiceCall(serviceOrderId)
                .observe(this) { serviceCallLabors ->
                    hideProgressBar()
                    setUpRecycler(serviceCallLabors.toMutableList(), technicianItems)
                    setupEmptyMessage()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.emptyLinearLayout.visibility = View.GONE
        setupEmptyMessage()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_assistants, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setUpRecycler(
        serviceCallLaborList: MutableList<ServiceCallLabor>,
        techList: MutableList<TechnicianItem>
    ) {
        val laborList = orderLaborsByTechName(techList, serviceCallLaborList)
        val adapter = AssistantsAdapter(techList, laborList)
        binding.recAssistants.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun setupEmptyMessage() {
        if (isFromResume) {
            if (binding.recAssistants.adapter?.itemCount ?: 1 == 0) {
                binding.emptyLinearLayout.visibility = View.VISIBLE
            } else {
                binding.emptyLinearLayout.visibility = View.GONE
            }
        } else {
            isFromResume = !isFromResume
        }
    }

    private fun orderLaborsByTechName(
        techList: MutableList<TechnicianItem>,
        serviceCallLaborList: MutableList<ServiceCallLabor>
    ): MutableList<ServiceCallLabor> {
        val realm = Realm.getDefaultInstance()
        val newLaborList = realm.copyFromRealm(serviceCallLaborList)
        try {
            for (serviceCallLabor in newLaborList) {
                for (technicianItem in techList) {
                    if (technicianItem.id == serviceCallLabor.technicianId) {
                        serviceCallLabor.techName = technicianItem.fullName
                    }
                }
            }
            newLaborList.sortBy { it.techName }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
        return serviceCallLaborList
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.add_assistant_action -> {
                if (AppAuth.getInstance().isConnected) {
                    showAssistantDialog()
                } else {
                    showUnavailableWhenOfflineMessage()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAssistantDialog() {
        if (isAssist) {
            showMessageBox(
                getString(R.string.add_assistant),
                getString(R.string.assist_add_assist_error)
            )
            return
        }
        val intent = Intent(this, AddAssistantActivity::class.java)
        intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrderId)
        startActivity(intent)

    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recAssistants.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.recAssistants.visibility = View.VISIBLE
    }
}
