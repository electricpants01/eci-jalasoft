package eci.technician.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.repository.ServiceCallNotesRepository
import eci.technician.workers.OfflineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class SyncViewModel : ViewModel() {

    var isAttachmentEmpty = false
    var isServiceCallListEmpty = false
    var shouldRefreshServiceCallList = false
    var isNotesEmpty = false


    fun removeSelectedActions(callNumberCode: String, context: Context) {
        viewModelScope.launch {
            IncompleteRequestsRepository.deleteIncompleteRequestByCallNumberCode(callNumberCode)
            OfflineManager.retryWorker(context)
            shouldRefreshServiceCallList = true
        }
    }

    fun removeSelectedAttachments(id: Int, date: Date?, fileName: String) {
        IncompleteRequestsRepository.deleteIncompleteAttachmentsById(
            id,
            date,
            fileName,
            GlobalScope
        )
    }

    fun removeSelectedNote(customUUID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            IncompleteRequestsRepository.deleteIncompleteNoteById(customUUID)
            ServiceCallNotesRepository.deleteNoteByCustomId(customUUID)
        }
    }

    fun refreshSCList(size: Int) = viewModelScope.launch {
        if (shouldRefreshServiceCallList && size == 0) {
            RetrofitRepository.RetrofitRepositoryObject.getInstance().getTechnicianActiveServiceCallsFlow().collect {  }
            shouldRefreshServiceCallList = false
        }
    }
}