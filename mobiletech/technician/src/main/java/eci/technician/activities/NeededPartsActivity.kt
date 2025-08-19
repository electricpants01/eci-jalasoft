package eci.technician.activities

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.allparts.AllPartsViewModel
import eci.technician.activities.allparts.NeededPartsFragment
import eci.technician.databinding.ActivityNeededPartsBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.models.data.UsedPart
import eci.technician.repository.PartsRepository
import eci.technician.tools.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NeededPartsActivity : BaseActivity() {
    private lateinit var binding: ActivityNeededPartsBinding
    private var orderId: Int = 0
    private var equipmentId: Int = 0
    private var orderStatus: String? = null
    private var usageStatusId = 2
    private var holdCodeId = 0
    private lateinit var neededPartsData: List<UsedPart>
    private lateinit var banner: TextView

    private val viewModelParts: AllPartsViewModel by viewModels()

    companion object {
        const val TAG = "NeededPartsActivity"
        const val EXCEPTION = "Exception logger"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNeededPartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        banner = binding.containerOffline.offlineTextView
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.needed_parts_title)
        orderId = intent.getIntExtra(Constants.EXTRA_ORDER_ID, 0)
        equipmentId = intent.getIntExtra(Constants.EXTRA_EQUIPMENT_ID, 0)
        orderStatus = intent.getStringExtra(Constants.EXTRA_ORDER_STATUS)
        holdCodeId = intent.getIntExtra(Constants.EXTRA_HOLD_CODE_ID, 0)
        viewModelParts.currentCallId = orderId
        viewModelParts.updateIsAddingPartsFroOnHold(true)
        viewModelParts.onHoldCodeIdForNeededParts = holdCodeId
        if (AppAuth.getInstance().isConnected) {
            banner.visibility = View.GONE
        } else {
            banner.setText(R.string.offline_no_internet_connection)
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOfflineDark))
            banner.visibility = View.VISIBLE
        }

        val connection = NetworkConnection(baseContext)

        connection.observe(this, { t ->
            t?.let {
                if (it) {
                    banner.setText(R.string.back_online)
                    banner.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnline))
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            //do nothing
                        }

                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                            if (AppAuth.getInstance().isConnected) {
                                banner.visibility = View.GONE
                            }
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                    banner.setText(R.string.offline_no_internet_connection)
                    banner.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.colorOfflineDark
                        )
                    )
                    banner.visibility = View.VISIBLE
                }
            }
        })

        binding.btnOk.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        binding.btnCancel.setOnClickListener {
            confirmCancelAction()
        }

        setUpNeededPartsFragment()
    }

    private fun setUpNeededPartsFragment() {
        supportFragmentManager.commit {
            replace(binding.containerNeededPartsFrameLayout.id, NeededPartsFragment())
        }
    }

    private fun confirmCancelAction() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (holdCodeId != 0) {
                PartsRepository.deleteNeededPartsAddedLocallyWithHolId(orderId, holdCodeId)
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        confirmCancelAction()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            confirmCancelAction()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
