package eci.technician.activities.attachment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eci.technician.R
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.attachments.ui.AttachmentItemUI
import eci.technician.repository.AttachmentRepository
import eci.technician.viewmodels.ViewModelUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class AttachmentsViewModel : ViewModel() {

    companion object {
        const val TAG = "AttachmentsViewModel"
        const val EXCEPTION = "Exception"
    }

    private var loadCounter = 0
    var fileName = ""
    var maxFileName: Int = 150

    var callNumberId: Int = 0
    var callNumber: String = ""

    private var _loading: MutableLiveData<Boolean> = MutableLiveData()
    val loading: LiveData<Boolean> = _loading

    private var _attachmentList: MutableLiveData<List<AttachmentItemUI>> = MutableLiveData()
    val attachmentList: LiveData<List<AttachmentItemUI>> = _attachmentList

    val networkError = MutableLiveData<ViewModelUtils.Event<Pair<ErrorType, String?>>>()
    val toastMessage = MutableLiveData<ViewModelUtils.Event<Int>>()
    val downloadSuccess = MutableLiveData<ViewModelUtils.Event<String>>()
    val saveError = MutableLiveData<ViewModelUtils.Event<Boolean>>()
    val offlineError = MutableLiveData<ViewModelUtils.Event<Boolean>>()


    fun loadAttachmentFromDB(callNumberId: Int) = viewModelScope.launch {
        AttachmentRepository.getAttachmentsFromDBFlow(callNumberId).collect {
            _attachmentList.value = it
        }
    }

    fun fetchAttachments() = viewModelScope.launch {
        AttachmentRepository.fetchAttachmentList(callNumber, callNumberId).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loading.value = false
                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                    when (pair.first) {
                        ErrorType.SOCKET_TIMEOUT_EXCEPTION -> {
                            toastMessage.value = ViewModelUtils.Event(R.string.timeout_message)
                        }
                        ErrorType.CONNECTION_EXCEPTION,
                        ErrorType.IO_EXCEPTION,
                        -> {
                            if (loadCounter < 1) {
                                offlineError.value = ViewModelUtils.Event(true)
                                loadCounter++
                            }
                        }
                        ErrorType.NOT_SUCCESSFUL,
                        ErrorType.BACKEND_ERROR,
                        ErrorType.HTTP_EXCEPTION,
                        ErrorType.SOMETHING_WENT_WRONG -> {
                            networkError.value = ViewModelUtils.Event(pair)

                        }
                    }
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }

        }
    }

    fun getAttachmentFile(linkId: Int) = viewModelScope.launch {
        AttachmentRepository.getAttachmentFile(linkId, callNumberId, callNumber).collect { value ->
            when (value) {
                is Resource.Success -> {
                    _loading.value = false
                    val fileName = value.data?.fileName ?: ""
                    val path = AttachmentRepository.getFilePathForFile(fileName, callNumber)
                    downloadSuccess.value = ViewModelUtils.Event(path)
                }
                is Resource.Error -> {
                    _loading.value = false
                    val pair = value.error ?: Pair(ErrorType.SOMETHING_WENT_WRONG, "Error")
                    when (pair.first) {
                        ErrorType.SOCKET_TIMEOUT_EXCEPTION,
                        ErrorType.CONNECTION_EXCEPTION,
                        ErrorType.IO_EXCEPTION,
                        ErrorType.NOT_SUCCESSFUL,
                        ErrorType.BACKEND_ERROR,
                        ErrorType.HTTP_EXCEPTION,
                        ErrorType.SOMETHING_WENT_WRONG -> {
                            networkError.value = ViewModelUtils.Event(pair)

                        }
                    }
                }
                is Resource.Loading -> {
                    _loading.value = true
                }
            }
        }
    }


    fun isRepeated(newText: String): Boolean {
        val list = _attachmentList.value ?: listOf()
        val nameList = list.map { attachmentItem ->
            getFileNameWithoutExtension(attachmentItem.filename ?: "")
        }
        return nameList.contains(newText)
    }

    fun getFileExtension(fileName: String): String {
        var extension = ""
        if (fileName.isNotEmpty()) {
            extension = File(fileName).extension
        }
        return extension
    }

    fun getFileNameWithoutExtension(fileName: String): String {
        val extension = getFileExtension(fileName)
        return fileName.substringBefore(".${extension}", fileName)
    }

    fun hasForbiddenCharacters(name: String): Boolean {
        val forbiddenChars = arrayOf('\\', '/', ':', '*', '?', '\"', '<', '>', '|')
        name.forEach { c ->
            if (forbiddenChars.contains(c))
                return true
        }
        return false
    }

    fun getPictureExtension(extension: String, fileExtension: String): String {
        return when {
            extension.isNotEmpty() -> extension
            fileExtension.isNotEmpty() -> fileExtension
            else -> "jpg"
        }
    }

    fun sendFile(fullFileName: String, size: Int, contentTypeFile: String, bytes: ByteArray) =
        viewModelScope.launch {
            try {
                AttachmentRepository.createAttachmentItemEntityLocally(
                    fileName = fullFileName,
                    callNumberId,
                    callNumber
                )

                AttachmentRepository.createAttachmentIncompleteRequest(
                    fileName = fullFileName,
                    callNumberId = callNumberId,
                    callNumber = callNumber,
                    fileSize = size,
                    contentType = contentTypeFile
                )
                AttachmentRepository.saveFile(fullFileName, bytes, callNumber,
                    onSuccess = {
                        // do nothing
                    }, onError = {
                        saveError.value = ViewModelUtils.Event(true)
                    })
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                AttachmentRepository.deleteAttachmentItemEntityBy(fullFileName, callNumberId)
                AttachmentRepository.deleteAttachmentIncompleteRequestBy(fullFileName, callNumberId)
                AttachmentRepository.deleteAttachmentFile(fullFileName, callNumber)
                saveError.value = ViewModelUtils.Event(true)
            }
        }
}