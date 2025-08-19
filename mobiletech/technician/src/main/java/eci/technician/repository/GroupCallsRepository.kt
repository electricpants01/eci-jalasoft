package eci.technician.repository

import android.util.Log
import eci.technician.activities.GroupCallsActivity
import eci.technician.helpers.AppAuth
import eci.technician.models.order.GroupCallServiceOrder
import io.realm.Realm

object GroupCallsRepository {
    suspend fun updateReassignedGroupCallServiceOrder(newGroupCallServiceOrder: GroupCallServiceOrder) {
        val realm = Realm.getDefaultInstance()
        deleteGroupCallServiceOrderByID(newGroupCallServiceOrder.callNumber_ID)
        try {
            realm.executeTransaction {
                realm.insert(newGroupCallServiceOrder)
            }
        } catch (e: Exception) {
            Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }
    suspend fun deleteGroupCallServiceOrder(groupCallServiceOrder: GroupCallServiceOrder) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                groupCallServiceOrder?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun deleteGroupCallServiceOrderByID(groupCallServiceOrderID: Int) {
        val realm = Realm.getDefaultInstance()
        try {
            var groupCallServiceOrder = realm
                .where(GroupCallServiceOrder::class.java)
                .equalTo(GroupCallServiceOrder.CALL_NUMBER_ID, groupCallServiceOrderID)
                .findFirst()

            realm.executeTransaction {
                groupCallServiceOrder?.deleteFromRealm()
            }
        } catch (e: Exception) {
            Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun updateGroupServiceCall(groupCallServiceOrder: GroupCallServiceOrder) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                realm.insertOrUpdate(groupCallServiceOrder)
            }
        } catch (e: Exception) {
            Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun updateGroupCallServiceOrderTechnicianData(groupCallServiceOrder: GroupCallServiceOrder) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                AppAuth.getInstance().technicianUser.technicianCode?.let {
                    groupCallServiceOrder.technicianNumber = it
                }
                groupCallServiceOrder.technicianName =
                    AppAuth.getInstance().technicianUser.technicianName
                realm.insertOrUpdate(groupCallServiceOrder)
            }
        } catch (e: Exception) {
            Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
        } finally {
            realm.close()
        }
    }

    suspend fun getGroupCallServiceOrder(serviceOrderCallNumberId: Int): GroupCallServiceOrder? {
        val realm = Realm.getDefaultInstance()
        var groupCall: GroupCallServiceOrder? = null
        try {
            groupCall = realm.where(GroupCallServiceOrder::class.java)
                .equalTo(GroupCallServiceOrder.CALL_NUMBER_ID, serviceOrderCallNumberId)
                .findFirst()
            groupCall = realm.copyFromRealm(groupCall)
        } catch (e: Exception) {
            Log.e(GroupCallsActivity.TAG, GroupCallsActivity.EXCEPTION, e)
        } finally {
            realm.close()
            return groupCall
        }
    }

}