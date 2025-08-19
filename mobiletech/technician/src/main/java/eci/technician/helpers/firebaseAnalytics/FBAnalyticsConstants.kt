package eci.technician.helpers.firebaseAnalytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object FBAnalyticsConstants {

    const val MAIN_ACTIVITY = "MainActivity"
    const val ORDER_DETAIL_ACTIVITY = "OrderDetailActivity"
    const val COMPLETE_ACTIVITY = "CompleteActivity"
    const val MESSENGER_ACTIVITY = "MessengerActivity"
    const val COMPLETED_CALLS_ACTIVITY = "CompletedCallsActivity"
    const val COMPLETED_CALLS_DETAIL_ACTIVITY = "CompletedCallsDetailActivity"
    const val GROUP_CALL_ACTIVITY = "GroupCallActivity"
    const val NEW_MY_WAREHOUSE_ACTIVITY = "NewMyWarehouseActivity"
    const val SEARCH_WAREHOUSES_PARTS_ACTIVITY = "SearchWarehousesPartsActivity"
    const val REQUEST_PARTS_ACTIVITY = "RequestPartsActivity"
    const val PARTS_ACTIVITY = "PartsActivity"
    const val FIELD_TRANSFER_ACTIVITY = "FieldTransferActivity"
    const val NEW_PART_REQUEST_ACTIVITY = "NewPartRequestActivity"
    const val TRANSFER_ACTIVITY = "TransferActivity"
    const val SYNC_ACTIVITY = "SyncActivity"
    const val SETTINGS_ACTIVITY = "SettingsActivity"
    const val CREATE_CALL_ACTIVITY = "CreateCallActivity"
    const val APP_AUTH = "AppAuth"
    const val ADD_ASSISTANT_ACTIVITY = "AddAssistantActivity"
    const val ABOUT_ACTIVITY = "AboutActivity"
    const val ATTACHMENTS_ACTIVITY = "AttachmentsActivity"
    const val EQUIPMENT_HISTORY_ACTIVITY = "EquipmentHistoryActivity"
    const val MAP_SETTINGS_ACTIVITY = "MapSettingsActivity"
    const val MAPS_ACTIVITY = "MapsActivity"
    const val NEW_CANCEL_ACTIVITY = "NewCancelActivity"
    const val NOTES_ACTIVITY = "NotesActivity"
    const val PENDING_PARTS_FRAGMENT = "PendingPartsFragment"
    const val ADD_PARTS_ACTIVITY = "AddPartsActivity"
    const val USED_PARTS_FRAGMENT = "UsedPartFragment"
    const val NEEDED_PARTS_FRAGMENT = "NeededPartsFragment"

    object OrderDetailActivity{
        const val ARRIVE_ACTION = "ArriveAction"
        const val UNDISPATCH_ACTION = "UndispatchAction"
        const val SCHEDULE_ACTION = "ScheduleAction"
        const val DISPATCH_ACTION = "DispatchAction"
    }

    object AddAssistantActivity{
        const val SELECT_ASSISTANCE_ACTION = "SelectAssistance"
    }

    object CompleteActivity{
        const val COMPLETE_ACTION = "CompleteAction"
        const val INCOMPLETE_ACTION = "IncompletingAction"
    }

    object CreateCallActivity{
        const val CREATE_CALL_ACTION = "CreateCallAction"
    }

    object NewCancelActivity{
        const val CANCEL_ACTION = "CancelCallAction"
    }

    object AttachmentsActivity{
        const val OPEN_FILE_ACTION = "AttachmentOpenFileAction"
    }

    object NotesActivity{
        const val SHOW_ALL_NOTES_ACTION = "ShowAllNotesFragment"
        const val CREATE_NOTE_ACTION = "CreateNotesFragment"
    }

    object PendingPartsFragment{
        const val ADD_PENDING_PART_ACTION = "AddPendingPartAction"
    }

    object UsedPartFragment{
        const val ADD_USED_PART_ACTION = "AddUsedPartAction"
    }

    object NeededPartsFragment{
        const val ADD_NEEDED_PART_ACTION = "AddNeededPartAction"
    }

    fun logEvent(context: Context, keyEvent: String){
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        bundle.putString("MESSAGE", "$keyEvent has been openned" )
        firebaseAnalytics.logEvent(keyEvent,bundle)
    }

    fun logEvent(context: Context, keyEvent: String, message: String){
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        bundle.putString("MESSAGE", message )
        firebaseAnalytics.logEvent(keyEvent,bundle)
    }
}