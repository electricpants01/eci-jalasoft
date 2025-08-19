package eci.technician.activities.transfers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import eci.technician.R
import eci.technician.databinding.FragmentTransferBinding
import eci.technician.helpers.DialogHelperManager
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository


class TransferFragment : Fragment() {
    private lateinit var binding: FragmentTransferBinding
    private val viewModel: TransferViewModel by activityViewModels()
    private lateinit var imm: InputMethodManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_transfer, container, false)
        binding.transferViewModel = viewModel
        binding.lifecycleOwner= this
        imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setLoading(true)
        setClickListeners()
        setHasOptionsMenu(true)
        setQuantityKeyListener()
        setTransferClickListener()
        observeAvailablePartsResponse()
        observeAvailablePartsLoader()
    }

    private fun observeAvailablePartsLoader() {
        viewModel.availablePartsLoader.observe(viewLifecycleOwner){ showLoader ->
            if (showLoader){
                showProgressBar()
            }else {
                hideProgressBar()
            }
        }
    }

    private fun observeAvailablePartsResponse() {
        viewModel.availablePartsResponseEvent.observe(viewLifecycleOwner){
            it.getContentIfNotHandledOrReturnNull()?.let { event ->
                if (event){
                    DialogHelperManager.displayOkMessage(
                        getString(R.string.transfer),
                        getString(R.string.successful_transfer),
                        getString(R.string.ok),
                        requireContext()
                    ) {
                        viewModel.resetAll()
                    }
                }
            }
        }
    }


    private fun showProgressBar() {
        binding.progressBarContainer.isVisible = true
        binding.scrollView.visibility = View.GONE
        binding.btnTransfer.visibility =View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.isVisible = false
        binding.scrollView.visibility = View.VISIBLE
        binding.btnTransfer.visibility = View.VISIBLE
    }


    private fun setTransferClickListener() {
        binding.btnTransfer.setOnClickListener {
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .createNewPartTranster(viewModel.getCreateTransferModel()).observe(
                    viewLifecycleOwner, { genericDataResponse ->
                        when (genericDataResponse.responseType) {
                            RequestStatus.SUCCESS -> {
                                if (genericDataResponse.data?.isHasError == true) {
                                    val error = genericDataResponse.data.errors[0]
                                    displayDialog(
                                        getString(R.string.somethingWentWrong) ,
                                        error.errorText + getString(R.string.server_error),
                                    )
                                } else {
                                    viewModel.getAvailablePartsForTech()
                                }
                            }
                            else -> {
                                displayDialog(
                                    getString(R.string.error_code),
                                    getString(R.string.somethingWentWrong)
                                )
                            }
                        }

                    }
                )
        }
    }

    private fun displayDialog(title: String, description: String) {
        DialogHelperManager.displayOkMessage(
            title,
            description,
            getString(R.string.ok),
            requireContext()
        ) {
            viewModel.resetAll()
        }
    }

    private fun setQuantityKeyListener() {
        binding.sourceQuantity.txtQuantity.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                viewModel.setSourceUsedQuantity(0.0)
                binding.btnTransfer.isEnabled = false
            } else {
                if(viewModel.isResettingWarehouse){
                    viewModel.isResettingWarehouse = false
                    return@doOnTextChanged
                }
                if(viewModel.sourceSelectBin.value != null)
                    viewModel.setSourceUsedQuantity(text.toString().toDouble())
                if (!isSelectedQuantityCorrect())
                    return@doOnTextChanged
                if (viewModel.destinationSelectedBin.value != null && viewModel.sourceUsedQuantity.value?:0.0 > 0.0)
                    binding.btnTransfer.isEnabled = true
            }
        }
    }

    private fun isSelectedQuantityCorrect(): Boolean {
        if (viewModel.sourceUsedQuantity?.value?:0.0 > viewModel.sourceAvailableQuantity.value!! && viewModel.sourceSelectBin != null) {
            binding.sourceQuantity.txtQuantity.error = getString(
                R.string.transfer_quantity_error_message,
                viewModel.sourceAvailableQuantity.value?.toInt()
            )
            binding.btnTransfer.isEnabled = false
            return false
        }
        return true
    }

    private fun setClickListeners() {
        binding.apply {
            viewModel.selectingSource = true
            sourceWarehouse.selectorCard.setOnClickListener {
                Navigation.findNavController(binding.root)
                    .navigate(R.id.action_transferFragment_to_selectWarehouseFragment)
            }
            sourcePart.selectorCard.setOnClickListener {
                Navigation.findNavController(binding.root)
                    .navigate(R.id.action_transferFragment_to_selectPartFragment)
            }
            sourceBin.selectorCard.setOnClickListener {
                Navigation.findNavController(binding.root)
                    .navigate(R.id.action_transferFragment_to_selectBinFragment)
            }

            destinationWarehouse.selectorCard.setOnClickListener {
                viewModel.selectingSource = false
                view?.findNavController()
                    ?.navigate(R.id.action_transferFragment_to_selectWarehouseFragment)
            }
            destinationBin.selectorCard.setOnClickListener {
                viewModel.selectingSource = false
                view?.findNavController()
                    ?.navigate(R.id.action_transferFragment_to_selectBinFragment)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        bindValues()
    }

    private fun bindValues() {
        viewModel.destinationSelectedBin?.let {
            binding.destinationBin.cardContent = it.value?.bin
            if (viewModel.sourceUsedQuantity?.value?:0.0 > 0.0 && isSelectedQuantityCorrect()) {
                binding.btnTransfer.isEnabled = true
            }
        }
        if (viewModel.setFocusToQuantity) {
            if(viewModel.sourceSelectBin.value?.serialNumber.isNullOrBlank()){
                binding.sourceQuantity.txtQuantity.isEnabled = true
                binding.sourceQuantity.txtQuantity.requestFocus()
                imm.showSoftInput(binding.sourceQuantity.txtQuantity, InputMethodManager.SHOW_IMPLICIT)
            }
            viewModel.setFocusToQuantity = false
            val posText = viewModel.sourceUsedQuantity.value?.toInt().toString().length
            binding.sourceQuantity.txtQuantity.setText(viewModel.sourceUsedQuantity.value?.toInt().toString())
            binding.sourceQuantity.txtQuantity.setSelection(posText)
            binding.sourceQuantity.txtQuantity.selectAll()
        }
        binding.sourceQuantity.isEnabled = viewModel.sourceSelectBin.value != null && viewModel.sourceSelectBin.value?.serialNumber.isNullOrBlank()
        binding.btnTransfer.isEnabled = viewModel.destinationSelectedBin.value != null && viewModel.sourceUsedQuantity.value?:0.0 > 0.0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}