package eci.technician.activities.allparts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.addParts.AddPartsActivity
import eci.technician.databinding.FragmentUsedPartsBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.ProcessingResult
import eci.technician.models.data.UsedPart
import eci.technician.models.parts.postModels.PartToDeletePostModel
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.PartsRepository
import eci.technician.repository.ServiceOrderRepository
import eci.technician.tools.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class UsedPartsFragment : Fragment(), IPartsUpdate {

    private var _binding: FragmentUsedPartsBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: AllPartsViewModel by activityViewModels()
    var currentServiceCallStatus: ServiceOrderRepository.ServiceOrderStatus =
        ServiceOrderRepository.ServiceOrderStatus.PENDING

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.USED_PARTS_FRAGMENT)
        _binding = FragmentUsedPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialValue()
        setButtonsListeners()
        setBasicRecycler()
        setObservers()
    }

    private fun setInitialValue() {
        lifecycleScope.launch(Dispatchers.IO) {
            val customerWarehouseId =
                ServiceOrderRepository.getServiceOrderByCallId(viewModel.currentCallId)?.customerWarehouseId
                    ?: 0
            viewModel.customerWarehouseId = customerWarehouseId
        }
    }


    override fun onResume() {
        super.onResume()
        setObservers()
    }

    private fun setObservers() {
        observeUsedParts()
        observeServiceCallStatus()
    }

    private fun observeServiceCallStatus() {
        viewModel.serviceCallStatus.observe(viewLifecycleOwner, {
            currentServiceCallStatus = it
            when (it) {
                ServiceOrderRepository.ServiceOrderStatus.ARRIVED -> {
                    binding.btnAddUsedPart.visibility = View.VISIBLE
                }
                else -> {
                    binding.btnAddUsedPart.visibility = View.GONE
                }
            }
            observeUsedParts()
        })
    }

    private fun observeUsedParts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isAssist = ServiceOrderRepository.isServiceOrderAssist(viewModel.currentCallId)
            withContext(Dispatchers.Main) {
                viewModel.isAssist = isAssist
                if (isAssist) {
                    DatabaseRepository.getInstance().getUsedPartsForAssist(viewModel.currentCallId)
                        .observe(viewLifecycleOwner,
                            {
                                updatePartsForAssist(it ?: listOf())
                            })
                } else {
                    DatabaseRepository.getInstance().getUsedPartsByCallId(viewModel.currentCallId)
                        .observe(viewLifecycleOwner, {
                            setUsedRecycler(it)
                        })
                }
            }
        }
    }

    private fun updatePartsForAssist(parts: List<UsedPart>) {
        val filtered = parts
            .filter {
                it.isFromTechWarehouse(viewModel.customerWarehouseId) ||
                        it.isFromTechWarehouse(
                            viewModel.currentTechWarehouseId
                        )
            }
        setUsedRecycler(filtered)
    }

    private fun setUsedRecycler(usedParts: List<UsedPart>) {
        binding.usedRecycler.adapter =
            AllUsedPartsAdapter(
                usedParts,
                this,
                viewModel.customerWarehouseId,
                currentServiceCallStatus,
                isInHoldProcess = false,
                isAssist = viewModel.isAssist
            )
    }


    private fun setBasicRecycler() {
        binding.usedRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.usedRecycler.setHasFixedSize(true)
        binding.usedRecycler.adapter =
            AllUsedPartsAdapter(
                mutableListOf(),
                this,
                viewModel.customerWarehouseId,
                currentServiceCallStatus,
                isInHoldProcess = false,
                isAssist = viewModel.isAssist
            )

    }

    private fun setButtonsListeners() {
        binding.btnAddUsedPart.setOnClickListener {
            addUsedPart()
        }
    }

    private fun addUsedPart() {
        val intent = Intent(requireContext(), AddPartsActivity::class.java)
        intent.putExtra(Constants.EXTRA_ORDER_ID, viewModel.currentCallId)
        intent.putExtra(AddPartsActivity.EXTRA_PENDING_PART, false)
        intent.putExtra(AddPartsActivity.EXTRA_CUSTOMER_WAREHOUSE_ID, viewModel.customerWarehouseId)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun showNotFromMyWarehouseMessage() {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.warning),
                getString(R.string.itemAnotherWarehouse)
            )
        }
    }

    override fun markAsPending(partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            PartsRepository.markPartAsPending(partCustomId)
        }
    }

    override fun markAsUsed(partCustomId: String) {
        // nothing to do
    }

    override fun deletePendingPart(partCustomId: String) {
        // nothing to do
    }

    override fun deleteUsedPart(partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val part = PartsRepository.getPartByCustomPartId(partCustomId) ?: return@launch
            when {
                part.isAddedLocally -> {
                    PartsRepository.deletePart(partCustomId)
                }
                part.warehouseID != AppAuth.getInstance().technicianUser.warehouseId -> {
                    withContext(Dispatchers.Main) {
                        showNotFromMyWarehouseMessage()
                    }
                }
                part.isSent -> {
                    PartsRepository.deleteSentUsedPartLocally(partCustomId)
                }
            }
        }
    }

    suspend fun showConfirmationMessage(partCustomId: String) {
        withContext(Dispatchers.Main) {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setTitle(getString(R.string.permanentDeleteWarning))
                .setMessage(getString(R.string.eautomateDeleteWarning))
                .setPositiveButton(getString(R.string.remove)) { _, _ ->
                    deleteSentUsedPartOnline(partCustomId)
                }.setNegativeButton(getString(R.string.cancel)) { _, _ -> }.show()
        }
    }

    private fun deleteSentUsedPartOnline(partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val part = PartsRepository.getPartByCustomPartId(partCustomId) ?: return@launch
            val listOfPartsToDelete: MutableList<PartToDeletePostModel> =
                mutableListOf(PartToDeletePostModel.createInstanceWith(part))
            withContext(Dispatchers.Main) {
                RetrofitRepository.RetrofitRepositoryObject.getInstance()
                    .deletePartsFromEa(listOfPartsToDelete, mutableListOf(part))
                    .observe(viewLifecycleOwner, {
                        manageGenericResponse(it)
                    })
            }

        }

    }

    private fun manageGenericResponse(it: GenericDataResponse<ProcessingResult>?) {
        if (it == null) return
        when (it.responseType) {
            RequestStatus.SUCCESS -> {
                // do nothing
            }
            RequestStatus.ERROR -> {
                showMessageOnError(
                    it.onError?.title ?: getString(R.string.somethingWentWrong),
                    it.onError?.description ?: getString(R.string.error)
                )
                it.onError?.title
            }
            else -> {
                // review
            }
        }
    }

    fun showMessageOnError(title: String, description: String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(title)
        dialog.setMessage(description)
        dialog.show()
    }

    override fun deleteNeededPart(partCustomId: String) {
        //noting to do
    }

    override fun onLongPress(partCustomId: String) {
        //nothing to do
    }
}