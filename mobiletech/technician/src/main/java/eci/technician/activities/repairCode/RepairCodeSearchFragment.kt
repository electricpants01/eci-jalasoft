package eci.technician.activities.repairCode

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.databinding.SearchableListSearchButtonBinding
import eci.technician.helpers.KeyboardHelper
import eci.technician.interfaces.IRepairCodeListener
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.RepairCode
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.equipment_search_dialog.*


class RepairCodeSearchFragment : Fragment(), IRepairCodeListener, ISearchableList {

    private lateinit var binding: SearchableListSearchButtonBinding
    private var orderId: Int = 0
    private lateinit var adapter: RepairCodesListAdapter
    private val repairCodeViewModel: RepairCodeSearchViewModel by activityViewModels()

    companion object {
        const val TAG = "RepairCodeSearchFragment"
        const val EXCEPTION = "Exception"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SearchableListSearchButtonBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupEmptyList()
        binding.swipeRefresh.isEnabled = false
        binding.loaderIncluded.progressBarContainer.visibility = View.GONE
        orderId = activity?.intent?.getIntExtra(Constants.EXTRA_ORDER_ID, 0) ?: 0
        repairCodeViewModel.getAllRepairCodes()
        observeList()
        setupSearchView()
    }

    override fun onProblemCodePressed(item: RepairCode) {
        repairCodeViewModel.saveProblemCodeToDB(
            orderId,
            item.repairCodeId,
            item.repairCodeName.toString(),
            item.description.toString()
        ){
            activity?.finish()
        }
    }

    override fun setupRecycler() {
        adapter = RepairCodesListAdapter(this)
        adapter.setRepairCodeListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL)
        )
        binding.recyclerView.adapter = adapter
    }

    override fun observeList() {
        repairCodeViewModel.repairCodeList.observe(viewLifecycleOwner,{
            adapter.setOriginalListFirsTime(it)
        })

        repairCodeViewModel.searchQuery.observe(viewLifecycleOwner,{ searchQuery ->
            adapter.filter.filter(searchQuery)
        })
    }

    override fun setupSearchView() {
        binding.searchbarIncluded.btnSearch.setOnClickListener {
            val searchText = binding.searchbarIncluded.txtSearch.text.toString()
            repairCodeViewModel.setSearchQuery(searchText)
            KeyboardHelper.hideSoftKeyboard(binding.root)
        }
    }

    override fun setupEmptyList() {
        binding.emptyListIncluded.emptyListContainer.visibility = View.GONE
        binding.emptyListIncluded.mainText.text = getString(R.string.repair_codes)
        binding.emptyListIncluded.secondaryText.text =
            getString(R.string.no_items_were_found)
    }

    override fun onEmptyList(isEmpty: Boolean) {
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyListIncluded.emptyListContainer.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun observeNetworkError() {
        //Do nothing
    }

    override fun observeSwipe() {
        //Do nothing
    }

    override fun observeToast() {
        //Do nothing
    }

}