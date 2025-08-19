package eci.technician.models.order

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class IncompleteRequests(
    @PrimaryKey
    var id:String = ""
) : RealmObject() {

    var requestType: String? = null
    var actionTime: Date? = null
    var dateAdded: Date? = null
    var savedOdometer: Double? = null
    var activityCodeId = 0
    var callId: Int? = null
    var itemType: Int? = null
    var newValue: String? = null
    var status: Int? = null
    var callNumberCode: String? = null
    var requestErrors: String? = null
    var requestErrorCode: Int? = null
    var assistActionType: Int? = null
    var technicianId: Int? = null
    var holdCodeId: Int? = null
    var holdCodeTypeId: Int? = null
    var requestCategory: Int? = null
    var comments: String? = null
    var isPreventiveMaintenance = false
    var fileContentBase64: String? = null
    var fileName: String? = null
    var fileSize:Int = 0
    var signeeName: String? = null
    var orderId:Int = 0
    var completeCallDispatchTime: Date? = null
    var completeCallArriveTime: Date? = null
    var isAssist: Boolean = false
    var equipmentId: Int = 0
    var incompleteMode: Boolean = false
    var incompleteCategory: String? = null
    var incompleteCodeId: Int = 0
    var callStatusCode: String? = ""
    var description: String? = ""

    /**
     * For the "itemType"  column
     * 0  -> Ip Address
     * 1  -> MAC Address
     * 2  -> Location remarks
     */

    /**
     * For the "status"  column
     * 0  -> waiting
     * 1  -> in progress
     * 2  -> success
     * 3  -> fail
     */
    /**
     * For the "assistActionType"  column
     * 1  -> dispatch
     */
    /**
     * For the "requestType"  column
     * 0  -> service calls
     * 1  -> actions
     */
    companion object {
        const val ID = "id"
        const val DATE_ADDED = "dateAdded"
        const val STATUS = "status"
        const val REQUEST_CATEGORY = "requestCategory"
        const val ITEM_TYPE = "itemType"
        const val CALL_NUMBER_CODE = "callNumberCode"
        const val CALL_ID = "callId"
        const val INCOMPLETE_MODE = "incompleteMode"

    }
}