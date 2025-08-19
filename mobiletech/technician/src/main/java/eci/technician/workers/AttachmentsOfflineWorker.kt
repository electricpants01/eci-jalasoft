package eci.technician.workers

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Base64OutputStream
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eci.technician.R
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest
import eci.technician.models.attachments.postModels.UploadFileModel
import eci.technician.models.order.IncompleteRequests
import eci.technician.repository.AttachmentRepository
import eci.technician.repository.IncompleteRequestsRepository
import eci.technician.tools.Constants
import eci.technician.workers.serviceOrderQueue.OfflineManagerRefactor
import io.realm.Realm
import io.realm.Sort
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream


class AttachmentsOfflineWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "OfflineWorker"
        const val EXCEPTION = "Exception"
    }

    override suspend fun doWork(): Result {
        try {
            val realm = Realm.getDefaultInstance()
            realm.refresh()
            val incompleteRequests = realm
                .where(AttachmentIncompleteRequest::class.java)
                .equalTo(
                    IncompleteRequests.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                )
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value)
                .or().equalTo(
                    IncompleteRequests.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                )
                .sort(IncompleteRequests.DATE_ADDED, Sort.ASCENDING)
                .findAll()
            val incompleteRequestsCopy = realm.copyFromRealm(incompleteRequests)
            realm.close()
            if (incompleteRequestsCopy.isEmpty()) {
                return Result.success()
            } else {
                coroutineScope {
                    for (incompleteRequest in incompleteRequestsCopy) {
                        if (incompleteRequest != null) {
                            val send = async { sendAttachmentOffline(incompleteRequest.id) {} }
                            send.start()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        return Result.failure()
    }

    private fun convertFileToBase64(imageFile: InputStream): String {
        return ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(outputStream, Base64.DEFAULT).use { base64FilterStream ->
                imageFile.use { inputStream ->
                    inputStream.copyTo(base64FilterStream)
                }
            }
            return@use outputStream.toString()
        }
    }

    private suspend fun sendAttachmentOffline(
        incompleteReqId: String,
        completion: () -> Unit
    ) {
        val incompleteReq =
            IncompleteRequestsRepository.getIncompleteAttachmentById(incompleteReqId) ?: return
        val uri = Uri.fromFile(File(incompleteReq.localPath))
        val fileName = incompleteReq.fileName
        val callNumber = incompleteReq.callNumber
        val callNumberId = incompleteReq.callNumberId
        val incompleteRequestId = incompleteReq.id
        kotlin.runCatching {
            val stream = applicationContext.contentResolver.openInputStream(uri)
            if (stream != null) {
                val base64 = convertFileToBase64(stream)
                stream.close()
                val uploadFileModel = UploadFileModel(
                    fileContentBase64 = base64,
                    contentType = incompleteReq.contentType,
                    fileSize = incompleteReq.fileSize,
                    fileName = incompleteReq.fileName,
                    callNumber = incompleteReq.callNumber
                )

                try {
                    AttachmentRepository.uploadAttachment(uploadFileModel).collect { value ->
                        when (value) {
                            is Resource.Success -> {
                                AttachmentRepository.setIncompleteAttachmentToSuccess(
                                    incompleteRequestId
                                )
                                AttachmentRepository.fetchAttachmentList(callNumber, callNumberId)
                                    .collect { }
                                completion.invoke()
                            }
                            is Resource.Error -> {
                                val errors = value.error?.second ?: "error"
                                AttachmentRepository.setIncompleteAttachmentToFail(
                                    incompleteRequestId,
                                    errors
                                )

                                ErrorHandler.get().notifyListeners(
                                    error = value.error,
                                    requestType = "UploadEsnDocument",
                                    callId = callNumberId,
                                    data = ""
                                )
                                if (value.error?.first == ErrorType.BACKEND_ERROR) {
                                    showNotificationOnDelete(
                                        incompleteReq,
                                        applicationContext,
                                        errors
                                    )
                                    AttachmentRepository.deleteAttachmentFile(fileName, callNumber)
                                    AttachmentRepository.deleteAttachmentItemEntityById(
                                        incompleteReq.attachmentItemEntityId
                                    )
                                    AttachmentRepository.fetchAttachmentList(
                                        callNumber,
                                        callNumberId
                                    )
                                        .collect { }
                                    AttachmentRepository.deleteIncompleteRequestById(incompleteReqId)
                                }
                                completion.invoke()
                            }
                            is Resource.Loading -> {
                                AttachmentRepository.setIncompleteAttachmentToInProgress(
                                    incompleteRequestId
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                    handleUnexpectedError(
                        incompleteReqId,
                        applicationContext.getString(R.string.something_went_wrong_connection),
                        applicationContext,
                        incompleteReq.callNumberId
                    )
                }
            }
        }

    }

    private fun handleUnexpectedError(
        incompleteReqId: String,
        error: String,
        applicationContext: Context,
        callId: Int
    ) {
        AttachmentRepository.setIncompleteAttachmentToFail(incompleteReqId, error)
        OfflineManager.stopWorker(applicationContext)
        ErrorHandler.get()
            .notifyListeners(
                error = Pair(
                    ErrorType.SOMETHING_WENT_WRONG,
                    OfflineManagerRefactor.GENERIC_ERROR
                ),
                requestType = "UploadEsnDocument",
                callId = callId,
                data = OfflineManagerRefactor.GENERIC_ERROR
            )
    }

    private fun showNotificationOnDelete(
        firstIncomplete: AttachmentIncompleteRequest,
        applicationContext: Context,
        errors: String
    ) {
        val title = applicationContext.getString(R.string.upload_error, firstIncomplete.fileName)
        eci.technician.helpers.notification.NotificationManager.showAttachmentNotification(
            applicationContext,
            title,
            errors
        )
    }

}