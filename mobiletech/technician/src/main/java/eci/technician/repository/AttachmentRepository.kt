package eci.technician.repository

import android.os.Environment
import android.util.Log
import com.google.android.gms.common.util.Base64Utils
import eci.technician.MainApplication
import eci.technician.helpers.api.retroapi.ApiUtils
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest
import eci.technician.models.attachments.persistModels.AttachmentItemEntity
import eci.technician.models.attachments.postModels.UploadFileModel
import eci.technician.models.attachments.responses.AttachmentFile
import eci.technician.models.attachments.responses.AttachmentItem
import eci.technician.models.attachments.ui.AttachmentItemUI
import eci.technician.tools.Constants
import eci.technician.tools.ConstantsKotlin
import io.realm.Realm
import io.realm.kotlin.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

object AttachmentRepository {

    const val TAG = "AttachmentRepository"
    const val EXCEPTION = "Exception"

    fun getAttachmentFile(
        id: Int,
        callNumberId: Int,
        callNumber: String
    ): Flow<Resource<out AttachmentFile>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getAttachmentFile(id) }
            if (resource is Resource.Success) {
                resource.data?.let { attachmentFile ->
                    if (attachmentFile.base64text == null) {
                        val errorR = Resource.Error<AttachmentFile>(
                            "",
                            null,
                            Pair(ErrorType.SOMETHING_WENT_WRONG, ConstantsKotlin.INVALID_DATA)
                        )
                        emit(errorR)
                    } else {
                        val bytes = Base64Utils.decode(attachmentFile.base64text ?: "")
                        saveFile(
                            attachmentFile.fileName ?: "",
                            bytes,
                            callNumber,
                            onSuccess = {
                                val path =
                                    getFilePathForFile(attachmentFile.fileName ?: "", callNumber)
                                updateAttachmentItemEntity(
                                    callNumberId,
                                    attachmentFile.fileName ?: "",
                                    path
                                )
                                emit(resource)
                            },
                            onError = {
                                val errorR = Resource.Error<AttachmentFile>(
                                    "",
                                    null,
                                    Pair(ErrorType.SOMETHING_WENT_WRONG, it)
                                )
                                emit(errorR)
                            }
                        )
                    }
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    private fun updateAttachmentItemEntity(callNumberId: Int, fileName: String, path: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val attachmentItemEntity = realm.where(AttachmentItemEntity::class.java)
                .equalTo(AttachmentItemEntity.FILE_NAME, fileName)
                .equalTo(AttachmentItemEntity.CALL_NUMBER_ID, callNumberId)
                .findFirst()
            realm.executeTransaction {
                attachmentItemEntity?.let {
                    it.localPath = path
                    it.isCreatedLocally = false
                    it.downloadTime = Date()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getFilePathForFile(fileName: String, callNumber: String): String {
        return File(
            MainApplication.appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            changeFileName(fileName, callNumber)
        ).toString()

    }

    suspend fun saveFile(
        fileName: String,
        bytes: ByteArray,
        callNumber: String,
        onSuccess: suspend () -> Unit,
        onError: suspend (error: String) -> Unit
    ) {
        val file = File(getFilePathForFile(fileName, callNumber))
        if (!file.exists()) {
            kotlin.runCatching {
                file.createNewFile()
                try {
                    val outputStream = FileOutputStream(file)
                    outputStream.write(bytes)
                    outputStream.close()
                    onSuccess.invoke()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    onError.invoke(ConstantsKotlin.CAN_NOT_SAVE_FILE)
                }
            }
        } else {
            onSuccess.invoke()
        }
    }

    fun fetchAttachmentList(
        callNumber: String,
        callNumberId: Int
    ): Flow<Resource<List<AttachmentItem>?>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getAttachments2(callNumber) }
            if (resource is Resource.Success) {
                resource.data?.let {
                    saveAttachmentListInDB(it, callNumber, callNumberId)
                    deleteAttachmentsDeletedInResponse(it, callNumber, callNumberId)
                }
            }
            emit(resource)
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    private fun deleteAttachmentsDeletedInResponse(
        listFromResponse: List<AttachmentItem>,
        callNumber: String,
        callNumberId: Int
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            val listAlreadyInDb = mutableListOf<String>()
            listFromResponse.forEach { item ->
                val attachmentItemEntity = realm.where(AttachmentItemEntity::class.java)
                    .equalTo(AttachmentItemEntity.FILE_NAME, item.filename)
                    .equalTo(AttachmentItemEntity.CALL_NUMBER_ID, callNumberId)
                    .equalTo(AttachmentItemEntity.NUMBER, item.number)
                    .equalTo(AttachmentItemEntity.CREATED_LOCALLY, false)
                    .findFirst()
                attachmentItemEntity?.let { itemInDB -> listAlreadyInDb.add(itemInDB.id) }
            }

            val listToDelete = realm.where(AttachmentItemEntity::class.java)
                .not()
                .`in`(AttachmentItemEntity.ID, listAlreadyInDb.toTypedArray())
                .findAll()
            listToDelete.forEach {
                deleteAttachmentFile(it.filename ?: "", callNumber)
            }
            realm.executeTransaction {
                listToDelete.deleteAllFromRealm()
            }

        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun uploadAttachment(uploadFileModel: UploadFileModel): Flow<Resource<ProcessingResult>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.uploadFile2(uploadFileModel) }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    emit(Resource.getProcessingResultError<ProcessingResult>(response))
                } else {
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    private suspend fun saveAttachmentListInDB(
        attachmentList: List<AttachmentItem>,
        callNumber: String,
        callNumberId: Int
    ) {

        val mappedToEntity = attachmentList.map { attachmentItem ->
            val path = File(
                MainApplication.appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                changeFileName(attachmentItem.filename ?: "", callNumber)
            ).toString()

            AttachmentItemEntity.convertToAttachmentItemEntity(attachmentItem, callNumberId, path)
        }

        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(mappedToEntity)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getAttachmentsFromDBFlow(
        callNumberId: Int
    ): Flow<List<AttachmentItemUI>> {
        val realm = Realm.getDefaultInstance()
        return try {
            realm.where(AttachmentItemEntity::class.java)
                .equalTo(AttachmentItemEntity.CALL_NUMBER_ID, callNumberId)
                .findAll().toFlow().map { list ->
                    val uiList = mutableListOf<AttachmentItemUI>()
                    list.forEach { item ->
                        uiList.add(
                            AttachmentItemUI(
                                item.id,
                                item.dBFileLinkID,
                                item.createDate,
                                item.description,
                                item.filename,
                                item.link,
                                item.mimeHeader,
                                item.number,
                                item.callNumberId,
                                item.localPath,
                                item.isCreatedLocally,
                                item.downloadTime
                            )
                        )
                    }
                    uiList
                }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            flow { }
        } finally {
            realm.close()
        }
    }

    fun createAttachmentItemEntityLocally(
        fileName: String,
        callNumberId: Int,
        callNumber: String,
    ) {
        val path = File(
            MainApplication.appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            changeFileName(fileName, callNumber)
        ).toString()

        val attachmentItemEntity = AttachmentItemEntity()
        attachmentItemEntity.id = UUID.randomUUID().toString()
        attachmentItemEntity.dBFileLinkID = 0
        attachmentItemEntity.createDate = null
        attachmentItemEntity.description = "Upload Pending"
        attachmentItemEntity.filename = fileName
        attachmentItemEntity.link = null
        attachmentItemEntity.mimeHeader = ""
        attachmentItemEntity.number = 0

        attachmentItemEntity.callNumberId = callNumberId
        attachmentItemEntity.localPath = path
        attachmentItemEntity.isCreatedLocally = true

        val realm = Realm.getDefaultInstance()
        try {

            realm.executeTransaction {
                realm.insertOrUpdate(attachmentItemEntity)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    private fun changeFileName(fileName: String, callNumber: String): String {
        return "${callNumber}_" + fileName
    }


    fun getAttachmentFromDBBy(fileName: String, callNumberId: Int): AttachmentItemEntity? {
        val realm = Realm.getDefaultInstance()
        try {
            val attachmentItemEntity = realm
                .where(AttachmentItemEntity::class.java)
                .equalTo(AttachmentItemEntity.FILE_NAME, fileName)
                .equalTo(AttachmentItemEntity.CALL_NUMBER_ID, callNumberId)
                .findFirst()
            return if (attachmentItemEntity != null) {
                realm.copyFromRealm(attachmentItemEntity)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return null
        } finally {
            realm.close()
        }
    }

    fun setIncompleteAttachmentToInProgress(id: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(AttachmentIncompleteRequest::class.java)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.ID, id)
                .findFirst()
            realm.executeTransaction {
                incompleteRequest?.let {
                    incompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    fun setIncompleteAttachmentToFail(incompleteReqId: String, errors: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(AttachmentIncompleteRequest::class.java)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.ID, incompleteReqId)
                .findFirst()
            realm.executeTransaction {
                incompleteRequest?.let {
                    incompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value
                    incompleteRequest.requestErrors = errors
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    fun setIncompleteAttachmentToSuccess(incompleteReqId: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(AttachmentIncompleteRequest::class.java)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.ID, incompleteReqId)
                .findFirst()
            realm.executeTransaction {
                incompleteRequest?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun deleteAttachmentItemEntityById(attachmentItemEntityId: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val attachmentItem = realm
                .where(AttachmentItemEntity::class.java)
                .equalTo(AttachmentItemEntity.ID, attachmentItemEntityId)
                .findFirst()
            realm.executeTransaction {
                attachmentItem?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun deleteIncompleteRequestById(id: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(AttachmentIncompleteRequest::class.java)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.ID, id)
                .findFirst()
            realm.executeTransaction {
                incompleteRequest?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun deleteAttachmentItemEntityBy(fileName: String, callNumberId: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(AttachmentItemEntity::class.java)
                .equalTo(AttachmentItemEntity.FILE_NAME, fileName)
                .equalTo(AttachmentItemEntity.CALL_NUMBER_ID, callNumberId)
                .findFirst()
            realm.executeTransaction {
                incompleteRequest?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun deleteAttachmentIncompleteRequestBy(fileName: String, callNumberId: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(AttachmentIncompleteRequest::class.java)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.FILE_NAME, fileName)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.CALL_NUMBER_ID, callNumberId)
                .findFirst()
            realm.executeTransaction {
                incompleteRequest?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun deleteAttachmentFile(fileName: String, callNumber: String) {
        val path = getFilePathForFile(fileName, callNumber)
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    fun createAttachmentIncompleteRequest(
        fileName: String,
        callNumberId: Int,
        callNumber: String,
        fileSize: Int,
        contentType: String
    ) {

        val realm = Realm.getDefaultInstance()
        try {
            val attachmentItemEntityId = getAttachmentFromDBBy(fileName, callNumberId)?.id ?: ""
            val path = getFilePathForFile(fileName, callNumber)
            val incompleteRequest = AttachmentIncompleteRequest()
            incompleteRequest.id = UUID.randomUUID().toString()
            incompleteRequest.fileName = fileName
            incompleteRequest.localPath = path
            incompleteRequest.fileSize = fileSize
            incompleteRequest.callNumber = callNumber
            incompleteRequest.callNumberId = callNumberId
            incompleteRequest.contentType = contentType
            incompleteRequest.dateAdded = Date()
            incompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
            incompleteRequest.attachmentItemEntityId = attachmentItemEntityId

            realm.executeTransaction {
                realm.insertOrUpdate(incompleteRequest)
            }

        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }
}