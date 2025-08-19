package eci.technician.activities.allparts

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.activities.addParts.AddPartsActivity
import eci.technician.databinding.FragmentPendingPartsBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.data.UsedPart
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.tools.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PendingPartsFragment : Fragment(), IPartsUpdate {

    private var _binding: FragmentPendingPartsBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: AllPartsViewModel by activityViewModels()
    var currentServiceCallStatus: ServiceOrderRepository.ServiceOrderStatus =
        ServiceOrderRepository.ServiceOrderStatus.PENDING

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.PENDING_PARTS_FRAGMENT)
        _binding = FragmentPendingPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialView()
        setBasicRecycler()
        setButtonListeners()
        setObservers()
    }

    private fun setInitialView() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isAssist = ServiceOrderRepository.isServiceOrderAssist(viewModel.currentCallId)
            val customerWarehouseId = ServiceOrderRepository.getServiceOrderByCallId(viewModel.currentCallId)?.customerWarehouseId ?: 0

            viewModel.isAssist = isAssist
            viewModel.customerWarehouseId = customerWarehouseId
            if (isAssist) {
                withContext(Dispatchers.Main) {
                    binding.containerPendingPartsFragment.visibility = View.GONE
                    binding.btnAddPendingPart.visibility = View.GONE
                }
            }
        }
    }

    private fun setBasicRecycler() {
        binding.pendingRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.pendingRecycler.setHasFixedSize(true)
        binding.pendingRecycler.adapter =
            AllUsedPartsAdapter(
                mutableListOf(),
                this,
                viewModel.customerWarehouseId,
                currentServiceCallStatus,
                isInHoldProcess = false,
                isAssist = viewModel.isAssist
            )
    }

    private fun setObservers() {
        observePendingParts()
        observeServiceCallStatus()
    }

    private fun observeServiceCallStatus() {
        viewModel.serviceCallStatus.observe(viewLifecycleOwner, {
            currentServiceCallStatus = it
            when (it) {
                ServiceOrderRepository.ServiceOrderStatus.PENDING,
                ServiceOrderRepository.ServiceOrderStatus.SCHEDULED,
                ServiceOrderRepository.ServiceOrderStatus.DISPATCHED -> {
                    binding.btnAddPendingPart.visibility = View.VISIBLE
                }
                else -> {
                    binding.btnAddPendingPart.visibility = View.GONE
                }
            }
            observePendingParts()
        })
    }

    private fun setButtonListeners() {
        binding.btnAddPendingPart.setOnClickListener {
            addPendingPart()
        }
    }

    override fun onResume() {
        super.onResume()
        setObservers()
    }

    private fun observePendingParts() {
        DatabaseRepository.getInstance().getPendingPartsByCallId(viewModel.currentCallId)
            .observe(viewLifecycleOwner, {
                setPendingRecycler(it)
            })
    }

    private fun setPendingRecycler(pendingParts: List<UsedPart>) {
        binding.pendingRecycler.adapter =
            AllUsedPartsAdapter(
                pendingParts,
                this,
                viewModel.customerWarehouseId,
                currentServiceCallStatus,
                isInHoldProcess = false,
                isAssist = viewModel.isAssist
            )

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun addPendingPart() {
        val intent = Intent(requireContext(), AddPartsActivity::class.java)
        intent.putExtra(Constants.EXTRA_ORDER_ID, viewModel.currentCallId)
        intent.putExtra(AddPartsActivity.EXTRA_PENDING_PART, true)
        intent.putExtra(AddPartsActivity.EXTRA_CUSTOMER_WAREHOUSE_ID, viewModel.customerWarehouseId)
        startActivity(intent)
    }

    override fun markAsPending(partCustomId: String) {
        // nothing to do
    }

    override fun markAsUsed(partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            PartsRepository.markPartAsUsed(partCustomId)
        }
    }

    override fun deletePendingPart(partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            PartsRepository.deletePendingPart(partCustomId)
        }
    }

    override fun deleteUsedPart(partCustomId: String) {
        //nothing to do
    }

    override fun deleteNeededPart(partCustomId: String) {
        //nothing to do
    }

    override fun onLongPress(partCustomId: String) {
        // nothing to do
    }

}