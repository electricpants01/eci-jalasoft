package eci.technician.activities.problemCodes

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.databinding.SearchableListSearchButtonBinding
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.ProblemCode
import eci.technician.tools.Constants

class ProblemCodesSearchFragment : Fragment(), ProblemCodesListAdapter.IProblemCodeListener,
    ISearchableList {

    companion object {
        const val TAG = "ProblemCodeSearchFragment"
        const val EXCEPTION = "Exception"
    }

    private lateinit var binding: SearchableListSearchButtonBinding
    lateinit var adapter: ProblemCodesListAdapter
    private val problemCodeViewModel: ProblemCodesSearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.searchable_list_search_button, container, false)
        binding = SearchableListSearchButtonBinding.bind(view)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupEmptyList()
        binding.swipeRefresh.isEnabled = false
        binding.loaderIncluded.progressBarContainer.visibility = View.GONE
        problemCodeViewModel.orderId.value =
            activity?.intent?.getIntExtra(Constants.EXTRA_ORDER_ID, 0) ?: 0
        problemCodeViewModel.getAllProblemCodes()
        observeList()
        setupSearchView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            activity?.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onProblemCodePressed(item: ProblemCode) {
        problemCodeViewModel.saveProblemCodeToDB(item.problemCodeId,item.problemCodeName.toString(),item.description.toString()) {
            activity?.finish()
        }
    }

    override fun setupRecycler() {
        adapter = ProblemCodesListAdapter(this, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.recyclerView.adapter = adapter
    }

    override fun observeList() {
        problemCodeViewModel.problemCodesList.observe(viewLifecycleOwner, { problemCodeList ->
            adapter.setOriginalListFirsTime(problemCodeList)
        })

        problemCodeViewModel.searchQuery.observe(viewLifecycleOwner, { searchQuery ->
            adapter.filter.filter(searchQuery)
        })
    }

    override fun setupSearchView() {
        binding.searchbarIncluded.btnSearch.setOnClickListener {
            val searchText = binding.searchbarIncluded.txtSearch.text.toString()
            problemCodeViewModel.searchQuery.postValue(searchText)
            hideKeyboard(requireActivity())
        }
    }

    override fun setupEmptyList() {
        binding.emptyListIncluded.emptyListContainer.visibility = View.GONE
        binding.emptyListIncluded.mainText.text = getString(R.string.problem_codes)
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

    private fun hideKeyboard(activity: Activity) {
        Handler(Looper.getMainLooper())
            .postDelayed({
                val view = activity.currentFocus
                if (view != null) {
                    val imm =
                        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
                }
            }, 500)
    }

}