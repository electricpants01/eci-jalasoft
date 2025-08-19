package eci.technician.repository

import android.util.Log
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest
import eci.technician.models.attachments.persistModels.AttachmentItemEntity
import eci.technician.models.order.IncompleteRequests
import eci.technician.models.order.ServiceOrder
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest
import eci.technician.tools.Constants
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

object IncompleteRequestsRepository {
    const val TAG = "IncompleteRequestsRepository"
    const val EXCEPTION = "Exception"


    suspend fun setIncompleteToInProgress(id: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(IncompleteRequests::class.java)
                .equalTo(IncompleteRequests.ID, id)
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

    suspend fun setIncompleteToFail(id: String, errors: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(IncompleteRequests::class.java)
                .equalTo(IncompleteRequests.ID, id)
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

    suspend fun setIncompleteToSuccess(id: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequest = realm
                .where(IncompleteRequests::class.java)
                .equalTo(IncompleteRequests.ID, id)
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


    suspend fun deleteIncompleteRequestByCallNumberCode(callNumberCode: String) {
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val incompleteRequests = realm
                    .where(IncompleteRequests::class.java)
                    .equalTo(IncompleteRequests.CALL_NUMBER_CODE, callNumberCode)
                    .findAll()

                var currentServiceCall = realm.where(ServiceOrder::class.java)
                    .equalTo("callNumber_Code", callNumberCode)
                    .findFirst()

                for (item in incompleteRequests) {
                    item?.let { item ->
                        realm.executeTransaction {
                            when (item.requestType) {
                                Constants.STRING_DEPART_CALL -> {
                                    currentServiceCall?.completedCall = false
                                }
                                Constants.STRING_INCOMPLETE_CALL -> {
                                    currentServiceCall?.completedCall = false
                                }
                            }
                            item.deleteFromRealm()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }

    }

    fun getIncompleteRequestInProgressByCallId(callId: Int): RealmLiveData<IncompleteRequests>? {
        val realm = Realm.getDefaultInstance()
        try {
            return RealmLiveData(
                realm.where(IncompleteRequests::class.java)
                    .equalTo(IncompleteRequests.CALL_NUMBER_CODE, callId)
                    .equalTo(
                        IncompleteRequests.STATUS,
                        Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                    )
                    .findAllAsync()
            )
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return null
        } finally {
            realm.close()
        }
    }

    fun getIncompleteRequestsCopy(incompleteRequests: MutableList<IncompleteRequests>): MutableList<IncompleteRequests> {
        val realm = Realm.getDefaultInstance()
        try {
            return incompleteRequests.map { item -> realm.copyFromRealm(item) }.toMutableList()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return incompleteRequests
        } finally {
            realm.close()
        }
    }

    fun getIncompleteAttachmentCopy(incompleteRequest: AttachmentIncompleteRequest): AttachmentIncompleteRequest {
        val realm = Realm.getDefaultInstance()
        try {
            return realm.copyFromRealm(incompleteRequest)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return incompleteRequest
        } finally {
            realm.close()
        }
    }

    suspend fun getIncompleteNoteCopy(customUUID: String): NoteIncompleteRequest? {
        val realm = Realm.getDefaultInstance()
        return try {
            val noteIncompleteRequest = realm.where(NoteIncompleteRequest::class.java)
                .equalTo(NoteIncompleteRequest.CUSTOM_UUID, customUUID).findFirst()
            if (noteIncompleteRequest == null) {
                null
            } else {
                realm.copyFromRealm(noteIncompleteRequest)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            null
        } finally {
            realm.close()
        }
    }

    suspend fun deleteIncompleteNoteById(customUUID: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val noteIncompleteRequest = realm.where(NoteIncompleteRequest::class.java)
                .equalTo(NoteIncompleteRequest.CUSTOM_UUID, customUUID).findFirst()
            realm.executeTransaction {
                noteIncompleteRequest?.let { note ->
                    note.deleteFromRealm()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    /**
     * Attachments
     */
    fun deleteIncompleteAttachmentsById(
        id: Int,
        date: Date?,
        fileName: String,
        scope: CoroutineScope
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val realm = Realm.getDefaultInstance()
                try {
                    val incompleteRequest = realm
                        .where(AttachmentIncompleteRequest::class.java)
                        .equalTo(AttachmentIncompleteRequest.COLUMNS.CALL_NUMBER_ID, id)
                        .equalTo(AttachmentIncompleteRequest.COLUMNS.DATE_ADDED, date)
                        .findFirst()

                    incompleteRequest?.let { item ->
                        realm.executeTransaction {
                            item.deleteFromRealm()
                        }

                        val attachmentItemId = item.attachmentItemEntityId
                        val attachmentEntity = realm.where(AttachmentItemEntity::class.java)
                            .equalTo(AttachmentItemEntity.ID, attachmentItemId).findFirst()
                        realm.executeTransaction {
                            attachmentEntity?.deleteFromRealm()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, EXCEPTION, e)
                } finally {
                    realm.close()
                }
            }
        }
    }

    suspend fun getIncompleteAttachmentById(id: String): AttachmentIncompleteRequest? {
        val realm = Realm.getDefaultInstance()
        return try {
            val incompleteRequest = realm.where(AttachmentIncompleteRequest::class.java)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.ID, id)
                .findFirst()
            if (incompleteRequest != null) {
                realm.copyFromRealm(incompleteRequest)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            null
        } finally {
            realm.close()
        }
    }

    /**
     * End Attachments
     */


    suspend fun checkUnsyncData(): Boolean {
        val realm = Realm.getDefaultInstance()
        try {
            realm.refresh()
            val res = realm.where(
                IncompleteRequests::class.java
            )
                .equalTo(
                    IncompleteRequests.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                )
                .or()
                .equalTo(
                    IncompleteRequests.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                )
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value)
                .findAll()
            val resAttachment = realm.where(
                AttachmentIncompleteRequest::class.java
            )
                .equalTo(
                    AttachmentIncompleteRequest.COLUMNS.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                )
                .or()
                .equalTo(
                    AttachmentIncompleteRequest.COLUMNS.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                )
                .or()
                .equalTo(
                    AttachmentIncompleteRequest.COLUMNS.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value
                )
                .findAll()
            val resNotes = realm.where(
                NoteIncompleteRequest::class.java
            )
                .equalTo(
                    NoteIncompleteRequest.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
                )
                .or()
                .equalTo(
                    NoteIncompleteRequest.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                )
                .or()
                .equalTo(
                    NoteIncompleteRequest.STATUS,
                    Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value
                )
                .findAll()
            return !res.isEmpty() || !resAttachment.isEmpty() || !resNotes.isEmpty()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return false
        } finally {
            realm.close()
        }
    }

    fun createClockActionIncompleteRequest(action: String, odometer: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            val incompleteRequests = IncompleteRequests(UUID.randomUUID().toString())
            incompleteRequests.requestType = action
            incompleteRequests.requestCategory = Constants.REQUEST_TYPE.ACTIONS.value
            incompleteRequests.dateAdded = Date()
            incompleteRequests.status = Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
            if (odometer > -1) {
                incompleteRequests.savedOdometer = odometer.toDouble()
            }
            realm.executeTransaction {
                realm.insertOrUpdate(incompleteRequests)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

}