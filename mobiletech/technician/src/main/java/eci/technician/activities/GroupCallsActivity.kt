package eci.technician.activities

import android.content.*
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.serviceOrderFilter.FilterServiceOrderActivity
import eci.technician.adapters.GroupCallsAdapter
import eci.technician.adapters.GroupCallsAdapter.GroupCallListener
import eci.technician.databinding.ActivityGroupCallsBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ErrorHelper.RequestError
import eci.technician.helpers.FilterHelper
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.PhoneHelper
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.ProcessingResult
import eci.technician.models.TechnicianGroup
import eci.technician.models.data.ReassignResponse
import eci.technician.models.data.UnavailableParts
import eci.technician.models.filters.FilterCriteria
import eci.technician.models.order.GroupCallServiceOrder
import eci.technician.repository.GroupCallsRepository
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import eci.technician.viewmodels.GroupCallsViewModel
import eci.technician.viewmodels.OrderFragmentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap


class GroupCallsActivity : BaseActivity(), GroupCallListener {
    private lateinit var binding: ActivityGroupCallsBinding
    private lateinit var banner: TextView
    private var adapter: GroupCallsAdapter? = null
    private var searchMenuItem: MenuItem? = null
    private var textToSearch: String? = null
    private var isComingFromMap = false
    private val viewModel by lazy {
        ViewModelProvider(this)[GroupCallsViewModel::class.java]
    }

    private val filterViewModel: OrderFragmentViewModel by viewModels()
    private val connection by lazy {
        NetworkConnection(this)
    }

    companion object {
        const val TAG = "GroupCallsActivity"
        const val EXCEPTION = "Exception"
        const val LAUNCH_FILTER_ACTIVITY_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_group_calls)
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.GROUP_CALL_ACTIVITY)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        filterViewModel.isGroupFilter = true
        banner = binding.containerOffline.offlineTextView
        if (AppAuth.getInstance().isConnected) {
            banner.visibility = View.GONE
        } else {
            banner.setText(R.string.offline_no_internet_connection)
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOfflineDark))
            banner.visibility = View.VISIBLE
        }

        connection.observe(this) { aBoolean: Boolean ->
            if (aBoolean) {
                banner.setText(R.string.back_online)
                AppAuth.getInstance().isConnected = true
                banner.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnline))
                object : CountDownTimer(3000, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        Log.d(TAG, "waiting")
                    }

                    override fun onFinish() {
                        if (AppAuth.getInstance().isConnected) {
                            banner.visibility = View.GONE
                        }
                    }
                }.start()
            } else {
                banner.setText(R.string.offline_no_internet_connection)
                banner.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOfflineDark))
                banner.visibility = View.VISIBLE
                AppAuth.getInstance().isConnected = false
            }
        }


        binding.clearFilterButton.setOnClickListener { clearFilter() }
        binding.emptyLinearLayout.visibility = View.GONE
        RetrofitRepository.RetrofitRepositoryObject.getInstance().getAllCallTypes()
        setRecycler()
        setObservers()
    }

    private fun setObservers() {
        filterViewModel.filterCriteriaFlow.observe(this) {
            updateText(it)
            val temporalLastGroupNameSelected = it.groupNameSelected ?: ""
            if (filterViewModel.lastGroupNameSelected != temporalLastGroupNameSelected){
                filterViewModel.lastGroupNameSelected = it.groupNameSelected ?: ""
                validateGroupNames()
            }else {
                if (viewModel.listOfGroupCallLoaded.isNotEmpty()) {
                    initAdapter(viewModel.listOfGroupCallLoaded)
                }
            }
        }
    }

    private fun updateText(filterCriteria: FilterCriteria) {
        val sortList = mutableListOf<String>()
        val filterList = mutableListOf<String>()
        var sortItem = -1
        filterCriteria.callSortItemSelected.let {
            sortItem = it
            sortList.add(FilterHelper.getStringForTechniciansSort(it, this))
        }

        filterCriteria.callDateSelected.let {
            val value = FilterHelper.getStringForTechnicianDateFilter(it, this)
            if (value.isNotEmpty())  filterList.add(value)

        }
        filterCriteria.callStatusSelected.let {
            val value = FilterHelper.getStringFonTechnicianStatusFilter(it, this)
            if (value.isNotEmpty()) filterList.add(value)
        }
        filterCriteria.groupNameSelected?.let {
            if (it.isNotEmpty()) filterList.add(it)
        }
        filterCriteria.callPrioritySelected?.let {
            if (it.isNotEmpty()) filterList.add(it)
        }
        filterCriteria.callTechnicianNameSelected?.let {
            if (it.isNotEmpty()) filterList.add(it)
        }
        filterCriteria.callTypeFilterSelected?.let {
            if (it.isNotEmpty()) filterList.add(it)
        }
        val result = filterList.joinToString(", ")

        binding.sortByTextView.text = getString(R.string.sorted_by, sortList.joinToString(", "))
        binding.filterTextContainer.visibility = View.VISIBLE
        if (filterList.isNotEmpty()) {
            binding.filteredByTitleTextView.text = getString(R.string.filtered_by, result)
            binding.filteredByTitleTextView.visibility = View.VISIBLE
        } else {
            binding.filteredByTitleTextView.visibility = View.GONE
        }

        if (sortItem == 1 && filterList.isEmpty()) {
            binding.clearFilterButton.visibility = View.GONE
        } else {
            binding.clearFilterButton.visibility = View.VISIBLE
        }
    }

    private fun setRecycler() {
        binding.recGroupCalls.layoutManager = SafeLinearLayoutManager(this)
        adapter?.let {
            binding.recGroupCalls.adapter = it
        }
    }

    private fun showProgressBar() {
        setEnableOptionsMenu(false)
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.containerList.visibility = View.GONE
    }

    private fun hideProgressBar() {
        setEnableOptionsMenu(true)
        binding.progressBarContainer.visibility = View.GONE
        binding.containerList.visibility = View.VISIBLE
    }

    private fun getGroupsFromUser() {
        AppAuth.getInstance().technicianUser.id?.let {
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .getGroupsByUserId2(it, this)
                .observe(this) { genericDataResponse ->
                    when (genericDataResponse.responseType) {
                        RequestStatus.SUCCESS -> {
                            genericDataResponse.data?.let { manageGroupSuccess(it) }
                        }
                        RequestStatus.ERROR -> {
                            genericDataResponse.onError?.let { showMessageOnRequestError(it) }
                        }
                        else -> {
                            genericDataResponse.onError?.let { showMessageOnRequestError(it) }
                        }
                    }
                }
        }
    }

    private fun manageGroupSuccess(it: MutableList<TechnicianGroup>) {
        viewModel.technicianGroupList = it
    }

    private fun showMessageOnRequestError(it: RequestError) {
        this.showNetworkErrorDialog(it, this, supportFragmentManager)
        hideProgressBar()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_GROUP_CALLS_DETAIL) {
            val changedGroupCallId = data?.getIntExtra(Constants.FROM_DETAILS, 0)
            changedGroupCallId?.let {
                if (it > 0) {
                    viewModel.assignedIdCalls.add(it)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
        if (resultCode == Constants.ACTIVITY_GROUP_CALLS_DETAIL_ERROR) {
            val serviceOrderCallNumberId =
                data?.getStringExtra(Constants.ACTIVITY_GROUP_CALLS_DETAIL_ID)
            serviceOrderCallNumberId?.let {
                RetrofitRepository.RetrofitRepositoryObject
                    .getInstance()
                    .getOneGroupCallServiceByCallId(
                        serviceOrderCallNumberId.toInt(),
                        lifecycleScope,
                        this
                    )
                    .observe(this) {
                        when (it.responseType) {
                            RequestStatus.SUCCESS -> {
                                it.data?.let {
                                    val currentServiceOrder = it.first()
                                    adapter?.updateFromList(currentServiceOrder)
                                    adapter?.updateFromFilteredList(currentServiceOrder)
                                    viewModel.updateGroupCall(currentServiceOrder)
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        GroupCallsRepository.updateGroupServiceCall(
                                            currentServiceOrder
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
            clearFilterText()
        }
        if (resultCode == Constants.ACTIVITY_GROUP_CALLS_DETAIL_DELETE) {
            val serviceOrderCallNumberCode =
                data?.getStringExtra(Constants.ACTIVITY_GROUP_CALLS_DETAIL_CODE)
            serviceOrderCallNumberCode?.let {
                deleteGroupServiceCall(serviceOrderCallNumberCode)
            }
        }

        if (resultCode == Constants.ACTIVITY_GROUP_CALLS_MAP_LIST_DELETE) {
            val result = data?.getStringExtra(Constants.ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_DELETE)
            result?.let {
                val serviceOrderCallNumberCodeList = result.split("$").toTypedArray()
                for (groupCall in serviceOrderCallNumberCodeList) {
                    if (!groupCall.isNullOrBlank()) {
                        isComingFromMap = true
                        deleteGroupServiceCall(groupCall)
                    }
                }
            }
            val resultUpdate =
                data?.getStringExtra(Constants.ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_UPDATE)
            resultUpdate?.let {
                val serviceOrderCallNumberCodeList = resultUpdate.split("$").toTypedArray()
                for (groupCall in serviceOrderCallNumberCodeList) {
                    if (!groupCall.isNullOrBlank()) {
                        updateReassignedGroupServiceCall(groupCall)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun deleteGroupServiceCall(serviceOrderCallNumberCode: String) {
        val position = adapter?.getItemPositionById(serviceOrderCallNumberCode.toString())
        position?.let {
            if (position >= 0) {
                val groupCallServiceOrder = adapter?.getServiceCall(position)
                groupCallServiceOrder?.let { sc ->
                    if (!isComingFromMap) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            GroupCallsRepository.deleteGroupCallServiceOrder(sc)
                        }
                    }
                    adapter?.deleteFromLists(sc)
                    viewModel.deleteGroupCall(sc)
                    adapter?.notifyDataSetChanged()
                    if (adapter?.itemCount == 0) {
                        clearFilterText()
                    }
                }
            }
        }
        isComingFromMap = false
    }

    private fun updateReassignedGroupServiceCall(serviceOrderCallNumberCode: String) {
        val position = adapter?.getItemPositionById(serviceOrderCallNumberCode)
        position?.let {
            if (position >= 0) {
                val groupCallServiceOrder = adapter?.getServiceCall(position)
                groupCallServiceOrder?.let { sc ->
                    if (!isComingFromMap) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val updatedGroupCall =
                                GroupCallsRepository.getGroupCallServiceOrder(sc.callNumber_ID)
                            updatedGroupCall?.let {
                                withContext(Dispatchers.Main) {
                                    viewModel.updateGroupCall(updatedGroupCall)
                                    adapter?.updateFromList(updatedGroupCall)
                                    textToSearch?.let { it1 -> adapter?.setQuery(it1) }
                                    if (adapter?.itemCount == 0) {
                                        clearFilterText()
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToFilterActivity() {
        val intent = Intent(this, FilterServiceOrderActivity::class.java)
        intent.putExtra(FilterServiceOrderActivity.IS_GROUP_FILTER, true)
        startActivityForResult(intent, LAUNCH_FILTER_ACTIVITY_CODE)
    }

    override fun onResume() {
        super.onResume()
        filterViewModel.getFilterCriteriaFlow()
        getGroupsFromUser()
        setObservers()
    }


    private fun initAdapter(it: MutableList<GroupCallServiceOrder>) {
        val listOfGroupCallServiceOrderFiltered = filterViewModel.applyGroupServiceCallFilter(it)
        listOfGroupCallServiceOrderFiltered.removeAll { viewModel.assignedIdCalls.contains(it.callNumber_ID) }
        adapter = GroupCallsAdapter(listOfGroupCallServiceOrderFiltered, this, lifecycleScope)
        binding.recGroupCalls.adapter = adapter
        viewModel.textToSearch.let { textToSearch ->
            adapter?.setQuery(textToSearch)
        }
        (binding.recGroupCalls.adapter as GroupCallsAdapter).notifyDataSetChanged()
    }

    private fun validateGroupNames() {
        if (filterViewModel.lastGroupNameLoaded == "" && filterViewModel.lastGroupNameSelected == "") {
            if (viewModel.listOfGroupCallLoaded.isNotEmpty()) {
                initAdapter(viewModel.listOfGroupCallLoaded)
            } else {
                requestGroupCalls(null)
            }
        } else {
            if (filterViewModel.lastGroupNameLoaded != filterViewModel.lastGroupNameSelected) {
                requestGroupCalls(filterViewModel.lastGroupNameSelected)
            } else {
                initAdapter(viewModel.listOfGroupCallLoaded)
            }
        }
    }


    private fun requestGroupCalls(groupName: String?) {
        showProgressBar()
        RetrofitRepository.RetrofitRepositoryObject.getInstance()
            .getGroupServiceCallListWithFilterType(2, groupName, this)
            .observe(this) { genericDataResponse ->
                when (genericDataResponse.responseType) {
                    RequestStatus.SUCCESS -> {
                        hideProgressBar()
                        filterViewModel.lastGroupNameLoaded = groupName ?: ""
                        genericDataResponse.data?.let { manageGroupCallListSuccess(it) }
                    }
                    else -> {
                        hideProgressBar()
                        genericDataResponse.onError?.let { showMessageOnRequestError(it) }
                    }
                }
            }
    }

    private fun manageGroupCallListSuccess(it: MutableList<GroupCallServiceOrder>) {
        viewModel.listOfGroupCallLoaded = it
        initAdapter(it)
    }

    private fun callContact(phone: String) {
        val phoneToCall: String = PhoneHelper.getOnlyNumbersBeforeDialing(phone)
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneToCall")
        startActivity(intent)
    }

    private fun tryReAssignCall(groupCallServiceOrder: GroupCallServiceOrder) {

        groupCallServiceOrder.callNumber_ID?.let { serviceOrderCallNumberId ->
            RetrofitRepository.RetrofitRepositoryObject
                .getInstance()
                .getOneGroupCallServiceByCallId(serviceOrderCallNumberId, lifecycleScope, this)
                .observe(this) {
                    manageOneServiceCallResponse(it, groupCallServiceOrder)
                }
        }
    }

    private fun reAssignCall(groupCallServiceOrder: GroupCallServiceOrder) {
        showProgressBar()
        val reAssignMap: HashMap<String, Any> = HashMap()
        reAssignMap["CallId"] = groupCallServiceOrder.callNumber_ID
        reAssignMap["TechnicianId"] = AppAuth.getInstance().technicianUser.technicianNumber
        viewModel.reassignCall(reAssignMap).observe(this) {
            hideProgressBar()
            it?.let {
                if (it.isHasError) {
                    onReassignError(it, groupCallServiceOrder)
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        GroupCallsRepository.updateGroupCallServiceOrderTechnicianData(
                            groupCallServiceOrder
                        )
                    }
                    showAffirmationBox(
                        resources.getString(R.string.reassignSuccessfulTitle),
                        resources.getString(
                            R.string.reassignSuccessfulMessage,
                            groupCallServiceOrder.callNumber_Code
                        )
                    ) { _, _ ->
                        adapter?.updateFromList(groupCallServiceOrder)
                        adapter?.updateFromFilteredList(groupCallServiceOrder)
                        viewModel.updateGroupCall(groupCallServiceOrder)
                        viewModel.assignedIdCalls.add(groupCallServiceOrder.callNumber_ID)
                        clearFilterText()
                        viewModel.fetchTechnicianActiveServiceCalls(forceUpdate = true)
                        hideKeyboard(this@GroupCallsActivity)
                    }
                }
            }
        }
    }


    private fun manageOneServiceCallResponse(
        genericDataResponse: GenericDataResponse<MutableList<GroupCallServiceOrder>>,
        groupCallServiceOrder: GroupCallServiceOrder
    ) {
        when (genericDataResponse.responseType) {
            RequestStatus.SUCCESS -> {
                genericDataResponse.data?.let {
                    if (it.isEmpty()) {
                        showAffirmationBox(
                            getString(R.string.warning),
                            getString(
                                R.string.the_service_call_was_canceled_or_completed,
                                groupCallServiceOrder.callNumber_Code
                            )
                        )
                        { _, _: Int ->
                            deleteGroupServiceCall(groupCallServiceOrder.callNumber_Code)
                        }
                        return
                    }
                    val currentServiceOrder = it.first()
                    if (currentServiceOrder.canReassignChecker()) {
                        reAssignCall(currentServiceOrder)
                    } else {
                        adapter?.updateFromList(currentServiceOrder)
                        adapter?.updateFromFilteredList(currentServiceOrder)
                        viewModel.updateGroupCall(currentServiceOrder)
                        lifecycleScope.launch(Dispatchers.IO) {
                            GroupCallsRepository.updateGroupServiceCall(currentServiceOrder)
                        }
                        showMessageBox(
                            resources.getString(R.string.somethingWentWrong),
                            getString(R.string.the_service_call_was) + currentServiceOrder.statusCode
                        )
                        clearFilterText()
                    }
                }
            }
            else -> {
                val error = genericDataResponse.onError
                showNetworkErrorDialog(error, this, supportFragmentManager)
            }
        }
    }

    private fun clearFilterText() {
        val searchView: SearchView = searchMenuItem?.actionView as SearchView
        searchView.setQuery("", false);
        searchView.clearFocus();
    }

    private fun onReassignError(
        processingResult: ProcessingResult,
        groupCallServiceOrder: GroupCallServiceOrder
    ) {
        val intent = Intent(this, ReassignMissingPartsActivity::class.java)
        if (processingResult.result == null) {
            val message =
                processingResult.errors[0].errorText + " " + resources.getString(R.string.server_error)
            showMessageBox(resources.getString(R.string.couldnt_reassign_call), message)
            return
        }
        val reassignResponseFormatted: ReassignResponse =
            Gson().fromJson(processingResult.result, ReassignResponse::class.java)
        if (reassignResponseFormatted.unavailableParts != null) {
            val unavailableParts: UnavailableParts = reassignResponseFormatted.unavailableParts
            intent.putExtra(Constants.REASSIGN_UNAVAILABLE_PARTS, unavailableParts)
        }
        if (reassignResponseFormatted.neededParts != null) {
            val neededParts: UnavailableParts = reassignResponseFormatted.neededParts
            intent.putExtra(Constants.REASSIGN_NEEDED_PARTS, neededParts)
        }
        intent.putExtra(Constants.SERVICE_ORDER_ID, groupCallServiceOrder.callNumber_Code)
        startActivity(intent)
    }

    private fun sendContactEmail(serviceOrder: GroupCallServiceOrder) {
        serviceOrder.contactEmail?.let { contactEmail ->
            if (contactEmail.isNotEmpty()) {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "message/rfc822"
                i.putExtra(Intent.EXTRA_EMAIL, arrayOf(serviceOrder.contactEmail))
                i.putExtra(Intent.EXTRA_SUBJECT, "Call# " + serviceOrder.callNumber_Code)
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_email_title)))
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        this@GroupCallsActivity,
                        getString(R.string.no_email_clients),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun clearFilter() {
        filterViewModel.clearFilterData()
    }

    private fun setEnableOptionsMenu(enable: Boolean) {
        searchMenuItem?.let { it.isEnabled = enable }
        for (menuItem in binding.toolbar.menu.iterator()) {
            menuItem.isEnabled = enable
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_calls, menu)
        searchMenuItem = menu.findItem(R.id.search)
        val searchView: SearchView = searchMenuItem?.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                textToSearch = newText
                adapter?.setQuery(newText)
                viewModel.textToSearch = newText
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.loadMapGroupCalls -> {
                if (AppAuth.getInstance().isConnected) {
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra(Constants.EXTRA_QUERY_GROUP_CALL_LIST, viewModel.textToSearch)
                    intent.putExtra(Constants.ORIGIN_MAP_ACTIVITY, Constants.GROUP_CALLS)
                    startActivityForResult(intent, Constants.ACTIVITY_GROUP_CALLS_DETAIL_MAP)
                } else {
                    showUnavailableWhenOfflineMessage()
                }
            }
            R.id.filterGroupCallMenuItem -> {
                goToFilterActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextView) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onContactPhoneClick(serviceOrder: GroupCallServiceOrder) {
        serviceOrder.contactPhone?.let { contactPhone ->
            if (contactPhone.isNotEmpty()) callContact(contactPhone)
        }
    }

    override fun onContactEmailClick(serviceOrder: GroupCallServiceOrder) {
        sendContactEmail(serviceOrder)

    }

    override fun onReassignServiceCall(serviceOrder: GroupCallServiceOrder) {
        showQuestionBoxWithCustomButtons(
            "", resources.getString(R.string.reassignWarning),
            resources.getString(R.string.assignButton), resources.getString(R.string.cancel)
        ) { _: DialogInterface?, _: Int -> tryReAssignCall(serviceOrder) }
    }

    override fun copyContactPhone(serviceOrder: GroupCallServiceOrder) {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", serviceOrder.contactPhone)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this@GroupCallsActivity, getString(R.string.text_copied), Toast.LENGTH_SHORT)
            .show()
    }

    override fun copyContactEmail(serviceOrder: GroupCallServiceOrder) {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", serviceOrder.contactEmail)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this@GroupCallsActivity, getString(R.string.text_copied), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDetailedGroupClick(serviceOrder: GroupCallServiceOrder) {
        val intent = Intent(this@GroupCallsActivity, GroupCallDetailsActivity::class.java)
        intent.putExtra(Constants.SERVICE_ORDER_ID, serviceOrder.callNumber_ID)
        startActivityForResult(intent, Constants.ACTIVITY_GROUP_CALLS_DETAIL)
    }
}