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
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.CustomerLocationAdapter
import eci.technician.databinding.FragmentSearchCustomerAddressBinding
import eci.technician.helpers.KeyboardHelper
import eci.technician.helpers.api.retroapi.GenericDataModel
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.SearchDataRepository
import eci.technician.interfaces.CreateCallInterface
import eci.technician.models.create_call.CustomerItem
import eci.technician.viewmodels.CreateCallViewModel
import kotlinx.android.synthetic.main.fragment_call_types.*
import kotlinx.coroutines.*

class CustomerLocationFragment : Fragment(), CustomerLocationAdapter.CustomerItemListener {

    private val viewModel: CreateCallViewModel by activityViewModels()
    private var _binding: FragmentSearchCustomerAddressBinding? = null
    private var apiJob: Job? = null
    private val binding get() = _binding!!
    private var currentAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callBack = requireActivity().onBackPressedDispatcher.addCallback(this) {
            goToCreateCallFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSearchCustomerAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.select_customer)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        setSearchViewColors()
        binding.recyclerCustomerLocations.setHasFixedSize(true)
        binding.recyclerCustomerLocations.layoutManager = LinearLayoutManager(context)




        binding.searchview2.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    apiJob?.cancel()
                    if (it.isNotEmpty()) {
                        showProgressBar()
                        startSearching(it)
                    } else {
                        setCustomerLocationRecycler(mutableListOf())
                        hideProgressBar()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    apiJob?.cancel()
                    if (it.isNotEmpty()) {
                        showProgressBar()
                        startSearching(newText)
                    } else {
                        setCustomerLocationRecycler(mutableListOf())
                        hideProgressBar()
                    }
                }

                return false
            }
        })

    }

    override fun onStart() {
        super.onStart()
        binding.searchview2.requestFocus()
        KeyboardHelper.showKeyboard(binding.root)
    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recyclerCustomerLocations.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.recyclerCustomerLocations.visibility = View.VISIBLE
    }

    fun startSearching(searchQuery: String?) {
        apiJob = CoroutineScope(Dispatchers.IO).launch {
            SearchDataRepository.getSearchForCustomerItem(searchQuery)
            withContext(Dispatchers.Main) {
                SearchDataRepository.searchCustomerItemsLiveData.observe(viewLifecycleOwner, Observer { genericDataModel: GenericDataModel<MutableList<CustomerItem>>? ->
                    run {
                        if (genericDataModel?.isSuccess == true) {
                            val data = genericDataModel.data
                            data?.let {
                                setCustomerLocationRecycler(filterActiveCustomer(it))
                            }
                        } else {
                            if (genericDataModel != null && genericDataModel.isSuccess == false && genericDataModel.requestStatus == RequestStatus.TIMEOUT) {
                                showMessage(getString(R.string.somethingWentWrong), getString(R.string.the_request_time_out))
                                setCustomerLocationRecycler(mutableListOf())
                            }
                        }
                    }
                })
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
                }
            }
        }
    }

    private fun filterActiveCustomer(customerList: MutableList<CustomerItem>): MutableList<CustomerItem> {
        return customerList.filter { it.active }.toMutableList()
    }

    private fun setCustomerLocationRecycler(customerList: MutableList<CustomerItem>) {
        hideProgressBar()
        binding.recyclerCustomerLocations.adapter = CustomerLocationAdapter(customerList, this)
        (binding.recyclerCustomerLocations.adapter as CustomerLocationAdapter).notifyDataSetChanged()
    }

    private fun setSearchViewColors() {
        val searchTextView: TextView = binding.searchview2.findViewById(androidx.appcompat.R.id.search_src_text)
        val closeButton: ImageView = binding.searchview2.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setColorFilter(Color.WHITE)
        searchTextView.setTextColor(Color.WHITE)
        searchTextView.setHintTextColor(Color.DKGRAY)
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

    private fun goToCreateCallFragment() {
        KeyboardHelper.hideSoftKeyboard(binding.root)
        (activity as CreateCallInterface).goToCreateCallFragment()
    }

    override fun onTapCustomerItem(item: CustomerItem) {
        viewModel.customerLocationSelected = item
        viewModel.equipmentItemSelected = null
        viewModel.callerField = ""
        goToCreateCallFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        KeyboardHelper.hideSoftKeyboard(binding.root)
        apiJob?.cancel()
    }
}