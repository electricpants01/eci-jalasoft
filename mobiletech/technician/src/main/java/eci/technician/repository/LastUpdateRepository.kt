package eci.technician.repository

import android.util.Log
import eci.technician.models.lastUpdate.LastUpdate
import io.realm.Realm
import java.text.SimpleDateFormat
import java.util.*

object LastUpdateRepository {

    const val TAG = "UserPreferencesRepository"
    const val EXCEPTION = "Exception"

    const val ONE_DAY = 3600 * 24
    const val ONE_MINUTE = 60

    data class DBCacheConfiguration(val key: String, val durationInSeconds: Int)

    object LastUpdateKeys {
        private const val LAST_UPDATE_NEEDED_PARTS = "GetAvailablePartsFullList"
        private const val LAST_UPDATE_MY_WAREHOUSE = "GetAvailablePartByWarehouse"
        private const val LAST_UPDATE_GET_TECHNICIAN_ACTIVE_SERVICE_CALLS =
            "GetTechnicianActiveServiceCalls"


        fun neededPartsConfiguration(): DBCacheConfiguration = DBCacheConfiguration(
            LAST_UPDATE_NEEDED_PARTS, ONE_DAY
        )

        /**
         * Used as KEY to save the timeStamp to Load
         * getAvailablePartsByWarehouse    wit parameters
         * warehouse = 0
         * AvailableInLinkedWarehouse = true
         */
        fun myWarehousePartsConfiguration(): DBCacheConfiguration = DBCacheConfiguration(
            LAST_UPDATE_MY_WAREHOUSE, ONE_DAY
        )

        /**
         * Used as KEY to save the timeStamp to Load
         * getAvailablePartsByWarehouse    wit parameters
         * warehouse = currentTechWarehouseId
         * In case the warehouseId from the tech changes
         */
        fun myWarehousePartsConfigurationWithId(warehouseId: Int): DBCacheConfiguration =
            DBCacheConfiguration(
                "GetAvailablePartByWarehouseWithId$warehouseId", ONE_DAY
            )

        fun activeServiceCallsConfiguration(): DBCacheConfiguration = DBCacheConfiguration(
            LAST_UPDATE_GET_TECHNICIAN_ACTIVE_SERVICE_CALLS, ONE_MINUTE
        )

    }

    fun updateCacheDate(cacheConfiguration: DBCacheConfiguration) {
        val realm = Realm.getDefaultInstance()
        try {
            val lastUpdate = realm.where(LastUpdate::class.java)
                .equalTo(LastUpdate.ID, cacheConfiguration.key)
                .findFirst()
            lastUpdate?.let {
                realm.executeTransaction {
                    lastUpdate.lastUpdateDate = Date().time
                }
            } ?: kotlin.run {
                val lastUpdateNew = LastUpdate()
                lastUpdateNew.id = cacheConfiguration.key
                lastUpdateNew.lastUpdateDate = Date().time
                realm.executeTransaction {
                    realm.insertOrUpdate(lastUpdateNew)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun getLastUpdateString(cacheConfiguration: DBCacheConfiguration): String {
        val date = getDateFrom(cacheConfiguration)
        date?.let {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS Z", Locale.US)
            return formatter.format(date)
        } ?: kotlin.run {
            return ""
        }
    }

    private fun getDateFrom(cacheConfiguration: DBCacheConfiguration): Date? {
        val realm = Realm.getDefaultInstance()
        try {
            val lastUpdate =
                realm.where(LastUpdate::class.java).equalTo(LastUpdate.ID, cacheConfiguration.key)
                    .findFirst()
            lastUpdate?.let {
                val dateLong = it.lastUpdateDate ?: return null
                return Date(dateLong)
            } ?: kotlin.run {
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            return null
        } finally {
            realm.close()
        }
    }

    /**
     * If the cacheTime has expired, it means the parent process should save the data in the DB
     */
    fun cacheHasExpired(cacheConfiguration: DBCacheConfiguration): Boolean {
        val savedDate = getDateFrom(cacheConfiguration) ?: return true
        return hasExpired(savedDate, Date(), cacheConfiguration.durationInSeconds)
    }

    fun deleteLastUpdateCache() {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(LastUpdate::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    fun hasExpired(oldDate: Date, currentDate: Date, durationInSeconds: Int): Boolean {
        val expirationDate = oldDate.time + (durationInSeconds * 1000)
        return currentDate.time >= expirationDate
    }
}