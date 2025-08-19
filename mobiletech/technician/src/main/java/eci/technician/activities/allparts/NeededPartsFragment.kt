package eci.technician.activities.allparts

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.activities.requestNeededParts.PartsActivity
import eci.technician.databinding.FragmentNeededPartsBinding
import eci.technician.dialog.EditPartDescriptionDialog
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


class NeededPartsFragment : Fragment(), IPartsUpdate {

    private var _binding: FragmentNeededPartsBinding? = null
    val binding
        get() = _binding!!
    val viewModel: AllPartsViewModel by activityViewModels()
    var isAddingPartFromOnHold = false
    var isInIncompleteProcess = false
    var currentServiceCallStatus: ServiceOrderRepository.ServiceOrderStatus =
        ServiceOrderRepository.ServiceOrderStatus.PENDING


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNeededPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FBAnalyticsConstants.logEvent(requireContext(), FBAnalyticsConstants.NEEDED_PARTS_FRAGMENT)
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
                    binding.containerNeededPartsFragment.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setObservers()
    }

    private fun setObservers() {
        observeNeededParts()
        observeStepsFromCompleteProcess()
        observeOnHoldProcess()
        observeServiceCallStatus()
    }

    private fun observeServiceCallStatus() {
        viewModel.serviceCallStatus.observe(viewLifecycleOwner, {
            currentServiceCallStatus = it
            updateRecycler()
        })
    }

    private fun observeOnHoldProcess() {
        viewModel.isAddingNeededPartsForOnHold.observe(viewLifecycleOwner, {
            isAddingPartFromOnHold = it
            verifyAddPartsAvailability()
            updateRecycler()
        })
    }

    private fun updateRecycler() {
        observeNeededParts()
    }

    private fun observeStepsFromCompleteProcess() {
        viewModel.currentStep.observe(viewLifecycleOwner, {
            isInIncompleteProcess = it == 5
            verifyAddPartsAvailability()
            updateRecycler()
        })
    }

    private fun verifyAddPartsAvailability() {
        if (isAddingPartFromOnHold || isInIncompleteProcess) {
            binding.btnAddNeededPart.visibility = View.VISIBLE
        } else {
            binding.btnAddNeededPart.visibility = View.GONE
        }
    }

    private fun observeNeededParts() {
        when {
            isAddingPartFromOnHold -> {
                DatabaseRepository.getInstance()
                    .getNeededPartsByCallIdForHold(viewModel.currentCallId)
                    .observe(viewLifecycleOwner,
                        {
                            setNeededRecycler(it)
                            setDotVisibility(it)
                        })
            }
            isInIncompleteProcess -> {
                DatabaseRepository.getInstance()
                    .getNeededPartsByCallIdForIncomplete(viewModel.currentCallId)
                    .observe(viewLifecycleOwner,
                        {
                            setNeededRecycler(it)
                            setDotVisibility(it)
                        })
            }
            else -> {
                DatabaseRepository.getInstance().getNeededPartsByCallId(viewModel.currentCallId)
                    .observe(viewLifecycleOwner,
                        {
                            setNeededRecycler(it)
                            setDotVisibility(it)
                        })
            }
        }
    }

    private fun setDotVisibility(neededParts: List<UsedPart>) {
        if (neededParts.isEmpty()) {
            binding.redDot.visibility = View.GONE
        } else {
            if ((isAddingPartFromOnHold || isInIncompleteProcess)) {
                binding.redDot.visibility = View.GONE
            } else {
                binding.redDot.visibility = View.VISIBLE
            }
        }
    }

    private fun setNeededRecycler(neededParts: List<UsedPart>) {
        binding.neededRecycler.adapter =
            AllUsedPartsAdapter(
                neededParts,
                this,
                viewModel.customerWarehouseId,
                currentServiceCallStatus,
                isAddingPartFromOnHold,
                isAssist = viewModel.isAssist
            )
    }

    private fun setButtonListeners() {
        binding.btnAddNeededPart.setOnClickListener {
            val intent = Intent(requireContext(), PartsActivity::class.java)
            if (isAddingPartFromOnHold && viewModel.onHoldCodeIdForNeededParts != 0) {
                intent.putExtra(Constants.EXTRA_HOLD_CODE_ID, viewModel.onHoldCodeIdForNeededParts)
            }
            intent.putExtra(Constants.EXTRA_ORDER_ID, viewModel.currentCallId)
            intent.putExtra(Constants.SEARCH_FROM_NEEDED_PARTS, true)
            startActivity(intent)
        }
    }

    private fun setBasicRecycler() {
        binding.neededRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.neededRecycler.setHasFixedSize(true)
        binding.neededRecycler.adapter =
            AllUsedPartsAdapter(
                mutableListOf(),
                this,
                viewModel.customerWarehouseId,
                currentServiceCallStatus,
                isAddingPartFromOnHold,
                isAssist = viewModel.isAssist
            )
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun markAsPending(partCustomId: String) {
        // nothing to do
    }

    override fun markAsUsed(partCustomId: String) {
        // nothing to do
    }

    override fun deletePendingPart(partCustomId: String) {
        // nothing to do
    }

    override fun deleteUsedPart(partCustomId: String) {
        // nothing to do
    }

    override fun deleteNeededPart(partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val part = PartsRepository.getPartByCustomPartId(partCustomId) ?: return@launch

            when {
                part.isAddedLocally -> {
                    PartsRepository.deletePart(partCustomId)
                }
                part.isSent -> {
                    if (currentServiceCallStatus == ServiceOrderRepository.ServiceOrderStatus.ON_HOLD) return@launch
                    PartsRepository.deleteNeededPart(partCustomId)
                }

            }
        }
    }


    override fun onLongPress(partCustomId: String) {
        /**
         * Only allow edit the description in the ON_HOLD and INCOMPLETE process
         */
        if (isAddingPartFromOnHold || isInIncompleteProcess) {
            /**
             * Validate the Technician has the permission to UnknownItems
             */
            if (!AppAuth.getInstance().technicianUser.isAllowUnknownItems) return

            lifecycleScope.launch(Dispatchers.IO) {
                val part = PartsRepository.getPartByCustomPartId(partCustomId) ?: return@launch
                /**
                 * Validate the part is added locally (not sent)
                 */
                if (part.isSent) return@launch
                if (part.localUsageStatusId != UsedPart.NEEDED_STATUS_CODE) return@launch
                withContext(Dispatchers.Main) {
                    EditPartDescriptionDialog.showDialog(requireContext(), part.partDescription ?: "") {
                        saveDescription(it, partCustomId)
                    }
                }
            }
        }
    }

    fun saveDescription(description: String, partCustomId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val part = PartsRepository.getPartByCustomPartId(partCustomId) ?: return@launch
            part.localDescription = description
            part.partDescription = description
            part.actionType = if (part.isAddedLocally) "insert" else "update"
            PartsRepository.updatePart(part)
        }
    }

    suspend fun showConfirmationMessage(partCustomId: String) {
        withContext(Dispatchers.Main) {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setTitle(getString(R.string.permanentDeleteWarning))
                .setMessage(getString(R.string.eautomateDeleteWarning))
                .setPositiveButton(getString(R.string.remove)) { _, _ ->
                    deleteSentNeededPartOnline(partCustomId)
                }.setNegativeButton(getString(R.string.cancel)) { _, _ -> }.show()
        }
    }

    private fun deleteSentNeededPartOnline(partCustomId: String) {
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
                //review
            }
        }
    }

    fun showMessageOnError(title: String, description: String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(title)
        dialog.setMessage(description)
        dialog.show()
    }

}