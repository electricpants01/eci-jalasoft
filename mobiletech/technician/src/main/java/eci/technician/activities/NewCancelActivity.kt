package eci.technician.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.BaseActivity
import eci.technician.MainActivity
import eci.technician.R
import eci.technician.databinding.ActivityNewCancelBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.order.CancelCode
import eci.technician.models.order.StatusChangeModel
import eci.technician.repository.DatabaseRepository
import eci.technician.tools.Constants
import eci.technician.viewmodels.CancelCodeViewModel
import java.util.*

class NewCancelActivity : BaseActivity(), View.OnClickListener {

    lateinit var binding: ActivityNewCancelBinding
    var orderId: Int = 0
    var callStatusCode: String = ""
    var callNumberCode: String? = null
    private lateinit var viewModel: CancelCodeViewModel

    companion object {
        const val CANCEL_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCancelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.NEW_CANCEL_ACTIVITY)
        viewModel = ViewModelProvider(this).get(CancelCodeViewModel::class.java)
        setSupportActionBar(binding.toolbar)
        orderId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        callNumberCode = intent.getStringExtra(Constants.EXTRA_CALL_NUMBER_CODE)
        callStatusCode = intent.getStringExtra(Constants.EXTRA_CALL_STATUS_CODE) ?: ""
        callNumberCode?.let {
            title = getString(R.string.cancel_code_title, it)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel.getCancelCodeSelected().observe(this, {
            binding.cancelTitle.text = it.code
            if (it.description != null) {
                binding.cancelDescription.text = it.description
                binding.cancelDescription.visibility = View.VISIBLE
            } else {
                binding.cancelDescription.visibility = View.GONE
            }
        })

        binding.cancelCodeSelect.setOnClickListener(this)
        binding.btnSend.setOnClickListener(this)
        binding.txtComments.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                viewModel.setComments(p0.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //do nothing
            }

        })
        setObservers()
    }

    private fun setObservers() {
        observeLoading()
        observeNetworkError()
        observeSuccessEvent()
    }

    private fun observeSuccessEvent() {
        viewModel.successEvent.observe(this){
            it.getContentIfNotHandledOrReturnNull()?.let { isSuccess ->
                if (isSuccess){
                    DatabaseRepository.getInstance().deleteUsedParts(orderId)
                    openMainActivity()
                }
            }
        }
    }

    private fun observeNetworkError() {
        viewModel.networkError.observe(this){
            it.getContentIfNotHandledOrReturnNull()?.let { pair ->
                this.showNetworkErrorDialog(pair, this, supportFragmentManager)
            }
        }
    }

    private fun observeLoading() {
        viewModel.loading.observe(this) { showLoader ->
            binding.loaderIncluded.progressBarContainer.visibility =
                if (showLoader) View.VISIBLE else View.GONE
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.cancelCodeSelect -> {
                val intent = Intent(this, CancelCodesActivity::class.java)
                startActivityForResult(intent, CANCEL_CODE)
            }
            R.id.btnSend -> {
                cancelCall()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CANCEL_CODE && data != null) {
            val newCancelCodeSelected = CancelCode()
            newCancelCodeSelected.cancelCodeId = data.getIntExtra(Constants.EXTRA_CANCEL_CODE_ID, 0)
            newCancelCodeSelected.code = data.getStringExtra(Constants.EXTRA_CANCEL_CODE_TITLE)
            newCancelCodeSelected.description =
                data.getStringExtra(Constants.EXTRA_CANCEL_CODE_DESCRIPTION)
            viewModel.setCancelCodeSelected(newCancelCodeSelected)
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

    override fun onDestroy() {
        super.onDestroy()
        DatabaseRepository.getInstance().setInitialStateForCancelCodes()
    }

    private fun cancelCall() {
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.NewCancelActivity.CANCEL_ACTION)
        viewModel.getCancelCodeSelected().value?.let { cancelCode ->
            viewModel.getComments().value?.let { comments ->
                if (cancelCode.description != null) {
                    if (cancelCode.description != null) {
                        val statusChangeModel = StatusChangeModel()
                        statusChangeModel.actionTime = Date()
                        statusChangeModel.callId = orderId
                        statusChangeModel.codeId = cancelCode.cancelCodeId
                        statusChangeModel.comments = comments
                        statusChangeModel.statusCodeCode = callStatusCode
                        viewModel.cancelServiceCall(statusChangeModel)
                    }
                } else {
                    showMessageBox(
                        getString(R.string.required_title),
                        getString(R.string.select_cancel_code_message)
                    )
                }
            }
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}