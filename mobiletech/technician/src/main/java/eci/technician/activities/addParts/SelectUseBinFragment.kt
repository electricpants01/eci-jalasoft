package eci.technician.activities.addParts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.databinding.SearchableListBinding
import eci.technician.dialog.DialogManager
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.Bin


class SelectUseBinFragment : Fragment(), ISearchableList, IBinTapListener {

    private var _binding: SearchableListBinding? = null
    private val binding
        get() = _binding!!
    val viewModel: AddPartViewModel by activityViewModels()
    lateinit var adapter: BinAdapter

    private val searchListenerObj =
        object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchQueryBin = newText
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
        binding.swipeRefresh.isEnabled = false
        binding.loaderIncluded.progressBarContainer.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        setObservers()
    }

    override fun setupRecycler() {
        adapter = BinAdapter(this, "", "", this)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.recycler.adapter = adapter
    }

    override fun setupSearchView() {
        binding.searchbarIncluded.searchview2.setOnQueryTextListener(searchListenerObj)
        setSearchViewColors()
    }

    override fun setupEmptyList() {
        binding.emptyListIncluded.emptyListContainer.visibility = View.GONE
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

    override fun observeNetworkError() {
        // do nothing
    }

    override fun observeSwipe() {
        // do nothing
    }

    override fun observeList() {
        val bins = viewModel.partSelected?.bins ?: listOf<Bin>()
        val partSelected = viewModel.partSelected?.itemId ?: 0
        bins.forEach {
            it.updateAvailableQuantityUI(partSelected, viewModel.partSelected?.warehouseID ?: 0)
        }
        val binsCopy = bins.toMutableList()
        binsCopy.removeIf { it.binAvailableQuantityUI < 1.0 }
        binsCopy.sortedWith(compareBy({ it.bin }, { it.serialNumber }))

        adapter.partDescription = viewModel.partSelected?.description ?: ""
        adapter.partName = viewModel.partSelected?.item ?: ""
        adapter.setOriginalListFirsTime(binsCopy)
        adapter.filter.filter(viewModel.searchQueryBin)
    }

    override fun observeToast() {
        // do nothing
    }

    override fun onEmptyList(isEmpty: Boolean) {
        binding.recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyListIncluded.emptyListContainer.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onBinSelected(bin: Bin, binAvailableQuantityUI: Double) {
        viewModel.binSelected = bin
        val binName = viewModel.binSelected?.bin ?: ""
        val serialNumber = viewModel.binSelected?.serialNumber ?: ""
        DialogManager.showAddPartDialog(
            binName,
            binAvailableQuantityUI,
            serialNumber,
            requireContext(),
            childFragmentManager,
            { quantity ->
                if(viewModel.addPartAsPending){
                    FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.PendingPartsFragment.ADD_PENDING_PART_ACTION)
                }else{
                    FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.UsedPartFragment.ADD_USED_PART_ACTION)
                }
                viewModel.createUsedPart(quantity) {
                    (requireActivity()).finish()
                }
            }, {
                //do nothing
            })
    }


}