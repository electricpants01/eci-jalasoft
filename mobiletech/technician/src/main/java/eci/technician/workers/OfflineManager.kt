package eci.technician.workers

import android.content.Context
import androidx.work.*
import eci.technician.tools.Constants
import eci.technician.workers.serviceOrderQueue.IncompleteRequestWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object OfflineManager {
    val mConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    val uploadWorkRequestRefactor = OneTimeWorkRequest.Builder(IncompleteRequestWorker::class.java)
        .addTag(Constants.TAG_WORKER_OFFLINE_REFACTOR)
        .setConstraints(mConstraints)
        .build()

    val uploadAttachmentWorkRequest = OneTimeWorkRequest.Builder(AttachmentsOfflineWorker::class.java)
        .addTag(Constants.TAG_ATTACHMENT_WORKER_OFFLINE)
        .setConstraints(mConstraints)
        .build()

    val uploadNotesWorkRequest = OneTimeWorkRequest.Builder(NotesOfflineWorker::class.java)
        .addTag(Constants.TAG_NOTES_WORKER_OFFLINE)
        .setConstraints(mConstraints)
        .build()


    private fun performNotesWorker(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORKER_UNIQUE_NAME_NOTES,
            ExistingWorkPolicy.KEEP,
            uploadNotesWorkRequest
        )
    }

    fun stopNotesWorker(context: Context) {
        GlobalScope.launch {
            delay(1000)
            WorkManager.getInstance(context).cancelAllWorkByTag(Constants.TAG_NOTES_WORKER_OFFLINE)
        }
    }

    fun retryNotesWorker(context: Context) {
        GlobalScope.launch {
            delay(300)
            performNotesWorker(context)
        }
    }

    fun stopWorker(context: Context) {
        GlobalScope.launch {
            delay(1000)
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(Constants.TAG_WORKER_OFFLINE_REFACTOR)
        }

    }

    fun retryWorker(context: Context) {
        GlobalScope.launch {
            delay(300)
            WorkManager.getInstance(context).enqueueUniqueWork(
                Constants.WORKER_UNIQUE_NAME_REFACTOR,
                ExistingWorkPolicy.KEEP,
                uploadWorkRequestRefactor
            )
        }
    }

    fun performAttachmentsWorker(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORKER_UNIQUE_NAME_ATTACHMENT,
            ExistingWorkPolicy.KEEP,
            uploadAttachmentWorkRequest
        )
    }

    fun stopAttachmentWorker(context: Context) {
        GlobalScope.launch {
            delay(1000)
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(Constants.TAG_ATTACHMENT_WORKER_OFFLINE)
        }
    }

    fun retryAttachmentWorker(context: Context) {
        GlobalScope.launch {
            delay(300)
            performAttachmentsWorker(context)
        }
    }
}