package eci.technician.activities.transfers

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.FragmentSelectWarehouseBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.DialogHelperManager
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.models.transfers.Warehouse

class SelectWarehouseFragment : Fragment(), WarehouseSearchAdapter.IWarehouseSearchListener {
    private lateinit var binding: FragmentSelectWarehouseBinding
    private lateinit var itemsListAdapter: WarehouseSearchAdapter
    private val viewModel: TransferViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_warehouse, container, false)
        binding.lifecycleOwner = this
        binding.transferViewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.select_warehouse)
        viewModel.getWarehouses()
        setSearchViewColors()
        listenErrors()
        initList()
    }

    private fun listenErrors() {
        viewModel.networkError.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let { error ->
                requireActivity().let { pActivity ->
                    if (pActivity is BaseActivity){
                        pActivity.showNetworkErrorDialog(error, requireContext(), childFragmentManager){
                            goBack()
                        }
                    }
                }
            }
        })
    }

    private fun handleError(pair: Pair<ErrorType, String?>?) {
        if (pair == null) {
            DialogHelperManager.displayOkMessage(
                getString(R.string.somethingWentWrong),
                getString(R.string.error),
                getString(R.string.ok),
                requireContext()
            ) {
                goBack()
            }
            return
        }
        when (pair.first) {
            ErrorType.SOMETHING_WENT_WRONG,
            ErrorType.NOT_SUCCESSFUL,
            ErrorType.HTTP_EXCEPTION,
            ErrorType.IO_EXCEPTION,
            ErrorType.CONNECTION_EXCEPTION,
            ErrorType.BACKEND_ERROR,
            ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                DialogHelperManager.displayOkMessage(
                    getString(R.string.somethingWentWrong),
                    pair.second ?: getString(R.string.error),
                    getString(R.string.ok),
                    requireContext()
                ) {
                    goBack()
                }
            }
        }
    }

    private fun setupEmptyMessage(empty: Boolean) {
        binding.apply {
            emptyView.title = "Warehouse"
            emptyView.message = "There are no warehouses."
            binding.emptyView.isVisible = empty && viewModel.isLoading.value == false
        }
    }

    private fun setSearchTextListener() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                        itemsListAdapter.filter.filter(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                        itemsListAdapter.filter.filter(it)
                }
                return false
            }
        })
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

    @Synchronized
    private fun initList() {
        viewModel.filteredWarehouseList.observe(viewLifecycleOwner, { filteredWarehouseList ->
            itemsListAdapter = WarehouseSearchAdapter(
                this
            )
            itemsListAdapter.setOriginalListFirsTime(filteredWarehouseList)
            itemsListAdapter.filter.filter(null)
            binding.listWarehouses.adapter = itemsListAdapter
            setSearchTextListener()
        })
    }


    override fun onWarehouseSearchPressed(currentWarehouse: Warehouse) {
        val techWarehouseId = AppAuth.getInstance().technicianUser.warehouseId
        if (viewModel.selectingSource) {
            viewModel.isResettingWarehouse = true
            viewModel.setSourceWarehouse(currentWarehouse)
            view?.findNavController()
                ?.navigate(R.id.action_selectWarehouseFragment_to_selectPartFragment)
            viewModel.setSourcePart(null)
            viewModel.setSourceSelectedBin(null)
            viewModel.setAvailableQuantity(0.0)
            viewModel.setSourceUsedQuantity(0.0)

            if (viewModel.destinationSelectedWarehouse.value?.warehouseTypeID == currentWarehouse.warehouseTypeID ||
                viewModel.destinationSelectedWarehouse.value?.warehouseID == currentWarehouse.warehouseID
            ) {
                viewModel.setDestinationWarehouse(null)
                viewModel.setDestinationSelectedBin(null)
            }
            if ((currentWarehouse.warehouseTypeID == Warehouse.COMPANY_TYPE || currentWarehouse.warehouseTypeID == Warehouse.CUSTOMER_TYPE)
                && currentWarehouse.warehouseID != techWarehouseId
            ) {
                viewModel.setDestinationWarehouse(viewModel.techWarehouse)
                viewModel.destinationSelectedWarehouse.value?.let {
                    if (it.bins.size == 1) {
                        viewModel.setDestinationSelectedBin(it.bins[0])
                    }
                }
            }
            if (viewModel.transferType == WarehouseType.CUSTOMER_TYPE.value) {
                setWarehousesForCustomerTransfer()
            }
        } else {
            viewModel.setDestinationWarehouse(currentWarehouse)
            if (currentWarehouse.bins.size == 1) {
                viewModel.setDestinationSelectedBin(currentWarehouse.bins[0])
                goBack()
            } else {
                var defaultBin = viewModel.getDefaultBin(currentWarehouse.bins,false)
                if(defaultBin != null){
                    viewModel.setDestinationSelectedBin(defaultBin)
                    goBack()
                }
                else {
                    view?.findNavController()
                        ?.navigate(R.id.action_selectWarehouseFragment_to_selectBinFragment)
                    viewModel.setDestinationSelectedBin(null)
                }
            }
        }
    }

    private fun setWarehousesForCustomerTransfer() {
        if (viewModel.sourceSelectWarehouse.value != null) { // we refresh warehouses with default based on source part
            viewModel.getWarehouses()
        }
        viewModel.filteredWarehouseList.observe(viewLifecycleOwner, { filteredWarehouseList ->
            val filteredList =
                filteredWarehouseList.filter { warehouse -> warehouse.warehouseID != viewModel.sourceSelectWarehouse.value?.warehouseID }
            if (filteredList?.size ?: 0 > 0) {
                viewModel.setDestinationWarehouse(filteredList?.get(0))
            }
            viewModel.destinationSelectedWarehouse.value?.let {
                if (it.bins.size == 1) {
                    viewModel.setDestinationSelectedBin(it.bins[0])
                }
            }
        })
    }

    private fun goBack() {
        view?.findNavController()
            ?.navigate(R.id.action_selectWarehouseFragment_to_transferFragment)
    }

    override fun onFilteredAction(empty:Boolean) {
        setupEmptyMessage(empty)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setWarehouseList(listOf())
    }

}

