package eci.technician.activities.searchWarehouses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.FragmentSearchWarehousesBinding
import eci.technician.interfaces.ISearchableList

class SearchWarehousesFragment : Fragment(), ISearchableList {

    private var _binding: FragmentSearchWarehousesBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: SearchWarehousesViewModel by activityViewModels()
    private var fragmentToast: Toast? = null
    lateinit var adapter: SearchWarehousePartsAdapter
    var dialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchWarehousesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearchView()
        setupEmptyList()
        binding.loaderIncluded.progressBarContainer.visibility = View.GONE
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchParts() }
        binding.btnSearch.setOnClickListener {
            viewModel.searchQuery = binding.txtPart.text.toString()
            viewModel.fetchParts(viewModel.searchQuery)
        }
        binding.btnFilter.setOnClickListener {
            viewModel.techSearchQuery = binding.txtFilter.text.toString()
            viewModel.filterListByTech(binding.txtFilter.text.toString())
            observeList()
        }
        binding.swipeRefresh.isEnabled = false
        setObservers()
    }

    override fun setupEmptyList() {
        binding.emptyListIncluded.mainText.text = getString(R.string.search_warehouses)
        binding.emptyListIncluded.secondaryText.text = getString(R.string.no_items_were_found)
    }

    override fun setupSearchView() {
        // do nothing
    }

    override fun setupRecycler() {
        adapter = SearchWarehousePartsAdapter(this)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recycler.adapter = adapter
    }

    override fun setObservers() {
        observeList()
        observeNetworkError()
        observeSwipe()
        observeToast()
        observeWarehouseList()
        observeShowCustomFilter()
        observeTechFilter()
    }

    private fun observeTechFilter() {
        viewModel.showTechFilter.observe(viewLifecycleOwner) { shouldShow ->
            binding.txtFilter.setText(viewModel.techSearchQuery)
            binding.layFilterText.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
    }

    private fun observeShowCustomFilter() {
        viewModel.showCustomFilter.observe(viewLifecycleOwner) { shouldShow ->
            binding.layFilter.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
    }

    private fun observeWarehouseList() {
        viewModel.warehouseList.observe(viewLifecycleOwner) {
            setWarehouseSpin(it.first, it.second)
        }
    }

    private fun setWarehouseSpin(selectedOption:String, warehouseList: List<String>) {
        val warehouseAdapter = WarehouseAdapter(warehouseList)
        binding.spinWarehouse.adapter = warehouseAdapter
        val selectedOptionValue = warehouseList.indexOf(selectedOption)
        if (selectedOptionValue >= 0){
            binding.spinWarehouse.setSelection(selectedOptionValue)
        }
        binding.spinWarehouse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //Not in use
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setFilterSelected(binding.spinWarehouse.selectedItem.toString())
            }
        }
    }


    override fun observeToast() {
        viewModel.toastMessage.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                showToast(getString(it))
            }
        })
    }

    override fun observeSwipe() {
        viewModel.swipeLoading.observe(viewLifecycleOwner, {
            binding.swipeRefresh.isRefreshing = it
        })
    }

    override fun observeNetworkError() {
        viewModel.networkError.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                requireActivity().let { pActivity ->
                    if (pActivity is BaseActivity) {
                        pActivity.showNetworkErrorDialog(it, requireContext(), childFragmentManager)
                    }
                }
            }
        })
    }


    private fun showToast(text: String) {
        fragmentToast?.setText(text) ?: kotlin.run {
            fragmentToast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            fragmentToast?.show()
        }
        fragmentToast = null
    }

    override fun observeList() {
        viewModel.warehouseItemsList.observe(viewLifecycleOwner, {
            adapter.setOriginalListFirstTime(it)
            adapter.filter.filter(viewModel.searchQuery)
        })
    }

    override fun onEmptyList(isEmpty: Boolean) {
        binding.recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyListIncluded.emptyListContainer.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}