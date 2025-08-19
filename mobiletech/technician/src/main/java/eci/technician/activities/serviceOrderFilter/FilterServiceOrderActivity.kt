package eci.technician.activities.serviceOrderFilter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.R
import eci.technician.databinding.ActivityFilterServiceOrderBinding
import eci.technician.models.filters.FilterCriteria
import eci.technician.viewmodels.OrderFragmentViewModel

class FilterServiceOrderActivity : AppCompatActivity() {

    companion object {
        const val TAG = "FilterServiceOrderActivity"
        const val EXCEPTION = "Exception"
        const val IS_GROUP_FILTER = "isGroupFilter"
    }

    private val viewModel: OrderFragmentViewModel by viewModels()
    lateinit var binding: ActivityFilterServiceOrderBinding
    private val rotateForward by lazy {
        AnimationUtils.loadAnimation(this, R.anim.rotate_arrow_forward)
    }
    private val rotateBackward by lazy {
        AnimationUtils.loadAnimation(this, R.anim.rotate_arrow_backward)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_filter_service_order)
        viewModel.isGroupFilter = intent.getBooleanExtra(IS_GROUP_FILTER, false)
        binding.lifecycleOwner = this
        FirebaseAnalytics.getInstance(this).logEvent("FILTER_SERVICE_ORDER_ACTIVITY", Bundle())
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title =
            if (viewModel.isGroupFilter) getString(R.string.filter_group_calls) else getString(R.string.filter_service_calls)
        binding.applyFilterButton.setOnClickListener {
            applyFilters()
        }

        if (viewModel.isGroupFilter) {
            viewModel.fetchTechniciansListFromDB()
            viewModel.fetchGroupsForFilter()
        } else {
            hideTechnicianViews()
            hideGroupViews()
        }
        setTechnicianListRecycler()
        setGroupListRecycler()
        setCallDateRecycler()
        setSortDateRecycler()
        setCallTypesRecycler()
        setCallPrioritiesRecycler()
        setCallStatusRecycler()

        viewModel.fetchPriorityListFromDB()
        viewModel.fetchCallTypeListFromDB()
        viewModel.fetchCallStatusList()
        viewModel.fetchDateList()
        viewModel.fetchSortItemsList()
        viewModel.fetchFilterCriteriaFromDB()

        setObservers()
        setShowHideFilters()
    }

    private fun setGroupListRecycler() {
        binding.recyclerGroups.layoutManager = LinearLayoutManager(this)
        binding.recyclerGroups.setHasFixedSize(true)
        binding.recyclerGroups.adapter = TechnicianGroupsAdapter(mutableListOf())
    }

    private fun hideGroupViews() {
        binding.containerFilterGroupsTitle.visibility = View.GONE
        binding.filterGroupContainerCardView.visibility = View.GONE
    }

    private fun hideTechnicianViews() {
        binding.containerFilterTechnicianTitle.visibility = View.GONE
        binding.filterCallTechnicianContainerCardView.visibility = View.GONE
    }

    private fun setTechnicianListRecycler() {
        binding.recyclerTechnician.layoutManager = LinearLayoutManager(this)
        binding.recyclerTechnician.setHasFixedSize(true)
        binding.recyclerTechnician.adapter = CallTechnicianAdapter(mutableListOf())
    }

    private fun setSortDateRecycler() {
        binding.recyclerSortDate.layoutManager = LinearLayoutManager(this)
        binding.recyclerSortDate.setHasFixedSize(true)
        binding.recyclerSortDate.adapter = SortCallsAdapter(listOf())
    }


    private fun setShowHideFilters() {
        binding.filterGroupsActionButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.filterGroupContainerCardView)
        }

        binding.filterCallTechnicianActionButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.filterCallTechnicianContainerCardView)
        }

        binding.sortByDateButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.sortByDateContainerCardView)
        }

        binding.filterDateActionButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.filterDateContainerCardView)
        }

        binding.filterCallStatusActionButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.filterCallStatusContainerCardView)
        }

        binding.filterCallTypeActionButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.filterCallTypeContainerCardView)
        }

        binding.filterCallPriorityActionButtonShowHide.setOnClickListener {
            onTapShowHideView(it, binding.filterCallPriorityContainerCardView)
        }
    }

    private fun onTapShowHideView(tapView: View, recyclerContainer: View) {
        recyclerContainer.visibility =
            if (recyclerContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        tapView.startAnimation(if (recyclerContainer.visibility == View.VISIBLE) rotateBackward else rotateForward)
    }

    private fun setObservers() {
        setPriorityObserver()
        setCallTypeObserver()
        setCallStatusObserver()
        setCallDateObserver()
        setSortDateObserver()
        observeFilterCriteria()
        if (viewModel.isGroupFilter) {
            setTechnicianObserver()
            setGroupsObserver()
        }
    }

    private fun setGroupsObserver() {
        viewModel.groupList.observe(this){
            if(it.size < 2){
                hideGroupViews()
            }else {
                binding.recyclerGroups.adapter = TechnicianGroupsAdapter(it.toMutableList())
            }
        }
    }


    private fun setTechnicianObserver() {
        viewModel.technicianFilterList.observe(this) {
            binding.recyclerTechnician.adapter = CallTechnicianAdapter(it.toMutableList())
        }
    }

    private fun observeFilterCriteria() {
        viewModel.filterCriteria.observe(this) {
            openSavedFilters(it)
        }
    }

    private fun openSavedFilters(filterCriteria: FilterCriteria) {
        if (viewModel.isGroupFilter) {
            binding.filterGroupContainerCardView.showView(filterCriteria.isCallTechnicianFilterOpen)
            binding.filterCallTechnicianActionButtonShowHide.startAnimation(if (filterCriteria.isCallTechnicianFilterOpen) rotateBackward else rotateForward)

            binding.filterGroupContainerCardView.showView(filterCriteria.isGroupFilterOpen)
            binding.filterGroupsActionButtonShowHide.startAnimation(if (filterCriteria.isGroupFilterOpen) rotateBackward else rotateForward)
        }

        binding.sortByDateContainerCardView.showView(filterCriteria.isSortByDateOpen)
        binding.sortByDateButtonShowHide.startAnimation(if (filterCriteria.isSortByDateOpen) rotateBackward else rotateForward)

        binding.filterDateContainerCardView.showView(filterCriteria.isDateFilterOpen)
        binding.filterDateActionButtonShowHide.startAnimation(if (filterCriteria.isDateFilterOpen) rotateBackward else rotateForward)

        binding.filterCallStatusContainerCardView.showView(filterCriteria.isCallStatusFilterOpen)
        binding.filterCallStatusActionButtonShowHide.startAnimation(if (filterCriteria.isCallStatusFilterOpen) rotateBackward else rotateForward)

        binding.filterCallTypeContainerCardView.showView(filterCriteria.isCallTypeFilterOpen)
        binding.filterCallTypeActionButtonShowHide.startAnimation(if (filterCriteria.isCallTypeFilterOpen) rotateBackward else rotateForward)

        binding.filterCallPriorityContainerCardView.showView(filterCriteria.isCallPriorityFilterOpen)
        binding.filterCallPriorityActionButtonShowHide.startAnimation(if (filterCriteria.isCallPriorityFilterOpen) rotateBackward else rotateForward)
    }

    private fun CardView.showView(show:Boolean){
        this.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setSortDateObserver() {
        viewModel.sortList.observe(this) { list ->
            binding.recyclerSortDate.adapter = SortCallsAdapter(list)
        }
    }

    private fun setCallDateObserver() {
        viewModel.dateFilter.observe(this) { list ->
            binding.recyclerDate.adapter = FilterDateAdapter(list)
        }
    }

    private fun setCallDateRecycler() {
        binding.recyclerDate.layoutManager = LinearLayoutManager(this)
        binding.recyclerDate.setHasFixedSize(true)
        binding.recyclerDate.adapter = FilterDateAdapter(listOf())

    }

    private fun setCallStatusRecycler() {
        binding.recyclerStatus.layoutManager = LinearLayoutManager(this)
        binding.recyclerStatus.setHasFixedSize(true)
        binding.recyclerStatus.adapter = FilterStatusAdapter(mutableListOf())
    }

    private fun setCallStatusObserver() {
        viewModel.statusList.observe(this) { list ->
            binding.recyclerStatus.adapter = FilterStatusAdapter(list.toMutableList())
        }
    }

    private fun setCallTypeObserver() {
        viewModel.callTypeList.observe(this) { list ->
            binding.recyclerCallTypes.adapter =
                CallTypesTechnicianAdapter(list.toMutableList())
        }
    }

    private fun setCallTypesRecycler() {
        binding.recyclerCallTypes.layoutManager = LinearLayoutManager(this)
        binding.recyclerCallTypes.setHasFixedSize(true)
        binding.recyclerCallTypes.adapter = CallTypesTechnicianAdapter(mutableListOf())
    }

    private fun setCallPrioritiesRecycler() {
        binding.recyclerPriorities.layoutManager = LinearLayoutManager(this)
        binding.recyclerPriorities.itemAnimator = null
        binding.recyclerPriorities.adapter = FilterPriorityAdapter()
    }

    private fun setPriorityObserver() {
        viewModel.priorityList.observe(this) {
            (binding.recyclerPriorities.adapter as FilterPriorityAdapter).submitList(it)
        }
    }

    override fun onResume() {
        super.onResume()
        setObservers()
    }

    private fun clearFilter() {
        val adapterList: List<FilterAdapter> = listOf(
            binding.recyclerDate.adapter as FilterAdapter,
            binding.recyclerStatus.adapter as FilterAdapter,
            binding.recyclerPriorities.adapter as FilterAdapter,
            binding.recyclerCallTypes.adapter as FilterAdapter,
            binding.recyclerSortDate.adapter as SortCallsAdapter,
            binding.recyclerTechnician.adapter as CallTechnicianAdapter,
            binding.recyclerGroups.adapter as TechnicianGroupsAdapter
        )
        adapterList.forEach {
            it.unCheckAllItems()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_filter_group_calls, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                saveOpenFilters()
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                finish()
                return true
            }
            R.id.clearFilterMenuButton -> {
                clearFilter()
                viewModel.clearFilterData()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyFilters() {
        saveOpenFilters()
        viewModel.saveCheckedFilters(
            (binding.recyclerDate.adapter as FilterDateAdapter).list,
            (binding.recyclerSortDate.adapter as SortCallsAdapter).list,
            (binding.recyclerCallTypes.adapter as CallTypesTechnicianAdapter).callTypesList,
            (binding.recyclerStatus.adapter as FilterStatusAdapter).list,
            (binding.recyclerPriorities.adapter as FilterPriorityAdapter).currentList,
            (binding.recyclerTechnician.adapter as CallTechnicianAdapter).callTechnicianList,
            (binding.recyclerGroups.adapter as TechnicianGroupsAdapter).groupList
        )
        val intent = Intent()
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun saveOpenFilters() {
        val isSortOpen = binding.sortByDateContainerCardView.visibility == View.VISIBLE
        val isWhenOpen = binding.filterDateContainerCardView.visibility == View.VISIBLE
        val isStatusOpen = binding.filterCallStatusContainerCardView.visibility == View.VISIBLE
        val isPriorityOpen = binding.filterCallPriorityContainerCardView.visibility == View.VISIBLE
        val isCallTypeOpen = binding.filterCallTypeContainerCardView.visibility == View.VISIBLE
        val isGroupOpen = binding.filterGroupContainerCardView.visibility == View.VISIBLE
        val isTechniciansOpen = binding.filterCallTechnicianContainerCardView.visibility == View.VISIBLE
        viewModel.saveOpenFilters(
            isSortOpen,
            isWhenOpen,
            isStatusOpen,
            isPriorityOpen,
            isCallTypeOpen,
            isGroupOpen,
            isTechniciansOpen
        )
    }


    override fun onBackPressed() {
        super.onBackPressed()
        saveOpenFilters()
    }


}