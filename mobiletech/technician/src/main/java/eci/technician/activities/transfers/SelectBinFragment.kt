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
import eci.technician.databinding.FragmentSelectBinBinding
import eci.technician.models.transfers.Bin


class SelectBinFragment : Fragment(), BinSearchAdapter.IBinSearchListener {
    private lateinit var binding: FragmentSelectBinBinding
    private lateinit var itemsListAdapter: BinSearchAdapter
    private val viewModel: TransferViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectBinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.select_bin)
        showProgressBar()
        setSearchViewColors()
        initList()
    }

    private fun setupEmptyMessage() {
        binding.apply {
            emptyView.title = "Bin"
            emptyView.message = "There are no bins."
            emptyView.isVisible = listBin.adapter?.itemCount ?: 0 == 0
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
        hideProgressBar()
        var binList: List<Bin> = listOf()
        if (viewModel.selectingSource) {
            viewModel.sourceSelectedPart.value?.bins?.let {
                binList = it
                binList = binList.filter { bin -> bin.binAvailableQty > 0 }
            }
        } else {
            viewModel.destinationSelectedWarehouse.value?.bins?.let {
                binList = it
            }
        }
        var warehouseId = 0
        var partId = 0
        if (viewModel.selectingSource){
            warehouseId = viewModel.sourceSelectWarehouse?.value?.warehouseID ?:0
            partId = viewModel.sourceSelectedPart.value?.itemId?:0
            binList = viewModel.updateListBinQuantity(binList,partId,warehouseId)
        }
        itemsListAdapter = BinSearchAdapter(viewModel.getSortedBinList(binList), this)
        binding.listBin.layoutManager = LinearLayoutManager(activity);
        binding.listBin.adapter = itemsListAdapter
        itemsListAdapter.notifyDataSetChanged()
        setSearchTextListener()
        setupEmptyMessage()
    }

    private fun showProgressBar() {
        binding.progressBarContainer.isVisible = true
        binding.listBin.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.isVisible = false
        binding.listBin.visibility = View.VISIBLE
    }

    override fun onBinSearchPressed(bin: Bin) {
        if (viewModel.selectingSource) {
            viewModel.setSourceSelectedBin(bin)
            viewModel.setSourceUsedQuantity(1.0)
            viewModel.setAvailableQuantity(bin.updatedBinQty)
            viewModel.setFocusToQuantity = true
        } else {
            viewModel.setDestinationSelectedBin(bin)
        }
        view?.findNavController()?.navigate(R.id.action_selectBinFragment_to_transferFragment)
    }
    override fun onFilteredAction() {
        setupEmptyMessage()
    }
}