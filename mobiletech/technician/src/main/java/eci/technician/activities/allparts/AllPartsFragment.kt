package eci.technician.activities.allparts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.FragmentAllPartsBinding
import eci.technician.helpers.ErrorHelper.RequestError


class AllPartsFragment : Fragment() {

    private var _binding: FragmentAllPartsBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: AllPartsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.parts_text)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        setSwipeRefresh()
        observeSwipeForUsedParts()
        observeNetworkError()
    }

    private fun observeNetworkError() {
        viewModel.networkError.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let { pair ->
                requireActivity().let { pActivity ->
                    if (pActivity is BaseActivity) {
                        pActivity.showNetworkErrorDialog(
                            pair,
                            requireContext(),
                            childFragmentManager
                        )
                    }
                }
            }
        }
    }

    private fun observeSwipeForUsedParts() {
        viewModel.swipeUsedParts.observe(viewLifecycleOwner) { showSwipe ->
            binding.swipeRefresh.isRefreshing = showSwipe
        }
    }


    private fun setSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchUsedPartsByCallIdFlow()
        }
    }

    private fun showMessageOnRequestError(requestError: RequestError) {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.somethingWentWrong),
                requestError.description
            )
        }
    }

    private fun showMessageOnNotConnected() {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showUnavailableWhenOfflineMessage()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}

