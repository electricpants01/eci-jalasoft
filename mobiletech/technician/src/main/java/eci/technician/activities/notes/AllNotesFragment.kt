package eci.technician.activities.notes

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.adapters.NotesAdapter
import eci.technician.databinding.FragmentAllNotesBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.ErrorHelper.RequestError
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.INotesListener
import eci.technician.interfaces.INotesNavigation
import eci.technician.models.ProcessingResult
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.postModels.DeleteNotePostModel
import eci.technician.models.serviceCallNotes.ui.ServiceCallNoteUIModel
import eci.technician.repository.DatabaseRepository
import eci.technician.repository.ServiceCallNotesRepository
import kotlinx.android.synthetic.main.fragment_all_notes.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class AllNotesFragment : Fragment(), INotesListener {

    companion object {
        const val EXTRA_SHOW_LOADING_ERROR = "extra_show_loading_error"
    }

    private var _binding: FragmentAllNotesBinding? = null
    private val binding
        get() = _binding!!

    private val notesAdapter =
        NotesAdapter(listOf(), this, AppAuth.getInstance().technicianUser.isAllowServiceCallNotes)
    private val viewModel: ServiceCallNotesViewModel by activityViewModels()
    private var shouldShowLoadingError = true
    private var isSwipeRefreshing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.NotesActivity.SHOW_ALL_NOTES_ACTION)
        binding.toolbar.title = getString(R.string.notes)
        shouldShowLoadingError = arguments?.getBoolean(EXTRA_SHOW_LOADING_ERROR, true) ?: true
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        setRecycler()
        retrieveServiceCallNotes()
        observeNotesFromDB()
        setupEmptyMessage()

        binding.refreshNotes.setOnRefreshListener {
            isSwipeRefreshing = true
            showProgressBar()
            setRecycler()
            retrieveServiceCallNotes()
            observeNotesFromDB()
            setupEmptyMessage()
            refreshNotes.isRefreshing = false
        }
    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recyclerNotes.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.recyclerNotes.visibility = View.VISIBLE
    }

    private fun observeNotesFromDB() {
        DatabaseRepository.getInstance().getServiceCallNotesByCallId(viewModel.callId)
            .observe(viewLifecycleOwner, Observer {
                updateNotesList(it.toMutableList())
            })
    }

    private fun setupEmptyMessage() {
        if (binding.recyclerNotes.adapter?.itemCount ?: 0 == 0) {
            binding.emptyLinearLayout.visibility = View.VISIBLE
        } else {
            binding.emptyLinearLayout.visibility = View.GONE
        }
    }

    private fun retrieveServiceCallNotes() {
        if (!viewModel.notesAlreadyLoaded) {
            showProgressBar()
        }
        lifecycleScope.launch {
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .getServiceCallNotesByCallId(viewModel.callId, requireContext())
                .observe(viewLifecycleOwner, Observer { genericDataResponse ->
                    viewModel.notesAlreadyLoaded = true
                    hideProgressBar()
                    when (genericDataResponse.responseType) {
                        RequestStatus.SUCCESS -> {
                            binding.refreshNotes.isRefreshing = false
                        }
                        RequestStatus.ERROR -> {
                            genericDataResponse.onError?.let { showMessageOnRequestError(it) }
                        }
                        RequestStatus.NOT_CONNECTED -> {
                            showMessageOnNotConnected()
                        }
                        else -> {
                            genericDataResponse.onError?.let { showMessageOnRequestError(it) }

                        }
                    }
                    isSwipeRefreshing = false
                })
        }
    }

    private fun showMessageOnNotConnected() {
        if (!shouldShowLoadingError && !isSwipeRefreshing) return
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.can_not_retrieve_notes),
                getString(R.string.offline_warning)
            )
        }
    }

    private fun showMessageOnRequestError(requestError: RequestError) {
        requireActivity().let { pActivity ->
            if (pActivity is BaseActivity) {
                pActivity.showNetworkErrorDialog(requestError, requireContext(), childFragmentManager)
            }
        }
    }

    private fun updateNotesList(listOfNotes: MutableList<ServiceCallNoteEntity>) {
        lifecycleScope.launch {
            val listOfUINotes =
                listOfNotes.map { note -> ServiceCallNoteUIModel.createUIModelFromEntity(note) }
            withContext(Dispatchers.IO) {
                listOfUINotes.forEach {
                    val noteType = ServiceCallNotesRepository.getNoteTypeByNoteTypeId(it.noteTypeId)
                    it.noteTypeString = noteType?.noteType ?: ""
                    it.isEditable = noteType?.isEditable ?: false
                }
                withContext(Dispatchers.Main) {
                    notesAdapter.listOfNotes = listOfUINotes
                    notesAdapter.notifyDataSetChanged()
                    setupEmptyMessage()
                }
            }
        }
    }

    private fun setRecycler() {
        binding.recyclerNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotes.setHasFixedSize(true)
        binding.recyclerNotes.adapter = notesAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (AppAuth.getInstance().technicianUser.isAllowServiceCallNotes) {
            inflater.inflate(R.menu.menu_notes_list_options, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                return true
            }
            R.id.add_note_action -> {
                goToAddNote()
                return true
            }
        }
        return super.onOptionsItemSelected(item)

    }

    private fun goToAddNote() {
        activity?.let {
            (it as INotesNavigation).goToCreateNoteFragment()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onTapEditNote(noteId: Long) {
        if (!AppAuth.getInstance().isConnected) {
            (requireActivity() as NotesActivity).showUnavailableWhenOfflineMessage()
            return
        }
        viewModel.currentNoteDetailId = noteId
        activity?.let {
            (it as INotesNavigation).goToEditNoteFragment()
        }
    }

    override fun onTapDeleteNote(noteDetailId: Long, customUUID: String) {
        if (!AppAuth.getInstance().isConnected) {
            (requireActivity() as NotesActivity).showUnavailableWhenOfflineMessage()
            return
        }
        if (!AppAuth.getInstance().technicianUser.isAllowServiceCallNotes) {
            showUnAuthorizedMessage()
            return
        }
        showDeleteConfirmation {
            if (!AppAuth.getInstance().isConnected) {
                (requireActivity() as NotesActivity).showUnavailableWhenOfflineMessage()
                return@showDeleteConfirmation
            }
            deleteNote(noteDetailId, customUUID)
        }
    }

    private fun showUnAuthorizedMessage() {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.somethingWentWrong),
                getString(R.string.do_not_have_enough_permissions)
            )
        }
    }

    private fun showDeleteConfirmation(onTapDelete: () -> Unit) {
        val alert = AlertDialog.Builder(requireContext())
        alert.setTitle(getString(R.string.delete_note_title_dialog))
        alert.setMessage(getString(R.string.delete_note_message))
        alert.setPositiveButton(R.string.delete) { _, _ ->
            onTapDelete.invoke()
        }
        alert.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        alert.show()
    }

    fun deleteNote(noteDetailId: Long, customUUID: String) {
        showProgressBar()
        val deleteNotePostModel = DeleteNotePostModel(noteDetailId)
        lifecycleScope.launch {
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .deleteServiceCallNoteByNoteDetailId(deleteNotePostModel, requireContext())
                .observe(viewLifecycleOwner, Observer { genericDataResponse ->
                    hideProgressBar()
                    when (genericDataResponse.responseType) {
                        RequestStatus.SUCCESS -> {
                            manageDataResponseForDelete(
                                genericDataResponse.data,
                                noteDetailId,
                                customUUID
                            )
                        }
                        RequestStatus.ERROR -> {
                            genericDataResponse.onError?.let { showMessageOnRequestError(it) }
                        }
                        else -> {
                            genericDataResponse.onError?.let { showMessageOnRequestError(it) }
                        }
                    }
                })
        }
    }

    private fun manageDataResponseForDelete(
        data: ProcessingResult?,
        noteDetailId: Long,
        customUUID: String
    ) {
        data?.let {
            viewModel.deleteNote(customUUID) {
                Toast.makeText(requireContext(), getString(R.string.deleted), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

}