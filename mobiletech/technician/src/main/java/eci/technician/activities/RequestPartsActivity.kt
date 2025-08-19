package eci.technician.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.requestNeededParts.PartsActivity
import eci.technician.adapters.RequestPartsAdapter
import eci.technician.dialog.DialogManager
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.tools.Constants
import eci.technician.viewmodels.RequestPartsViewModel
import kotlinx.android.synthetic.main.activity_request_parts.*

class RequestPartsActivity : BaseActivity(), RequestPartsAdapter.ClearAdapterPartListener {

    private var adapter: RequestPartsAdapter? = null
    private val viewModel: RequestPartsViewModel by viewModels()

    private val startPartsActivityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val itemId = data.getIntExtra(Constants.EXTRA_PART_ID, 0)
                val name = data.getStringExtra(Constants.EXTRA_PART_NAME) ?: ""
                val quantity = data.getDoubleExtra(Constants.EXTRA_QUANTITY, 0.0)
                adapter?.addPart(itemId, quantity, name)
                btnRequestParts.isEnabled = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_parts)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.REQUEST_PARTS_ACTIVITY)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        adapter = RequestPartsAdapter(this)
        recRequestedParts.layoutManager = LinearLayoutManager(this)
        recRequestedParts.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        recRequestedParts.adapter = adapter

        btnAddPart.setOnClickListener {
            val intent = Intent(this, PartsActivity::class.java)
            intent.putExtra(Constants.EXTRA_ADD_PART, true)
            intent.putExtra(Constants.EXTRA_REQUEST, true)
            intent.putExtra(Constants.SEARCH_FROM_REQUEST_PARTS, true)
            intent.putExtra(Constants.REQUESTED_PARTS, true)
            startPartsActivityForResult.launch(intent)
        }

        btnRequestParts.setOnClickListener {
            requestParts()
        }

        observeLoading()
        observeNetworkError()
        observeSuccessRequestParts()

    }

    private fun observeSuccessRequestParts() {
        viewModel.successRequestParts.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { isSuccess ->
                if (isSuccess) {
                    adapter?.clear()
                    DialogManager.showSimpleAlertDialog(
                        message = getString(R.string.part_request_success),
                        positiveButtonName = getString(R.string.ok),
                        fragmentManager = supportFragmentManager,
                        justOneButton = true
                    ) {
                        finish()
                    }
                }
            }
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
        viewModel.loading.observe(this) { showLoading ->
            showProgressBar(showLoading)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            verifyItems()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        verifyItems()
    }

    private fun verifyItems() {
        val currentParts = adapter?.getParts()
        if (currentParts != null && currentParts.isNotEmpty()) {
            DialogManager.showSimpleAlertDialog(
                message = getString(R.string.request_part_quit_message),
                positiveButtonName = getString(R.string.discard),
                fragmentManager = supportFragmentManager
            ) { confirmClose ->
                if (confirmClose)
                    super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun showProgressBar(show: Boolean) {
        loaderContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun requestParts() {
        val parts = adapter?.getParts()
        viewModel.requestParts(parts?.toList())
    }

    private fun showErrorDialog() {
        DialogManager.showSimpleAlertDialog(
            message = getString(R.string.part_request_failed),
            positiveButtonName = getString(R.string.ok), fragmentManager = supportFragmentManager
        ) { confirmClose ->
            if (confirmClose) {
                requestParts()
            }
        }
    }

    override fun onAdapterCleared() {
        btnRequestParts.isEnabled = false
    }
}
