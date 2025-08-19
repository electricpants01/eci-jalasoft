package eci.technician.activities.allparts

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commit
import eci.technician.BaseActivity
import eci.technician.databinding.ActivityPartsManagerBinding
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.models.order.ServiceOrder
import eci.technician.repository.DatabaseRepository
import io.realm.RealmResults

class PartsManagerActivity : BaseActivity(), IAllPartsNavigation {

    companion object {
        const val CALL_ID = "call_id"
        const val EXTRA_IS_ASSIST = "extra_is_assist"
    }

    private lateinit var binding: ActivityPartsManagerBinding
    val viewModel: AllPartsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPartsManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.currentCallId = intent.getIntExtra(CALL_ID, 0)
        viewModel.updatePartsByCallId()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            goToAllParts()
        }
        setMode()
        setObservers()
    }

    private fun setObservers() {
        observeOneServiceCall()
    }

    private fun observeOneServiceCall() {
        DatabaseRepository.getInstance().getServiceOrderLiveDataByNumberId(viewModel.currentCallId)
            .observe(this) { serviceOrders: RealmResults<ServiceOrder?> ->
                if (!serviceOrders.isEmpty()) {
                    val firstServiceOrder = serviceOrders.first()
                    if (firstServiceOrder != null && firstServiceOrder.isValid) {
                        viewModel.updateServiceOrderStatus(firstServiceOrder.callNumber_ID)
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        setObservers()
    }

    private fun setMode() {
        viewModel.allowChangePartStatus = false
    }

    override fun goToAllParts() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle()
            replace(binding.fragmentToReplace56.id, AllPartsFragment::class.java, bundle)
        }
    }


}