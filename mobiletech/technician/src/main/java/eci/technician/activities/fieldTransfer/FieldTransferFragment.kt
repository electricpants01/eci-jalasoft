package eci.technician.activities.fieldTransfer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.activities.NewPartRequestActivity
import eci.technician.databinding.FragmentFieldTransferBinding
import eci.technician.helpers.AppAuth
import eci.technician.models.field_transfer.PartRequestTransfer


class FieldTransferFragment : Fragment(), FieldTransferRequestListener {

    private var _binding: FragmentFieldTransferBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: FieldTransferViewModel by activityViewModels()
    lateinit var adapter: FieldTransferItemsAdapter
    var dialog: AlertDialog? = null

    private val startPartRequestForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.fetchMyPartRequest()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldTransferBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppAuth.getInstance().requestHasBeenSeen = true
        setupRecycler()
        setupEmptyList()
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchMyPartRequest() }
        binding.btnNewTransferRequest.setOnClickListener { openNewPartRequestActivity() }
    }

    private fun openNewPartRequestActivity() {
        val partRequestIntent = Intent(requireContext(), NewPartRequestActivity::class.java)
        startPartRequestForResult.launch(partRequestIntent)
    }

    private fun setupEmptyList() {
        binding.emptyListIncluded.mainText.text = getString(R.string.field_transfer)
        binding.emptyListIncluded.secondaryText.text =
            getString(R.string.there_are_no_items_in_field_transfer)
    }

    override fun onResume() {
        super.onResume()
        setupObservers()
    }

    private fun setupObservers() {
        observeList()
        observeLoading()
        observeEmptyList()
        observeSwipe()
        observeNetworkError()
    }

    private fun observeEmptyList() {
        viewModel.isEmpty.observe(viewLifecycleOwner) {
            binding.emptyListIncluded.emptyListContainer.visibility =
                if (it) View.VISIBLE else View.GONE
        }
    }

    private fun observeLoading() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loaderIncluded.progressBarContainer.visibility =
                if (it) View.VISIBLE else View.GONE
        }
    }

    private fun observeList() {
        viewModel.fieldTransferList.observe(viewLifecycleOwner) {
            adapter.submitList(it ?: listOf())
        }
    }

    private fun observeSwipe() {
        viewModel.swipeLoading.observe(viewLifecycleOwner, {
            if (binding.swipeRefresh.isRefreshing && it) {
                //do nothing
            } else {
                binding.swipeRefresh.isRefreshing = it
            }
        })
    }

    private fun setupRecycler() {
        adapter = FieldTransferItemsAdapter(this)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recycler.adapter = adapter
    }

    override fun onAcceptClick(item: PartRequestTransfer) {
        showDialog(
            getString(R.string.title_dialog_accept_request),
            getString(R.string.message_dialog_accept_request),
            getString(R.string.accept),
            true,
            {
                viewModel.acceptTransferOrder(item)
            }
        )

    }

    override fun onRejectClick(item: PartRequestTransfer) {
        showDialog(
            getString(R.string.title_dialog_reject_request),
            getString(R.string.message_dialog_reject_request),
            getString(R.string.reject),
            true,
            {
                viewModel.rejectTransferOrder(item)
            }
        )
    }

    override fun onCancelClick(item: PartRequestTransfer) {
        showDialog(
            getString(R.string.title_dialog_delete_request),
            getString(R.string.message_dialog_delete_request),
            getString(R.string.delete),
            true,
            {
                viewModel.cancelTransferOrder(item)
            }
        )
    }

    private fun showDialog(
        title: String,
        description: String,
        positiveText: String = getString(R.string.ok),
        showNegative: Boolean = false,
        onPositiveTap: () -> Unit = {},
        onNegativeTap: () -> Unit = {}
    ) {
        if (dialog != null) return
        val builderDialog = AlertDialog.Builder(requireContext())
        builderDialog.setTitle(title)
        builderDialog.setMessage(description)
        builderDialog.setPositiveButton(positiveText) { _, _ ->
            onPositiveTap.invoke()
        }
        if (showNegative) {
            builderDialog.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                onNegativeTap.invoke()
            }
        }
        builderDialog.setOnDismissListener {
            dialog = null
        }
        dialog = builderDialog.create()
        dialog?.show()

    }

    private fun observeNetworkError() {
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

}