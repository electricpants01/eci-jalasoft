package eci.technician.fragments.createCall

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.adapters.EquipmentItemAdapter
import eci.technician.databinding.FragmentSearchEquipmentBinding
import eci.technician.helpers.KeyboardHelper
import eci.technician.helpers.api.retroapi.GenericDataModel
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.SearchDataRepository
import eci.technician.interfaces.CreateCallInterface
import eci.technician.models.create_call.CustomerItem
import eci.technician.models.create_call.EquipmentItem
import eci.technician.viewmodels.CreateCallViewModel
import kotlinx.android.synthetic.main.fragment_call_types.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchEquipmentFragment : Fragment(), EquipmentItemAdapter.EquipmentItemListener {

    private val viewModel: CreateCallViewModel by activityViewModels()
    private var _binding: FragmentSearchEquipmentBinding? = null
    private var apiJob: Job? = null
    private val binding get() = _binding!!
    private var currentAlertDialog: AlertDialog? = null

    companion object{
        val TAG: String = SearchEquipmentFragment::class.java.name
        const val EXCEPTION = "Exception"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callBack = requireActivity().onBackPressedDispatcher.addCallback(this) {
            goToCreateCallFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSearchEquipmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.select_equipment)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        setSearchViewColors()
        initObservers()
        binding.recyclerEquipmentItems.setHasFixedSize(true)
        binding.recyclerEquipmentItems.layoutManager = LinearLayoutManager(context)
        binding.searchview2.requestFocus()
        viewModel.equipmentListFromCustomerSelected?.let {
            if (it.isEmpty()) {
                KeyboardHelper.showKeyboard(binding.searchview2)
            } else {
                KeyboardHelper.hideSoftKeyboard(binding.root)
            }
            setEquipmentRecycler(filterActiveEquipments(it))
        } ?: KeyboardHelper.showKeyboard(binding.searchview2)

        initSearchQueryListener()
    }

    private fun initSearchQueryListener(){

        binding.searchview2.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchQuery: String?): Boolean {
                searchQuery?.let { searchQueryText ->
                    apiJob?.cancel()
                    if (searchQueryText.isNotEmpty()) {
                        showProgressBar()
                        startSearching(searchQueryText)
                    } else {
                        hideProgressBar()
                        setEquipmentRecycler(mutableListOf())
                    }
                }
                return false
            }

            override fun onQueryTextChange(searchQuery: String?): Boolean {
                if (searchQuery.isNullOrEmpty() && viewModel.equipmentListFromCustomerSelected != null) {
                    viewModel.equipmentListFromCustomerSelected?.let {
                        setEquipmentRecycler(filterActiveEquipments(it))
                    }
                } else {
                    searchQuery?.let { searchQueryText ->
                        apiJob?.cancel()
                        if (searchQueryText.isNotEmpty()) {
                            showProgressBar()
                            startSearching(searchQueryText)
                        } else {
                            hideProgressBar()
                            setEquipmentRecycler(mutableListOf())
                        }
                    }
                }
                return false
            }
        })
    }

    private fun initObservers(){

        SearchDataRepository.searchEquipmentItemLiveData.observe(
            viewLifecycleOwner) { genericDataModel: GenericDataModel<MutableList<EquipmentItem>>? ->
            run {
                if (genericDataModel?.isSuccess == true) {
                    val data = genericDataModel.data
                    data?.let {
                        setEquipmentRecycler(filterActiveEquipments(it))
                    }
                } else {
                    if (genericDataModel != null && genericDataModel.isSuccess == false && genericDataModel.requestStatus == RequestStatus.TIMEOUT) {
                        showMessage(
                                getString(R.string.somethingWentWrong),
                                getString(R.string.the_request_time_out)
                        )
                        setEquipmentRecycler(mutableListOf())
                    }
                }
            }
        }
    }

    private fun showMessage(title: String, message: String) {
        if (currentAlertDialog?.isShowing != true) {
            context?.let { context ->
                currentAlertDialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                try {
                    currentAlertDialog?.show()
                } catch (e: Exception) {
                    Log.e(TAG,EXCEPTION, e)
                }
            }
        }
    }

    private fun startSearching(searchQuery: String) {
        if( viewModel.customerLocationSelected != null){
            SearchDataRepository.filterEquipmentItemBySearchQuery(searchQuery, viewModel.customerLocationSelected)
        }else{
            apiJob = CoroutineScope(Dispatchers.IO).launch {
                SearchDataRepository.fetchEquipmentItemBySearchQuery(searchQuery)
            }
        }
    }

    private fun filterActiveEquipments(equipmentList: MutableList<EquipmentItem>): MutableList<EquipmentItem> {
        return equipmentList.filter { it.active }.toMutableList()
    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recyclerEquipmentItems.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.recyclerEquipmentItems.visibility = View.VISIBLE
    }

    private fun setSearchViewColors() {
        val searchTextView: TextView =
            binding.searchview2.findViewById(androidx.appcompat.R.id.search_src_text)
        val closeButton: ImageView =
            binding.searchview2.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setColorFilter(Color.WHITE)
        searchTextView.setTextColor(Color.WHITE)
        searchTextView.setHintTextColor(Color.DKGRAY)
    }

    private fun setEquipmentRecycler(equipmentList: MutableList<EquipmentItem>) {
        hideProgressBar()
        binding.recyclerEquipmentItems.adapter = EquipmentItemAdapter(equipmentList, this)
        (binding.recyclerEquipmentItems.adapter as EquipmentItemAdapter).notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                goToCreateCallFragment()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTapEquipmentItem(item: EquipmentItem) {

        viewModel.equipmentItemSelected = item
        viewModel.remarksField = item.location ?: ""
        viewModel.callerField = item.caller ?: ""
        item.customerNumberCode?.let {
            viewModel.getCustomerDataByText(it)
                .observe(viewLifecycleOwner) { customerList ->
                    val filteredCustomerList =
                            customerList.filter { customerItem -> customerItem.active }

                    if (filteredCustomerList.isNotEmpty()) {
                        val customerItem = filteredCustomerList.find { customerItemFromList ->
                            customerItemFromList.customerNumberID == item.locationNumberID
                        }
                        if (customerItem != null) {
                            viewModel.customerLocationSelected = customerItem
                        } else {
                            var customerWithEquipmentSelected: CustomerItem? = null
                            filteredCustomerList.forEach { oneCustomer ->
                                val equipmentFound =
                                        oneCustomer.equipments?.find { equipmentFromOneCustomer ->
                                            equipmentFromOneCustomer.equipmentNumberID == item.equipmentNumberID
                                        }
                                equipmentFound?.let {
                                    customerWithEquipmentSelected = oneCustomer
                                }
                            }
                            customerWithEquipmentSelected?.let { customerFound ->
                                viewModel.customerLocationSelected = customerFound
                            }
                        }
                    }
                    goToCreateCallFragment()
                }
        }
    }

    private fun goToCreateCallFragment() {
        binding.searchview2.clearFocus()
        KeyboardHelper.hideSoftKeyboard(binding.root)
        (activity as CreateCallInterface).goToCreateCallFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        KeyboardHelper.hideSoftKeyboard(binding.root)
        apiJob?.cancel()
    }
}