package eci.technician.repository

import android.util.Log
import eci.technician.activities.CompleteActivity
import eci.technician.models.order.CompletedServiceOrder
import eci.technician.models.order.ServiceOrder
import io.realm.Realm

object CompletedCallsRepository {
    const val TAG = "CompletedCallsRepository"
    const val EXCEPTION = "Exception"

    suspend fun getCompletedCalls(): List<CompletedServiceOrder> {
        var completedCallsList: List<CompletedServiceOrder> = listOf()
        val realm = Realm.getDefaultInstance()
        return try {
            val realmList = realm.where(CompletedServiceOrder::class.java).findAll()
            completedCallsList = realm.copyFromRealm(realmList)
            completedCallsList
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            completedCallsList
        } finally {
            realm.close()
        }
    }

    suspend fun getCompletedCallById(id: String): CompletedServiceOrder? {
        var completedCall: CompletedServiceOrder? = null
        val realm = Realm.getDefaultInstance()
        return try {
            val realmCompletedCall =
                    realm.where(CompletedServiceOrder::class.java).equalTo(CompletedServiceOrder.ID, id)
                            .findFirst()
            completedCall = realm.copyFromRealm(realmCompletedCall)
            completedCall
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            completedCall
        } finally {
            realm.close()
        }
    }

    suspend fun persistCompletedCalls(completedServiceCallsList: List<CompletedServiceOrder>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.delete(CompletedServiceOrder::class.java)
                realm.insertOrUpdate(completedServiceCallsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }


    suspend fun saveDescriptionInServiceCallTemporalData(orderId:Int, text: String)
    {
        val realm = Realm.getDefaultInstance()
        try {
            val temporalServiceOrder =
                    DatabaseRepository.getInstance().getServiceCallTemporaryDataImproved(orderId)
            temporalServiceOrder?.let {
                realm.executeTransaction{
                    temporalServiceOrder.description =  text
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }
    }
}