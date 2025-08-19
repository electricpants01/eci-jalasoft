package eci.technician.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import eci.technician.R
import eci.technician.helpers.ErrorHelper.RequestCodeHandler
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.postModels.CreateNotePostModel
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteResponse
import eci.technician.repository.ServiceCallNotesRepository
import eci.technician.tools.Constants
import eci.technician.tools.Settings
import io.realm.Realm
import io.realm.Sort
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class NotesOfflineWorker(appContext: Context, private val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        const val TAG = "NotesOfflineWorker"
        const val EXCEPTION = "Exception"
    }

    override fun doWork(): Result {
        val realm = Realm.getDefaultInstance()
        realm.refresh()
        val incompleteRequests = realm
            .where(NoteIncompleteRequest::class.java)
            .equalTo(
                NoteIncompleteRequest.STATUS,
                Constants.INCOMPLETE_REQUEST_STATUS.WAITING.value
            )
            .or()
            .equalTo(NoteIncompleteRequest.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value)
            .or().equalTo(
                NoteIncompleteRequest.STATUS,
                Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
            )
            .sort(NoteIncompleteRequest.DATE_ADDED, Sort.ASCENDING)
            .findAll()
        if (incompleteRequests.isEmpty()) {
            return Result.success()
        } else {
            for (incompleteRequest in incompleteRequests) {
                if (incompleteRequest != null) {
                    realm.executeTransaction {
                        incompleteRequest.status =
                            Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.value
                    }

                    when (incompleteRequest.requestCategory) {
                        NoteIncompleteRequest.REQUEST_TYPE_CREATE -> {
                            performCreateNote(incompleteRequest, applicationContext) {
                                Result.failure()
                            }
                        }
                        NoteIncompleteRequest.REQUEST_TYPE_UPDATE -> {
                            performUpdateNote(incompleteRequest, applicationContext) {
                                Result.failure()
                            }
                        }
                        NoteIncompleteRequest.REQUEST_TYPE_DELETE -> {
                            performDeleteNote(incompleteRequest, applicationContext) {
                                Result.failure()
                            }
                        }
                    }
                } else {
                    return Result.failure()
                }
            }

        }

        return Result.failure()
    }

    private fun performDeleteNote(
        incompleteRequest: NoteIncompleteRequest,
        applicationContext: Context,
        shouldRetry: () -> Result
    ) {
        /**
         * Perform delete process in the future
         */
    }

    private fun manageException(
        firstIncomplete: NoteIncompleteRequest,
        applicationContext: Context,
        e: Exception,
        onEvent: () -> Unit
    ) {
        when (e) {
            is ConnectException -> {
                setIncompleteToSimpleFailWithMessage(
                    firstIncomplete, applicationContext, applicationContext.getString(
                        R.string.something_went_wrong_connection
                    )
                )
                onEvent.invoke()
            }
            is SocketTimeoutException -> {
                setIncompleteToSimpleFailWithMessage(
                    firstIncomplete, applicationContext, applicationContext.getString(
                        R.string.something_went_wrong_timeout
                    )
                )
                onEvent.invoke()
            }
            is IOException -> {
                setIncompleteToSimpleFailWithMessage(
                    firstIncomplete, applicationContext, applicationContext.getString(
                        R.string.something_went_wrong_io_exception
                    )
                )
                onEvent.invoke()
            }
            else -> {
                setIncompleteToSimpleFail(firstIncomplete, applicationContext)
                onEvent.invoke()
            }
        }
    }

    private fun performUpdateNote(
        incompleteRequest: NoteIncompleteRequest,
        applicationContext: Context,
        function: () -> Result
    ) {
        /**
         * Perform Update process in the future
         */
    }

    private fun performCreateNote(
        incompleteRequest: NoteIncompleteRequest,
        applicationContext: Context,
        shouldRetry: () -> Result
    ) {

        val createNotePostModel =
            CreateNotePostModel.instanceFromIncompleteRequest(incompleteRequest)
        try {
            val retroResponse =
                RetrofitApiHelper.getApi()?.createServiceCallNote(createNotePostModel)?.execute()
            if (retroResponse == null) {
                setIncompleteToSimpleFail(incompleteRequest, applicationContext)
                return
            }
            if (retroResponse.body() != null && retroResponse.isSuccessful) {
                retroResponse.body()?.let {
                    validateProcessingResult(
                        incompleteRequest,
                        it,
                        applicationContext,
                        retroResponse
                    )
                }
                return
            } else {
                manageResultOnError(incompleteRequest, retroResponse, applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            manageException(incompleteRequest, applicationContext, e) {
                shouldRetry.invoke()
            }
        }
    }

    private fun manageResultOnError(
        incompleteRequest: NoteIncompleteRequest,
        retroResponse: Response<ProcessingResult>,
        applicationContext: Context
    ) {
        val requestError = RequestCodeHandler.getMessageErrorFromResponse(retroResponse, null)
        setIncompleteToSimpleFailWithMessage(
            incompleteRequest,
            applicationContext,
            requestError.description
        )
    }

    private fun validateProcessingResult(
        incompleteRequest: NoteIncompleteRequest,
        processingResult: ProcessingResult,
        applicationContext: Context,
        retroResponse: Response<ProcessingResult>
    ) {
        if (processingResult.isHasError) {
            val requestError =
                RequestCodeHandler.getMessageErrorFromResponse(retroResponse, processingResult)
            setIncompleteToSimpleFailWithMessage(
                incompleteRequest,
                applicationContext,
                requestError.description
            )
        } else {
            var listOfNotes: MutableList<ServiceCallNoteResponse> =
                mutableListOf(
                    *Settings.createGson()
                        .fromJson(
                            processingResult.result,
                            Array<ServiceCallNoteResponse>::class.java
                        )
                )
            if (listOfNotes.isNullOrEmpty()) {
                listOfNotes = mutableListOf()
            }
            val listTosave = listOfNotes.map { note ->
                ServiceCallNoteEntity.convertToNoteEntity(note)
            }
            val customUUID = incompleteRequest.customUUID
            GlobalScope.launch {
                ServiceCallNotesRepository.saveNoteFromResponse(listTosave, customUUID)
            }

            setIncompleteToSuccess(incompleteRequest, processingResult, applicationContext)
        }
    }

    private fun setIncompleteToSuccess(
        incompleteRequest: NoteIncompleteRequest,
        processingResult: ProcessingResult,
        applicationContext: Context
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                incompleteRequest.status = Constants.INCOMPLETE_REQUEST_STATUS.SUCCESS.value
                incompleteRequest.deleteFromRealm()
            }
//            OfflineManager.retryNotesWorker(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    private fun setIncompleteToSimpleFailWithMessage(
        firstIncomplete: NoteIncompleteRequest,
        applicationContext: Context,
        message: String
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                firstIncomplete.status = Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value
                firstIncomplete.requestErrors = message
                firstIncomplete.requestErrorCode = 0
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    private fun setIncompleteToSimpleFail(
        firstIncomplete: NoteIncompleteRequest,
        applicationContext: Context
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                firstIncomplete.status = Constants.INCOMPLETE_REQUEST_STATUS.FAIL.value
                firstIncomplete.requestErrors =
                    applicationContext.getString(R.string.somethingWentWrong)
                firstIncomplete.requestErrorCode = 0
            }
//            OfflineManager.stopAttachmentWorker(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }
}