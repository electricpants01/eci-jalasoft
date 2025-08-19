package eci.technician.activities.requestNeededParts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.searchWarehouses.SearchWarehousePartsAdapter
import eci.technician.custom.CustomPartUseDialog
import eci.technician.databinding.SearchableListBinding
import eci.technician.helpers.AppAuth
import eci.technician.interfaces.ISearchableList
import eci.technician.models.order.Part
import eci.technician.repository.PartsRepository
import eci.technician.tools.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllAvailablePartsFragment : Fragment(), ISearchableList,
    SearchWarehousePartsAdapter.IEmptyList,
    PartsAdapter.PartRequestNeedListener {
    private var _binding: SearchableListBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: PartUseViewModel by activityViewModels()
    private var fragmentToast: Toast? = null
    lateinit var adapter: PartsAdapter
    var dialog: AlertDialog? = null


    private val searchListenerObj =
        object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
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
        // Inflate the layout for this fragment
        _binding = SearchableListBinding
            .inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearchView()
        setupEmptyList()
        binding.loaderIncluded.progressBarContainer.visibility = View.GONE
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchParts(forceUpdate = true) }
        binding.swipeRefresh.isEnabled = true
        setObservers()
        observeLoading()
    }

    private fun observeLoading() {
        viewModel.loading.observe(viewLifecycleOwner){ showSpinner->
            binding.loaderIncluded.progressBarContainer.visibility = if (showSpinner) View.VISIBLE else View.GONE
            binding.emptyListIncluded.emptyListContainer.visibility = if (showSpinner) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun setupRecycler() {
        adapter = PartsAdapter(this, this)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recycler.adapter = adapter
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
        binding.emptyListIncluded.mainText.text = getString(R.string.add_part)
        binding.emptyListIncluded.secondaryText.text =
            getString(R.string.no_items_were_found)
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

    override fun observeSwipe() {
        viewModel.swipeLoading.observe(viewLifecycleOwner, {
            binding.swipeRefresh.isRefreshing = it
        })
    }

    override fun observeList() {
        viewModel.neededPartsList.observe(viewLifecycleOwner, {
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

    override fun onEmptyList(isEmpty: Boolean) {
        binding.recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyListIncluded.emptyListContainer.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onTapPart(part: Part) {
        val customId = part.customId
        val canShowEditDescription =
            AppAuth.getInstance().technicianUser.isAllowUnknownItems && !viewModel.isRequestingPart

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val partCopyFromRealm =
                    PartsRepository.getPartByCustomPartId2(customId) ?: return@withContext
                showPartUseDialog(partCopyFromRealm, canShowEditDescription)
            }
        }
    }

    private fun showPartUseDialog(partCopyFromRealm: Part, canShowEditDescription: Boolean) {
        CustomPartUseDialog(
            partCopyFromRealm.availableQty,
            partCopyFromRealm.description ?: "",
            canShowEditDescription,
            { quantity: Double, description: String ->
                if (viewModel.isRequestingPart) {
                    requireActivity().intent.putExtra(
                        Constants.EXTRA_PART_ID,
                        partCopyFromRealm.itemId
                    )
                    requireActivity().intent.putExtra(
                        Constants.EXTRA_PART_NAME,
                        partCopyFromRealm.item ?: ""
                    )
                    requireActivity().intent.putExtra(Constants.EXTRA_QUANTITY, quantity)
                    requireActivity().setResult(
                        AppCompatActivity.RESULT_OK,
                        requireActivity().intent
                    )
                    requireActivity().finish()
                } else {
                    viewModel.saveNeededPart(
                        partCopyFromRealm,
                        quantity,
                        description,
                        canShowEditDescription
                    ) {
                        requireActivity().finish()
                    }
                }
            },
            {
            }).show(childFragmentManager, "UsePartDialog")
    }

}