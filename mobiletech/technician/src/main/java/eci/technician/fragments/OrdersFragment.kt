package eci.technician.fragments

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.MapsActivity
import eci.technician.activities.OrderDetailActivity
import eci.technician.activities.serviceOrderFilter.FilterServiceOrderActivity
import eci.technician.adapters.ServiceOrdersAdapter
import eci.technician.helpers.AppAuth
import eci.technician.helpers.FilterHelper
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.interfaces.IOrderListTabListener
import eci.technician.models.TechnicianUser
import eci.technician.models.filters.FilterCriteria
import eci.technician.models.order.ServiceOrder
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import eci.technician.viewmodels.OrderFragmentViewModel
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_orders.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class OrdersFragment : Fragment(), ServiceOrdersAdapter.ServiceOrderListListener,
    AppAuth.AuthStateListener, IOrderListTabListener {

    companion object {
        const val TAG = "OrdersFragment"
        const val EXCEPTION = "Exception"
    }

    private var textToSearch = ""
    private var newadapter: ServiceOrdersAdapter? = null
    private var searchMenuItem: MenuItem? = null
    private lateinit var realm: Realm
    private var connection: NetworkConnection? = null
    private val viewModel: OrderFragmentViewModel by activityViewModels()
    var whenFilter: FilterHelper.DateFilter = FilterHelper.DateFilter.NOT_SELECTED
    var statusFilter: FilterHelper.ServiceCallStatus = FilterHelper.ServiceCallStatus.NOT_SELECTED
    private var apiJob: Job? = null
    private var technicianUser: TechnicianUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        context?.let {
            connection = NetworkConnection(it)
        }
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppAuth.getInstance().addUserUpdateListener(this)
        recOrders.layoutManager = SafeLinearLayoutManager(activity)
        newadapter = ServiceOrdersAdapter(mutableListOf(), this, lifecycleScope)

        showProgressBar()

        observeServiceOrders()
        observeSwipeRefresh()
        observeNetworkErrors()

        connection?.observe(viewLifecycleOwner, Observer { aBoolean ->
            if (aBoolean) {
                object : CountDownTimer(1500, 100) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        AppAuth.getInstance().isConnected = true
                    }
                }.start()
            } else {
                AppAuth.getInstance().isConnected = false
                if (refreshOrders.isRefreshing) {
                    refreshOrders.isRefreshing = false
                    showOfflineDialogue()
                }
            }
        })

        refreshOrders.setOnRefreshListener {
            if (AppAuth.getInstance().isConnected) {
                viewModel.fetchTechnicianActiveServiceCalls(forceUpdate = true)
                loadAllEquipmentMeters()
                loadAllCallTypes()
            } else {
                refreshOrders.isRefreshing = false
                showOfflineDialogue()
            }
        }
        checkFiltersVisibility()
    }

    private fun observeNetworkErrors() {
        viewModel.networkError.observe(viewLifecycleOwner) {
            it.getContentIfNotHandledOrReturnNull()?.let { error ->
                requireActivity().let { activity ->
                    if (activity is BaseActivity) {
                        activity.showNetworkErrorDialog(
                            error,
                            requireContext(),
                            childFragmentManager
                        )
                    }
                }
            }
        }
    }

    private fun observeSwipeRefresh() {
        viewModel.swipeLoading.observe(viewLifecycleOwner) { shouldRefresh ->
            refreshOrders.isRefreshing = shouldRefresh
        }
    }

    private fun checkFiltersVisibility() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                AppAuth.getInstance()?.technicianUser?.let {
                    val hasRestrictCallOrder = it.isRestrictCallOrder
                    if (hasRestrictCallOrder) {
                        imgStatusFilter.visibility = View.GONE
                        sortedByTextView.text = ""
                        txtFilter.text = ""
                        txtFilterDateAndStatus.text = ""
                    } else {
                        imgStatusFilter.visibility = View.VISIBLE
                        imgStatusFilter.setOnClickListener {
                            goToFilters()
                        }
                    }
                }
            }
        }
    }

    private fun loadAllCallTypes() {
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllCallTypes()
    }

    private fun loadAllEquipmentMeters() {
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllEquipmentMeters()
    }

    private fun showProgressBar() {
        progressBarContainer.visibility = View.VISIBLE
        recOrders.visibility = View.GONE
    }

    private fun hideProgressBar() {
        progressBarContainer.visibility = View.GONE
        recOrders.visibility = View.VISIBLE
    }

    private fun observeFilterCriteria() {
        viewModel.filterCriteriaFlow.observe(this) {
            showFilterText(it)
        }
    }

    private fun showFilterText(filterCriteria: FilterCriteria?) {
        val sortList = mutableListOf<String>()
        val filterList = mutableListOf<String>()
        filterCriteria?.callSortItemSelected?.let { value ->
            if (value != -1) sortList.add(
                FilterHelper.getStringForTechniciansSort(
                    value,
                    requireContext()
                )
            )
        }
        filterCriteria?.callDateSelected?.let { value ->
            if (value != -1) filterList.add(
                FilterHelper.getStringForTechnicianDateFilter(
                    value,
                    requireContext()
                )
            )
        }
        filterCriteria?.callStatusSelected?.let { value ->
            if (value != -1) filterList.add(
                FilterHelper.getStringFonTechnicianStatusFilter(
                    value,
                    requireContext()
                )
            )
        }
        filterCriteria?.callPrioritySelected?.let {
            if (it.isNotEmpty()) filterList.add(it)
        }
        filterCriteria?.callTypeFilterSelected?.let {
            if (it.isNotEmpty()) filterList.add(it)
        }
        txtFilterDateAndStatus.text = filterList.joinToString(", ")
        sortedByTextView.text = sortList.joinToString(", ")
    }

    private fun goToFilters() {
        val intentToFilter = Intent(context, FilterServiceOrderActivity::class.java)
        intentToFilter.putExtra(FilterServiceOrderActivity.IS_GROUP_FILTER, false)
        startActivity(intentToFilter)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getFilterCriteriaFlow()
        observeFilterCriteria()
        observeServiceOrders()
    }

    private fun showOfflineDialogue() {
        context?.let { context ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setMessage(getString(R.string.offline_warning)).setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
            builder.create().show()
        }
    }

    private fun initAdapter(orders: MutableList<ServiceOrder>) {
        if (AppAuth.getInstance().technicianUser == null) {
            return
        }
        val serviceOrders: MutableList<ServiceOrder> = realm.copyFromRealm(orders)
        if (serviceOrders.size > 0) {
            hideProgressBar()
        }
        newadapter = ServiceOrdersAdapter(orders, this, lifecycleScope)
        newadapter?.setQuery(textToSearch)
        recOrders.adapter = newadapter
    }

    override fun onDestroy() {
        apiJob?.cancel()
        realm.close()
        AppAuth.getInstance().removeUserUpdateListener(this)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        observeServiceOrders()
    }

    private fun observeServiceOrders() {
        viewModel.getServiceOrder()
        viewModel.serviceOrderList.observe(this) {
            hideProgressBar()
            checkFiltersVisibility()
            initAdapter(it.toMutableList())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_orders, menu)
        searchMenuItem = menu.findItem(R.id.search)
        searchMenuItem?.let { searchMenuItem ->
            val searchView: SearchView = searchMenuItem.actionView as SearchView
            searchView.maxWidth = Int.MAX_VALUE

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { queryText ->
                        textToSearch = queryText
                        if (queryText.isNotEmpty()) {
                            viewModel.textToSearch = queryText
                        }
                        newadapter?.setQuery(queryText)
                        if (whenFilter != FilterHelper.DateFilter.NOT_SELECTED || statusFilter != FilterHelper.ServiceCallStatus.NOT_SELECTED) {
                            txtFilter.text =
                                if (queryText.isNotEmpty()) "\"${queryText}\" , " else ""
                        } else {
                            txtFilter.text = if (queryText.isNotEmpty()) "\"${queryText}\" " else ""
                        }
                    }
                    return true
                }
            })
            searchView.setOnSearchClickListener {
                newadapter?.setQuery("")
            }
            try {
                val closeButton = searchView.findViewById<ImageView>(R.id.search_close_btn)
                closeButton.setOnClickListener {
                    viewModel.textToSearch = ""
                    searchView.setQuery("", false);
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e.fillInStackTrace())
            }

            if (viewModel.textToSearch.isNotEmpty()) {
                searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                        searchView.onActionViewExpanded()
                        searchView.setQuery(viewModel.textToSearch, true)
                        return true
                    }

                    override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                        viewModel.textToSearch = ""
                        return true
                    }
                })
                searchMenuItem.expandActionView()
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.loadMap) {
            if (AppAuth.getInstance().isConnected) {
                val intent = Intent(activity, MapsActivity::class.java)
                intent.putExtra(Constants.ORIGIN_MAP_ACTIVITY, Constants.MY_SERVICE_CALLS)
                intent.putExtra(Constants.EXTRA_QUERY_SERVICE_CALL_LIST, viewModel.textToSearch)
                startActivity(intent)
            } else {
                showOfflineDialogue()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onServiceOrderTap(serviceOrder: ServiceOrder) {
        try {
            val intent = Intent(context, OrderDetailActivity::class.java)
            intent.putExtra(Constants.EXTRA_ORDER_ID, serviceOrder.id)
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_CODE, serviceOrder.callNumber_Code)
            intent.putExtra(Constants.EXTRA_CALL_NUMBER_ID, serviceOrder.callNumber_ID)
            this.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }


    override fun userUpdated(technicianUser: TechnicianUser?) {
        this.technicianUser = AppAuth.getInstance().technicianUser
        this.technicianUser?.let {
            if (it.status.equals(Constants.STATUS_SIGNED_IN))
                showProgressBar()
        }
    }

    override fun authStateChanged(appAuth: AppAuth?) { /*not used*/
    }

    override fun gpsStateChanged(state: Boolean) { /*not used*/
    }

    override fun requestsChanged(count: Int) { /*not used*/
    }

    override fun updateTheCurrentList() {
        observeServiceOrders()
    }

}