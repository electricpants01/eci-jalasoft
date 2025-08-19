package eci.technician.activities.transfers

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.databinding.FragmentSelectPartBinding
import eci.technician.helpers.DialogHelperManager
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.models.ProcessingResult
import eci.technician.models.transfers.Part
import eci.technician.repository.PartsRepository
import eci.technician.tools.Settings


class SelectPartFragment : Fragment(), ITransferPartClickedInterface {

    private lateinit var binding: FragmentSelectPartBinding
    private lateinit var itemsListAdapter: AddTransferPartsAdapter
    private val viewModel: TransferViewModel by activityViewModels()
    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectPartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.select_part)
        showProgressBar()
        setSearchViewColors()
        initList()
    }
    
    private fun setupEmptyMessage() {
        binding.apply {
            emptyView.title = "Part"
            emptyView.message = "There are no parts."
            emptyView.isVisible = listParts.adapter?.itemCount ?: 0 == 0
        }
    }

    private fun setSearchTextListener() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotEmpty()) {
                        itemsListAdapter.filterListByQuery(it)
                    } else {
                        itemsListAdapter.resetList()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.isNotEmpty()) {
                        itemsListAdapter.filterListByQuery(it)
                    } else {
                        itemsListAdapter.resetList()
                    }
                }
                return false
            }
        })
    }

    private fun showProgressBar() {
        binding.progressBarContainer.isVisible = true
        binding.listParts.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.isVisible = false
        binding.listParts.visibility = View.VISIBLE
    }


    private fun setSearchViewColors() {
        val searchTextView: TextView =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text)
        val closeButton: ImageView =
            binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setColorFilter(Color.WHITE)
        searchTextView.setTextColor(Color.WHITE)
        searchTextView.setHintTextColor(Color.DKGRAY)
    }

    private fun initList() {
        viewModel.sourceSelectWarehouse?.value?.warehouseID?.let {
            PartsRepository.getWarehousePartsById(it, savePartInDB = false)
                .observe(
                    viewLifecycleOwner, { genericDataResponse ->
                        hideProgressBar()
                        when (genericDataResponse.responseType) {
                            RequestStatus.SUCCESS -> {
                                if (genericDataResponse.data?.isHasError == true) {
                                    val error = genericDataResponse.data.errors[0]
                                    DialogHelperManager.displayOkMessage(
                                        genericDataResponse.onError?.title
                                            ?: getString(R.string.somethingWentWrong),
                                        error.errorText
                                            ?: getString(R.string.error),
                                        getString(R.string.ok),
                                        requireContext()
                                    ) {}
                                } else {
                                    var partsList =
                                        genericDataResponse.data?.let { parseParts(it) }
                                    partsList?.let {
                                        var updatedList = viewModel.filterOnlyPositiveAvailable(
                                            viewModel.updateListPartQuantity(it)
                                        )
                                        itemsListAdapter = AddTransferPartsAdapter(updatedList, this)
                                        binding.listParts.layoutManager =
                                            LinearLayoutManager(activity);
                                        binding.listParts.adapter = itemsListAdapter
                                        itemsListAdapter.notifyDataSetChanged()
                                    }

                                }
                                setSearchTextListener()
                            }
                            RequestStatus.ERROR -> {
                                DialogHelperManager.displayOkMessage(
                                    genericDataResponse.onError?.title
                                        ?: getString(R.string.somethingWentWrong),
                                    genericDataResponse.onError?.description
                                        ?: getString(R.string.error),
                                    getString(R.string.ok),
                                    requireContext()
                                ) {}

                            }
                            else -> {
                                DialogHelperManager.displayOkMessage(
                                    getString(R.string.error_code),
                                    getString(R.string.somethingWentWrong),
                                    getString(R.string.ok),
                                    requireContext()
                                ) {}
                            }
                        }
                        setupEmptyMessage()

                    }
                )
        }
    }

    private fun parseParts(processingResult: ProcessingResult): List<Part> {
        processingResult?.result?.let {
            return Settings.createGson()
                .fromJson(
                    it, Array<Part>::class.java
                ).toList()
        }
        return listOf()
    }


    override fun onTapPart(part: Part) {
        viewModel.setSourcePart(part)
        if (viewModel.selectingSource) {
            viewModel.setSourceSelectedBin(null)
            viewModel.setSourceUsedQuantity(0.0)
            viewModel.setAvailableQuantity(0.0)
            var warehouseId = 0
            warehouseId = viewModel.sourceSelectWarehouse?.value?.warehouseID ?: 0
            setDefaultDestinationBin()
            part.bins = part.bins.filter { bin -> bin.binAvailableQty > 0 }
            if (part.bins.size == 1) {
                viewModel.setSourceSelectedBin(part.bins[0])
                viewModel.setSourceUsedQuantity(1.0)
                part.bins = viewModel.updateListBinQuantity(part.bins, part.itemId, warehouseId)
                viewModel.setAvailableQuantity(part.bins[0].updatedBinQty)
                viewModel.setFocusToQuantity = true
                goBack()
            } else {
                val defaultBin = viewModel.getDefaultBin(part.bins,true)
                if(defaultBin != null && defaultBin.serialNumber.isNullOrBlank()) {
                    viewModel.setSourceSelectedBin(defaultBin)
                    viewModel.setFocusToQuantity = true
                    viewModel.setSourceUsedQuantity(1.0)
                    viewModel.setAvailableQuantity(defaultBin.binAvailableQty)
                    goBack()
                } else {
                    view?.findNavController()
                        ?.navigate(R.id.action_selectPartFragment_to_selectBinFragment)
                }
            }
        } else {
            view?.findNavController()
                ?.navigate(R.id.action_selectPartFragment_to_transferFragment)
        }
    }
    private fun setDefaultDestinationBin() {
        if (viewModel.destinationSelectedWarehouse.value != null)
            viewModel.getWarehouses()
        viewModel.filteredWarehouseList.observe(viewLifecycleOwner, {
            viewModel.setDefaultDestinationBin()
        })
    }

    private fun goBack(){
        view?.findNavController()
            ?.navigate(R.id.action_selectPartFragment_to_transferFragment)
    }

    override fun onFilteredAction() {
        setupEmptyMessage()
    }
}
