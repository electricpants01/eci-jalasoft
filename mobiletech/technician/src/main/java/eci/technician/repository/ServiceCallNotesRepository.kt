package eci.technician.repository

import android.util.Log
import eci.technician.helpers.api.retroapi.ApiUtils
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.api.retroapi.Resource
import eci.technician.helpers.api.retroapi.RetrofitApiHelper
import eci.technician.models.ProcessingResult
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteTypeEntity
import eci.technician.models.serviceCallNotes.responses.ServiceCallNoteTypeResponse
import eci.technician.tools.Settings
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.*

object ServiceCallNotesRepository {

    const val TAG = "ServiceCallNotesRepository"
    const val EXCEPTION = "Exception"

    @Synchronized
    suspend fun getServiceCallNotesTypes(): Flow<Resource<ProcessingResult?>> {
        return flow {
            val api = RetrofitApiHelper.getApi() ?: return@flow
            emit(Resource.Loading())
            val resource = ApiUtils.safeCall { api.getServiceCallNoteTypes2() }
            if (resource is Resource.Success) {
                val response = resource.data ?: return@flow
                if (response.isHasError) {
                    val pair =
                        Pair(ErrorType.BACKEND_ERROR, "${response.formattedErrors} (Server Error)")
                    emit(Resource.Error<ProcessingResult?>("", null, pair))
                } else {
                    response.result?.let { result ->
                        val list = createServiceCallNoteTypes(result)
                        val listToSave = list.map { note ->
                            ServiceCallNoteTypeEntity.convertToNoteTypeEntity(note)
                        }
                        saveNoteTypes(listToSave)
                    }
                    emit(resource)
                }
            } else {
                emit(resource)
            }
        }.flowOn(Dispatchers.IO).catch { emit(Resource.getGenericErrorType()) }
    }

    private fun createServiceCallNoteTypes(responseBody: String): List<ServiceCallNoteTypeResponse> {
        var list = mutableListOf<ServiceCallNoteTypeResponse>()
        return try {
            list = mutableListOf(
                *Settings.createGson()
                    .fromJson(responseBody, Array<ServiceCallNoteTypeResponse>::class.java)
            )
            list
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            list
        }
    }


    suspend fun saveNotes(listOfNotes: List<ServiceCallNoteEntity>) {
        val realm = Realm.getDefaultInstance()
        try {
            val noteDetailIdList: Array<Long> =
                listOfNotes.map { note -> note.noteDetailId }.toTypedArray()
            val notesToDelete = realm.where(ServiceCallNoteEntity::class.java)
                .equalTo(ServiceCallNoteEntity.IS_ADDED_LOCALLY, false)
                .and()
                .not()
                .`in`(ServiceCallNoteEntity.NOTE_DETAIL_ID, noteDetailIdList)
                .findAll()

            listOfNotes.forEach { noteFromResponse ->
                val realmNote = realm.where(ServiceCallNoteEntity::class.java)
                    .equalTo(ServiceCallNoteEntity.NOTE_DETAIL_ID, noteFromResponse.noteDetailId)
                    .findFirst()
                if (realmNote != null) {
                    noteFromResponse.customUUID = realmNote.customUUID
                    realm.executeTransaction {
                        realm.insertOrUpdate(noteFromResponse)
                    }
                } else {
                    noteFromResponse.customUUID = UUID.randomUUID().toString()
                    realm.executeTransaction {
                        realm.insertOrUpdate(noteFromResponse)
                    }
                }
            }

            realm.executeTransaction {
                notesToDelete.deleteAllFromRealm()
                realm.insertOrUpdate(listOfNotes)

            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun saveNoteFromResponse(listOfNotes: List<ServiceCallNoteEntity>, customUUID: String) {

        val realm = Realm.getDefaultInstance()
        try {
            listOfNotes.forEach { noteFromResponse ->
                val lastUpdate = noteFromResponse.lastUpdate
                val callId = noteFromResponse.callId
                val noteEntity = realm.where(ServiceCallNoteEntity::class.java)
                    .equalTo(ServiceCallNoteEntity.CUSTOM_UUID, customUUID)
                    .findFirst()
                realm.executeTransaction {
                    if (noteEntity != null && customUUID.isNotEmpty()) {
                        noteEntity.let {
                            it.note = noteFromResponse.note
                            it.noteDetailId = noteFromResponse.noteDetailId
                            it.isAddedLocally = false
                        }
                    } else {
                        noteFromResponse.customUUID = customUUID
                        realm.insertOrUpdate(noteFromResponse)
                    }
                }

            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun getNoteById(noteDetailId: Long): ServiceCallNoteEntity? {
        var serviceCallNoteEntity: ServiceCallNoteEntity? = null
        val realm = Realm.getDefaultInstance()
        return try {
            val realmNoteEntity =
                realm.where(ServiceCallNoteEntity::class.java)
                    .equalTo(ServiceCallNoteEntity.NOTE_DETAIL_ID, noteDetailId)
                    .findFirst()
            serviceCallNoteEntity = realm.copyFromRealm(realmNoteEntity)
            serviceCallNoteEntity
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            serviceCallNoteEntity
        } finally {
            realm.close()
        }
    }

    suspend fun deleteNoteByCustomId(customUUID: String) {
        val realm = Realm.getDefaultInstance()
        try {
            val noteEntity = realm.where(ServiceCallNoteEntity::class.java)
                .equalTo(ServiceCallNoteEntity.CUSTOM_UUID, customUUID).findFirst()
            noteEntity?.let { note ->
                realm.executeTransaction {
                    note.deleteFromRealm()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun saveNoteTypes(listOfNoteTypes: List<ServiceCallNoteTypeEntity>) {
        val realm = Realm.getDefaultInstance()
        try {
            val noteTypeIdList: Array<Int> =
                listOfNoteTypes.map { noteType -> noteType.noteTypeId }.toTypedArray()

            val noteTypesToDelete = realm.where(ServiceCallNoteTypeEntity::class.java)
                .not()
                .`in`(ServiceCallNoteTypeEntity.NOTE_TYPE_ID, noteTypeIdList)
                .findAll()
            realm.executeTransaction {
                noteTypesToDelete.deleteAllFromRealm()
                realm.insertOrUpdate(listOfNoteTypes)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun getNoteTypeByNoteTypeId(noteTypeId: Int): ServiceCallNoteTypeEntity? {
        var serviceCallNoteTypeEntity: ServiceCallNoteTypeEntity? = null
        val realm = Realm.getDefaultInstance()
        return try {
            val realmNoteTypeEntity =
                realm.where(ServiceCallNoteTypeEntity::class.java)
                    .equalTo(ServiceCallNoteTypeEntity.NOTE_TYPE_ID, noteTypeId)
                    .findFirst()
            serviceCallNoteTypeEntity = realm.copyFromRealm(realmNoteTypeEntity)
            serviceCallNoteTypeEntity
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            serviceCallNoteTypeEntity
        } finally {
            realm.close()
        }
    }

    suspend fun canEditNoteByNoteDetailId(noteDetailId: Long): Boolean {
        val note =
            ServiceCallNotesRepository.getNoteById(noteDetailId) ?: return false
        val noteType =
            ServiceCallNotesRepository.getNoteTypeByNoteTypeId(note.noteTypeId) ?: return false
        return noteType.isEditable ?: false
    }

    suspend fun getAllServiceCallNoteTypes(): List<ServiceCallNoteTypeEntity> {
        val realm = Realm.getDefaultInstance()
        var typesList: List<ServiceCallNoteTypeEntity> = listOf()

        return try {
            val realmTypes =
                realm.where(ServiceCallNoteTypeEntity::class.java)
                    .findAll()
                    .sort(ServiceCallNoteTypeEntity.NOTE_TYPE)
            typesList = realm.copyFromRealm(realmTypes)
            typesList
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            listOf()
        } finally {
            realm.close()
        }
    }
}