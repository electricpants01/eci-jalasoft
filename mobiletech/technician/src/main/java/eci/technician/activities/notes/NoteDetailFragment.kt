package eci.technician.activities.notes

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.FragmentNoteDetailBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.api.retroapi.GenericDataResponse
import eci.technician.helpers.api.retroapi.RequestStatus
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.INoteTypesListener
import eci.technician.interfaces.INotesNavigation
import eci.technician.models.ProcessingResult
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity
import eci.technician.repository.ServiceCallNotesRepository
import eci.technician.workers.OfflineManager
import kotlinx.android.synthetic.main.fragment_call_types.*
import kotlinx.android.synthetic.main.fragment_call_types.toolbar
import kotlinx.android.synthetic.main.fragment_note_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class NoteDetailFragment : Fragment(), INoteTypesListener {

    companion object {
        const val EXTRA_IS_CREATING_NOTE = "extra_creating_note"
        const val EXTRA_IS_EDITING_NOTE = "extra_editing_note"
    }

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: ServiceCallNotesViewModel by activityViewModels()
    private var localMenu: Menu? = null
    private var isEditable: Boolean = false
    private var originalMessage: String = ""
    private val emptyType = "None"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.NotesActivity.CREATE_NOTE_ACTION)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            goBack()
        }
    }

    private fun goToEditMode() {
        setEditNoteView()
        disableNoteEdition()
        showEditOption()
        viewModel.isEditing = false
        viewModel.isUpdating = false
        val imm: InputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(activity?.window?.decorView?.windowToken, 0)
    }

    private fun goBack() {
        if (!viewModel.isEditing && !viewModel.isCreating) {
            goToAllNotesList()
            return
        }
        if (!hasDataUnsaved()) {
            goToPreviousScreen()
            return
        }
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.warning))
            .setMessage(R.string.cancel_notes)
            .setPositiveButton(R.string.confirm) { _, _ ->
                goToPreviousScreen()
            }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()

    }

    private fun goToPreviousScreen() {
        if (viewModel.isEditing) {
            goToEditMode()
        } else {
            goToAllNotesList()
        }
    }

    private fun hasDataUnsaved(): Boolean {
        val currentNoteText = binding.noteEditText.text.toString()
        val currentTypeText = binding.noteTypeDescription.text.toString()
        if (viewModel.isCreating) {
            if (currentNoteText.isEmpty() && currentTypeText == emptyType)
                return false
        }
        if (currentNoteText == viewModel.originalNoteToUpdate &&
            currentTypeText == viewModel.originalNoteType?.noteType
        ) {
            return false
        }
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.noteTypeSelectorCardView.setOnClickListener {
            prepareNoteTypeDialog()
        }
        binding.toolbar.title = getString(R.string.note)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        viewModel.isCreating = arguments?.getBoolean(EXTRA_IS_CREATING_NOTE, false) ?: false

        if (viewModel.isUpdating) {
            continueUpdating()
            return
        }

        if (viewModel.isCreating) {
            setCreateNoteView()
        } else {
            setEditNoteView()
        }
    }

    private fun continueUpdating() {
        enableNoteEdition()
        viewModel.serviceCallNoteEntity?.let {
            viewModel.originalNoteToUpdate = it.note
            binding.noteEditText.setText(it.note)
            binding.createDateText.text = getString(
                R.string.created_on,
                ServiceCallNoteEntity.parseNoteDate(it.createDate)
            )
            binding.lastUpdateDateText.text = getString(
                R.string.updated_on,
                ServiceCallNoteEntity.parseNoteDate(it.lastUpdate)
            )
            binding.noteTypeDescription.text =
                viewModel.noteTypeSelected?.noteType ?: viewModel.originalNoteType?.noteType
            originalMessage = it.note
        }
    }

    private fun prepareNoteTypeDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            var listTypes = ServiceCallNotesRepository.getAllServiceCallNoteTypes()
            if (viewModel.isEditing) {
                listTypes = listTypes.filter { it.isEditable == true }
            }
            withContext(Dispatchers.Main) {
                NoteTypeFragment(listTypes, this@NoteDetailFragment).show(
                    childFragmentManager,
                    NoteTypeFragment.TAG
                )
            }
        }
    }

    private fun setCreateNoteView() {
        binding.dateContainer.visibility = View.GONE
        viewModel.setBasicCreateNoteModel()
    }

    private fun setEditNoteView() {
        lifecycleScope.launch {
            val note = ServiceCallNotesRepository.getNoteById(viewModel.currentNoteDetailId)
            note?.let {
                val noteType = ServiceCallNotesRepository.getNoteTypeByNoteTypeId(note.noteTypeId)
                viewModel.noteTypeSelected = noteType
                viewModel.currentNoteToEditUUID = note.customUUID
                viewModel.originalNoteType = noteType
            }
            isEditable =
                ServiceCallNotesRepository.canEditNoteByNoteDetailId(viewModel.currentNoteDetailId)
            withContext(Dispatchers.Main) {
                if (note == null) {
                    goToAllNotesList()
                    return@withContext
                }
                note?.let {
                    viewModel.serviceCallNoteEntity = it
                    viewModel.setBasicUpdateNoteModel(it)
                    viewModel.originalNoteToUpdate = it.note
                    binding.noteEditText.setText(it.note)
                    binding.createDateText.text = getString(
                        R.string.created_on,
                        ServiceCallNoteEntity.parseNoteDate(it.createDate)
                    )
                    binding.lastUpdateDateText.text = getString(
                        R.string.updated_on,
                        ServiceCallNoteEntity.parseNoteDate(it.lastUpdate)
                    )
                    binding.noteTypeDescription.text =
                        viewModel.noteTypeSelected?.noteType ?: getString(R.string.none)
                    originalMessage = it.note
                }
                disableNoteEdition()

            }
        }
    }

    private fun disableNoteEdition() {
        if (!isEditable) {
            binding.apply {
                containerReadOnly.visibility = View.VISIBLE
                noteTypeSelectorCardView.setOnClickListener {
                    Toast.makeText(
                        context,
                        getString(R.string.this_note_is_not_editable),
                        Toast.LENGTH_LONG
                    ).show()
                }
                noteEditText.setOnLongClickListener {
                    Toast.makeText(
                        context,
                        getString(R.string.this_note_is_not_editable),
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }
            }

        } else {
            binding.noteTypeSelectorCardView.isEnabled = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.noteEditText.focusable = View.NOT_FOCUSABLE
        } else {
            binding.noteEditText.isEnabled = false
        }
        binding.noteEditText.isFocusableInTouchMode = false
        binding.noteTypeTitle.text = getString(R.string.type)
        binding.arrow.visibility = View.INVISIBLE
    }

    private fun enableNoteEdition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.noteEditText.focusable = View.FOCUSABLE
        } else {
            binding.noteEditText.isEnabled = true
        }
        binding.noteEditText.isFocusableInTouchMode = true
        binding.noteTypeSelectorCardView.isEnabled = true
        binding.noteTypeTitle.text = getString(R.string.select_type)
        binding.arrow.visibility = View.VISIBLE
        showSaveOption()
        viewModel.isUpdating = true

    }

    private fun checkSaveButtonAvailability() {
        val saveMenuItem = localMenu?.findItem(R.id.action_save_note)
        saveMenuItem?.isVisible =
            viewModel.originalNoteToUpdate != binding.noteEditText.text.toString() ||
                    viewModel.originalNoteType?.noteTypeId != viewModel.noteTypeSelected?.noteTypeId

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        lifecycleScope.launch(Dispatchers.IO) {
            val canEditNote =
                ServiceCallNotesRepository.canEditNoteByNoteDetailId(viewModel.currentNoteDetailId)
            if (canEditNote || viewModel.isCreating) {
                withContext(Dispatchers.Main) {
                    localMenu = menu
                    inflater.inflate(R.menu.menu_notes_detail, menu)
                    if (viewModel.isCreating) {
                        menu.findItem(R.id.action_save_note).title = getString(R.string.save)
                    } else {
                        if (isEditable)
                            showEditOption()
                    }
                }
            }
        }
    }

    private fun showEditOption() {
        val saveMenuItem = localMenu?.findItem(R.id.action_save_note)
        saveMenuItem?.title = getString(R.string.edit)
        saveMenuItem?.isVisible = true
    }

    private fun showSaveOption() {
        val saveMenuItem = localMenu?.findItem(R.id.action_save_note)
        saveMenuItem?.title = getString(R.string.save)

        saveMenuItem?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                goBack()
                return true
            }
            R.id.action_save_note -> {
                save()
                return true
            }

        }
        return super.onOptionsItemSelected(item)

    }

    private fun save() {
        if (viewModel.isCreating) {
            saveCreateNote()
            return
        }
        if (!viewModel.isEditing) {
            enableNoteEdition()
            viewModel.isEditing = true
            return
        } else {
            saveUpdateNote()
            return
        }
    }

    private fun saveCreateNote() {

        if (!AppAuth.getInstance().technicianUser.isAllowServiceCallNotes) {
            showUnAuthorizedMessage()
            return
        }

        if (binding.noteTypeDescription.text.toString() == emptyType) {
            showNoteTypeRequiredMessage()
            return
        }

        if (binding.noteEditText.text.toString().isEmpty()) {
            showNoteRequiredMessage()
            return
        }

        val customUUID = UUID.randomUUID().toString()
        viewModel.createNoteModelToPost.note = binding.noteEditText.text.toString()
        viewModel.createLocalNote(Date(), customUUID) {
            OfflineManager.retryNotesWorker(requireContext())
            goToAllNotesList()
        }

    }

    private fun showNoteRequiredMessage() {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.note),
                getString(R.string.can_not_create_empty_note)
            )
        }
    }

    private fun showNoteRequiredUpdateMessage() {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.note),
                getString(R.string.can_not_update_empty_note)
            )
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


    private fun saveUpdateNote() {
        if (!AppAuth.getInstance().isConnected) {
            (requireActivity() as NotesActivity).showUnavailableWhenOfflineMessage()
            return
        }

        if (!AppAuth.getInstance().technicianUser.isAllowServiceCallNotes) {
            showUnAuthorizedMessage()
            return
        }

        if (viewModel.noteTypeSelected == null) {
            showNoteTypeRequiredMessage()
            return
        }

        if (binding.noteEditText.text.toString().isEmpty()) {
            binding.noteEditText.setText(originalMessage)
            goBack()
            return
        }

        viewModel.updateNoteModelToPost.note = binding.noteEditText.text.toString()
        viewModel.updateNoteModelToPost.createDate = Date()
        viewModel.updateNoteModelToPost.noteTypeId =
            viewModel.noteTypeSelected?.noteTypeId ?: -1
        lifecycleScope.launch {
            showProgressBar()
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .updateServiceCallNote(
                    viewModel.updateNoteModelToPost,
                    requireContext(),
                    viewModel.currentNoteToEditUUID
                )
                .observe(viewLifecycleOwner, {
                    hideProgressBar()
                    when (it.responseType) {
                        RequestStatus.SUCCESS -> {
                            lifecycleScope.launch {
                                RetrofitRepository.RetrofitRepositoryObject.getInstance()
                                    .getServiceCallNotesByCallId(viewModel.callId, requireContext())
                                    .observe(this@NoteDetailFragment) {
                                        goToEditMode()
                                    }
                            }
                        }
                        RequestStatus.ERROR -> {
                            showErrorMessage(it)
                        }
                        else -> {
                            showErrorMessage(it)
                        }
                    }
                })
        }

    }

    private fun showErrorMessage(genericDataResponse: GenericDataResponse<ProcessingResult>?) {
        genericDataResponse?.let {
            requireActivity().let { pActivity ->
                if (pActivity is BaseActivity){
                    pActivity.showNetworkErrorDialog(it.onError, requireContext(), childFragmentManager)
                }
            }
        }

    }

    private fun showProgressBar() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.mainContainer.visibility = View.GONE
    }

    private fun hideProgressBar() {
        binding.progressBarContainer.visibility = View.GONE
        binding.mainContainer.visibility = View.VISIBLE
    }

    private fun showNoteTypeRequiredMessage() {
        if (requireActivity() is BaseActivity) {
            (requireActivity() as BaseActivity).showMessageBox(
                getString(R.string.note_type_is_required),
                getString(R.string.select_a_note_type)
            )
        }
    }

    private fun goToAllNotesList() {
        activity?.let {
            (it as INotesNavigation).goToAllNotesFragment(false)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onTapNoteType(noteType: ServiceCallNoteTypeEntity) {
        viewModel.noteTypeSelected = noteType
        binding.noteTypeDescription.text = noteType.noteType
        checkSaveButtonAvailability()
    }


}