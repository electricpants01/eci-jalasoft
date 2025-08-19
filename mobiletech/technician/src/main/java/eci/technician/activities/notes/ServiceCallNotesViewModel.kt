package eci.technician.activities.notes

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity
import eci.technician.models.serviceCallNotes.postModels.CreateNotePostModel
import eci.technician.models.serviceCallNotes.postModels.UpdateNotePostModel
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.repository.ServiceCallNotesRepository
import eci.technician.tools.Constants
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ServiceCallNotesViewModel : ViewModel() {
    companion object{
        const val TAG = "ServiceCallNotesViewModel"
        const val EXCEPTION = "Exception"
    }

    var callId: Int = 0
    var callNumberCode: String = ""
    var currentNoteDetailId: Long = 0
    var originalNoteToUpdate: String = ""
    var updateNoteModelToPost: UpdateNotePostModel = UpdateNotePostModel()
    var createNoteModelToPost: CreateNotePostModel = CreateNotePostModel()
    var notesAlreadyLoaded = false
    var noteTypeSelected: ServiceCallNoteTypeEntity? = null
    var currentNoteToEditUUID: String = ""
    var originalNoteType: ServiceCallNoteTypeEntity? = null
    var isCreating: Boolean = false
    var isEditing: Boolean = false
    var isUpdating: Boolean = false
    var serviceCallNoteEntity: ServiceCallNoteEntity = ServiceCallNoteEntity()

    fun setBasicUpdateNoteModel(noteEntity: ServiceCallNoteEntity) {
        updateNoteModelToPost.callId = noteEntity.callId
        updateNoteModelToPost.createDate = Date()
        updateNoteModelToPost.note = noteEntity.note
        updateNoteModelToPost.noteDetailId = noteEntity.noteDetailId
        updateNoteModelToPost.noteTypeId = noteEntity.noteTypeId
    }

    fun setBasicCreateNoteModel() {
        createNoteModelToPost.callId = callId
        createNoteModelToPost.createDate = Date()
        createNoteModelToPost.note = ""
        createNoteModelToPost.noteTypeId = noteTypeSelected?.noteTypeId ?: -1
    }

    fun createLocalNote(currentDate: Date, customUUID: String, onCreate: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val realm = Realm.getDefaultInstance()
                try {
                    val noteIncompleteRequest = createIncompleteRequest(currentDate, customUUID)
                    val noteEntity = createLocalNoteEntity(currentDate, customUUID)
                    realm.executeTransaction {
                        realm.insertOrUpdate(noteIncompleteRequest)
                        realm.insertOrUpdate(noteEntity)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                } finally {
                    realm.close()
                }
                withContext(Dispatchers.Main) {
                    onCreate.invoke()
                }
            }
        }
    }

    suspend fun createIncompleteRequest(
        currentDate: Date,
        customUUID: String
    ): NoteIncompleteRequest {
        val noteIncompleteRequest = NoteIncompleteRequest()
        noteIncompleteRequest.callId = createNoteModelToPost.callId
        noteIncompleteRequest.note = createNoteModelToPost.note
        noteIncompleteRequest.noteTypeId = noteTypeSelected?.noteTypeId ?: -1
        noteIncompleteRequest.createDate = currentDate
        noteIncompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
        noteIncompleteRequest.requestCategory = NoteIncompleteRequest.REQUEST_TYPE_CREATE
        noteIncompleteRequest.isCreatedLocally = true
        noteIncompleteRequest.dateAdded = currentDate
        noteIncompleteRequest.customUUID = customUUID
        noteIncompleteRequest.callNumberCode = callNumberCode
        return noteIncompleteRequest
    }

    suspend fun createLocalNoteEntity(
        currentDate: Date,
        customUUID: String
    ): ServiceCallNoteEntity {
        val noteItem = ServiceCallNoteEntity()
        noteItem.callId = createNoteModelToPost.callId
        noteItem.note = createNoteModelToPost.note
        noteItem.noteDetailId = currentDate.time
        noteItem.noteId = 0
        noteItem.createDate = currentDate
        noteItem.lastUpdate = currentDate
        noteItem.isAddedLocally = true
        noteItem.noteTypeId = noteTypeSelected?.noteTypeId ?: -1
        noteItem.customUUID = customUUID
        return noteItem
    }

    fun deleteNote(customUUID: String, onSuccess: () -> Unit) {
        GlobalScope.launch {
            IncompleteRequestsRepository.deleteIncompleteNoteById(customUUID)
            ServiceCallNotesRepository.deleteNoteByCustomId(customUUID)
            withContext(Dispatchers.Main) {
                onSuccess.invoke()
            }
        }
    }
}
