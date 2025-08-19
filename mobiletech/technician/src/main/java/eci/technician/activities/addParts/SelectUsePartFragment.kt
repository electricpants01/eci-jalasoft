package eci.technician.activities.addParts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.SearchableListBinding
import eci.technician.dialog.DialogManager
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.IPartClickedInterface
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.TechnicianWarehousePart
import eci.technician.repository.PartsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SelectUsePartFragment : Fragment(), ISearchableList, IPartClickedInterface {

    private var _binding: SearchableListBinding? = null
    private val binding
        get() = _binding!!
    val viewModel: AddPartViewModel by activityViewModels()
    lateinit var adapter: AddPartsAdapter
    private var fragmentToast: Toast? = null

    private val searchListenerObj =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchQuery = newText
                observeList()
                return true
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SearchableListBinding
            .inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearchView()
        setupEmptyList()
        binding.swipeRefresh.isEnabled = true
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchTechnicianWarehouseParts(true) }
        binding.loaderIncluded.progressBarContainer.visibility = View.GONE
        observeLoading()
    }

    private fun observeLoading() {
        viewModel.loadingParts.observe(viewLifecycleOwner){ showLoading ->
            binding.swipeRefresh.isRefreshing = showLoading
        }
    }

    override fun setupRecycler() {
        adapter = AddPartsAdapter(this, this, viewModel.currentTechWarehouse)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        viewModel.getTechnicianWarehousePartsFromDB()
        setObservers()
    }


    override fun setupSearchView() {
        binding.searchbarIncluded.searchview2.setOnQueryTextListener(searchListenerObj)
        setSearchViewColors()
    }

    private fun setSearchViewColors() {
        val searchTextView: TextView =
            binding.searchbarIncluded.searchview2.findViewById(androidx.appcompat.R.id.search_src_text)
        val closeButton: ImageView =
            binding.searchbarIncluded.searchview2.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setColorFilter(Color.WHITE)
        searchTextView.setTextColor(Color.WHITE)
        searchTextView.setHintTextColor(Color.DKGRAY)
    }

    override fun setupEmptyList() {
        binding.emptyListIncluded.emptyListContainer.visibility = View.GONE
    }

    override fun observeNetworkError() {
        viewModel.networkError.observe(viewLifecycleOwner){
            it.getContentIfNotHandledOrReturnNull()?.let { pair ->
                requireActivity().let { pActivity ->
                    if (pActivity is BaseActivity){
                        pActivity.showNetworkErrorDialog(pair, requireContext(), childFragmentManager)
                    }
                }
            }

        }
    }

    override fun observeSwipe() {
        // do nothing
    }

    override fun observeList() {
        viewModel.warehouseItemsList.observe(viewLifecycleOwner, {
            adapter.setOriginalListFirsTime(it)
            adapter.filter.filter(viewModel.searchQuery)
        })
    }

    override fun observeToast() {
        viewModel.toastMessage.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                showToast(getString(it))
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

    override fun onEmptyList(isEmpty: Boolean) {
        binding.recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyListIncluded.emptyListContainer.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onTapPart(part: TechnicianWarehousePart) {

        val customId = part.customId
        lifecycleScope.launch(Dispatchers.IO) {
            val partFromDB = PartsRepository.getTechnicianPartById(customId) ?: return@launch
            viewModel.partSelected = partFromDB
            if (partFromDB.bins.isNullOrEmpty()) {
                return@launch
            }

            partFromDB.bins.forEach {
                it.updateAvailableQuantityUI(partFromDB.itemId, partFromDB.warehouseID)
            }

            partFromDB.bins.removeIf { it.binAvailableQuantityUI <= 0 }

            if (partFromDB.bins.size == 1) {
                viewModel.binSelected = partFromDB.bins.first()
                viewModel.binSelected?.updateAvailableQuantityUI(
                    viewModel.partSelected?.itemId ?: 0, viewModel.partSelected?.warehouseID ?: 0
                )
                val binAvailableQuantityUI = viewModel.binSelected?.binAvailableQuantityUI ?: 0.0
                val binName = viewModel.binSelected?.bin ?: ""
                val serialNumber = viewModel.binSelected?.serialNumber ?: ""
                withContext(Dispatchers.Main) {
                    showConfirmationDialog(binName, binAvailableQuantityUI, serialNumber)
                }
                return@launch
            } else {
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_selectUsePartsFragment_to_selectUseBinFragment)
                }
            }
        }
    }


    private fun showConfirmationDialog(
        binName: String,
        binAvailableQuantityUI: Double,
        serialNumber: String
    ) {
        DialogManager.showAddPartDialog(
            binName,
            binAvailableQuantityUI,
            serialNumber,
            requireContext(),
            childFragmentManager,
            { quantity ->
                if(viewModel.addPartAsPending){
                    FBAnalyticsConstants.logEvent(requireContext(), FBAnalyticsConstants.PendingPartsFragment.ADD_PENDING_PART_ACTION)
                }else{
                    FBAnalyticsConstants.logEvent(requireContext(), FBAnalyticsConstants.UsedPartFragment.ADD_USED_PART_ACTION)
                }
                viewModel.createUsedPart(quantity) {
                    (requireActivity()).finish()
                }
            },
            {
                //do nothing
            })
    }

    override fun onTapDisabledItem() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(R.string.warning)
        dialog.setMessage(getString(R.string.this_item_is_available_in_the_customer_warehouse))
        dialog.setPositiveButton(R.string.ok) { _, _ ->
            //do nothing
        }
        dialog.show()
    }

}