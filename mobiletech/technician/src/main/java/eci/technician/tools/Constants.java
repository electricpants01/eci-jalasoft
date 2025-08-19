package eci.technician.tools;

import retrofit2.http.PUT;

public class Constants {
    public static final String PREFERENCE_AUTH = "auth";
    public static final String PREFERENCE_LAST_USER = "lastUser";
    public static final String PREFERENCE_USER = "user";
    public static final String PREFERENCE_LAST_LOGGED_IN_USER = "lastLoggedInUser";
    public static final String PREFERENCE_TOKEN = "token";
    public static final String PREFERENCE_LAST_ODOMETER = "odometer";
    public static final String PREFERENCE_LAST_ODOMETER_INT = "odometerInt";
    public static final String PREFERENCE_SERVER_ADDRESS = "serverAddress";
    public static final String PREFERENCE_SIGNAL_R_SERVER_ADDRESS = "signalRServerAddress";
    public static final String PREFERENCE_CHAT_ENABLED = "chatEnabled";
    public static final String PREFERENCE_GPS_SERVER_ADDRESS = "gpsServerAddress";
    public static final String PREFERENCE_GPS_PREFIX = "gpsPrefix";
    public static final String PREFERENCE_LICENSE_AGREEMENT = "licenseAgreement";
    public static final String PREFERENCE_NAVIGATION_APP = "navigationApp";
    public static final String PREFERENCE_NAVIGATION = "navigationAppShare";
    public static final String PREFERENCE_GROUP_CALLS_WHEN_FILTER = "groupcalls_when_filter";
    public static final String PREFERENCE_GROUP_CALLS_GROUP_FILTER = "groupcalls_group_filter";
    public static final String PREFERENCE_SERVICE_CALLS_WHEN_FILTER = "servicecalls_when_filter";
    public static final String PREFERENCE_SERVICE_CALLS_STATUS_FILTER = "servicecalls_status_filter";
    public static final String PREFERENCE_LOCATION_DENIED = "background_location_denied";
    public static final String PREFERENCE_PART_REQUEST_HAS_BEEN_SEEN = "part_request_has_been_seen";
    public static final String PREFERENCE_SET_OF_PART_REQUEST = "set_of_part_request";


    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_IDENT = "ident";
    public static final String EXTRA_PAY_PERIOD = "pay_period";
    public static final String EXTRA_SHIFT = "shift";
    public static final String EXTRA_ORDER = "order";
    public static final String EXTRA_ORDER_ID = "orderId";
    public static final String EXTRA_CALL_NUMBER_ID = "callNumberId";
    public static final String EXTRA_CALL_NUMBER_CODE = "callNumberCode";
    public static final String EXTRA_CALL_STATUS_CODE = "callStatusCode";
    public static final String EXTRA_REQUEST = "request";
    public static final String FIREBASE_TOKEN = "firebaseKey";
    public static final String EXTRA_MASTER_CALL_ID = "masterCallId";
    public static final String EXTRA_PART_ID = "partId";
    public static final String EXTRA_PART_NAME = "partName";
    public static final String EXTRA_PART_DESCRIPTION = "partDescription";
    public static final String EXTRA_AVAILABLE_QUANTITY = "availableQuantity";
    public static final String EXTRA_QUANTITY = "availableQuantity";
    public static final String EXTRA_ORDER_STATUS = "orderStatus";
    public static final String EXTRA_ADD_PART = "addPart";
    public static final String EXTRA_AVAILABLE = "available";
    public static final String EXTRA_ASSISTANT_ID = "assistantId";
    public static final String EXTRA_EQUIPMENT_ID = "equipmentId";
    public static final String EXTRA_SHOW_MESSAGES = "show_messages";
    public static final String EXTRA_INCOMPLETE_MODE = "incompleteMode";
    public static final String WAREHOUSE_ID = "warehouseId";
    public static final String EXTRA_TECH_NAME_FOR_REQUEST_PART_CHAT = "tech_name_for_request_part_chat";
    public static final String EXTRA_TECH_NAME_FOR_TECH_LIST = "tech_name_for_tech_list";
    public static final String EXTRA_LIBRARY_TITLE = "library_title";
    public static final String EXTRA_LIBRARY_RAW_ID = "library_raw_id";
    public static final String EXTRA_CANCEL_CODE_ID = "cancelCallId";
    public static final String EXTRA_CANCEL_CODE_DESCRIPTION = "cancelCallDescription";
    public static final String EXTRA_CANCEL_CODE_TITLE = "cancelCallTitle";
    public static final String EXTRA_COMPLETED_CALL_NUMBER_ID = "completedServiceOrderCallNumberId";
    public static final String EXTRA_COMPLETED_CALL_NUMBER_CODE = "completedServiceOrderCallNumberCode";
    public static final String EXTRA_QUERY_SERVICE_CALL_LIST = "queryFromServiceOrderList";
    public static final String EXTRA_QUERY_GROUP_CALL_LIST = "queryFromGroupCallServiceOrderList";
    public static final String EXTRA_HOLD_CODE_ID = "extra_hold_code_id";

    public static final String EXTRA_IS_ASSIST = "extra_is_assist";

    public static final String SELECTED_GROUP_CALL_NUMBER_CODE = "selectedGroupCallNumberCode";
    public static final String ORIGIN_MAP_ACTIVITY = "originMapActivity";
    public static final String MY_SERVICE_CALLS = "myServiceCalls";
    public static final String GROUP_CALLS = "myGroupCalls";
    public static final String FROM_DETAILS = "fromDetails";

    public static final String STATUS_SIGNED_IN = "SignIn";
    public static final String STATUS_SIGNED_OUT = "SignOut";
    public static final String STATUS_BRAKE_IN = "BrakeIn";
    public static final String STATUS_BRAKE_OUT = "BrakeOut";
    public static final String STATUS_LUNCH_IN = "LunchIn";
    public static final String STATUS_LUNCH_OUT = "LunchOut";

    public static final int STATUS_SIGNED_IN_CODE = 1;
    public static final int STATUS_SIGNED_OUT_CODE = 2;
    public static final int STATUS_LUNCH_IN_CODE = 3;
    public static final int STATUS_LUNCH_OUT_CODE = 4;
    public static final int STATUS_BRAKE_IN_CODE = 5;
    public static final int STATUS_BRAKE_OUT_CODE = 6;

    public static final String STRING_ARRIVE = "Arrive";
    public static final String STRING_DISPATCH = "Dispatch";
    public static final String STRING_COMPLETE = "Complete";
    public static final String STRING_INCOMPLETE = "Incomplete";
    public static final String STRING_HOLD = "Hold";
    public static final String STRING_UNDISPATCH = "Undispatch";

    public static final String STRING_DISPATCH_CALL = "DispatchCall";
    public static final String STRING_ARRIVE_CALL = "ArriveCall";
    public static final String STRING_UNDISPATCH_CALL = "UnDispatchCall";
    public static final String STRING_ON_HOLD_CALL = "OnHoldCall";
    public static final String STRING_SCHEDULE_CALL = "ScheduleCall";
    public static final String STRING_HOLD_RELEASE_CALL = "HoldRelease";
    public static final String STRING_DEPART_CALL = "DepartCall";
    public static final String STRING_INCOMPLETE_CALL = "IncompleteCall";

    public static final String STRING_UPDATE_ITEMS_DETAILS = "UpdateItemDetails";

    public static final String NEEDED_PARTS_HINT = "Service Calls with needed parts cannot be reassigned.";

    public static final String ACTION_TIME_PATTERN = "hh:mm aa";

    public static final int EMAIL_LIST_LIMIT = 10;

    public static final String BLACKLIST_ERROR_MESSAGE = "Unable to resolve host";
    public static final String NO_INTERNET_CONNECTION_MESSAGE = "The internet connection appears to be offline";

    public static final String APPLICATION_OCTET = "application/octet-stream";

    public static final int ACTIVITY_TECHNICIANS = 9001;
    public static final int ACTIVITY_PART_NEED = 9002;
    public static final int ACTIVITY_ATTACH_FILE = 2653;
    public static final int ACTIVITY_TRANSFER_REQUEST = 3245;
    public static final int ACTIVITY_GROUP_CALLS_DETAIL = 1212;
    public static final int ACTIVITY_GROUP_CALLS_DETAIL_ERROR = 1213;
    public static final String ACTIVITY_GROUP_CALLS_DETAIL_ID = "ACTIVITY_GROUP_CALLS_DETAIL_ID";
    public static final String ACTIVITY_GROUP_CALLS_DETAIL_CODE = "ACTIVITY_GROUP_CALLS_DETAIL_CODE";
    public static final String ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_DELETE = "ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_DELETE";
    public static final String ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_UPDATE = "ACTIVITY_GROUP_CALLS_MAP_CODES_LIST_UPDATE";
    public static final int ACTIVITY_GROUP_CALLS_DETAIL_DELETE = 1214;
    public static final int ACTIVITY_GROUP_CALLS_MAP_LIST_DELETE = 1215;
    public static final int ACTIVITY_GROUP_CALLS_DETAIL_MAP = 1216;


    public static final int MESSAGES_NOTIFICATION_ID = 15421;

    public static final String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";

    public static final String REFERENCE_CONVERSATIONS = "conversations";
    public static final String REFERENCE_MESSAGES = "messages";
    public static final String REFERENCE_SETTINGS = "settings";
    public static final String REFERENCE_SETTING_CONVERSATION_UPDATE = "conversationUpdated";
    public static final String REFERENCE_SETTING_MESSAGE_UPDATE = "messageUpdated";
    public static final String REFERENCE_SERVICE_ORDERS = "serviceOrders";
    public static final String REFERENCE_PROBLEM_CODES = "problemCodes";
    public static final String REFERENCE_REPAIR_CODES = "repairCodes";
    public static final String REFERENCE_HOLD_CODES = "holdCodes";
    public static final String REFERENCE_CANCEL_CODES = "cancelCodes";
    public static final String REFERENCE_USAGE_STATUSES = "usageStatuses";
    public static final String REFERENCE_USED_PARTS = "usedParts";
    public static final String REFERENCE_USED_REPAIR_CODES = "usedRepairCodes";
    public static final String REFERENCE_USED_PROBLEM_CODES = "usedProblemCodes";
    public static final String REFERENCE_USED_METER_DATA = "usedMeterData";

    public static final String SEARCH_FROM_NEEDED_PARTS = "willSearchFromNeedParts";
    public static final String SEARCH_FROM_REQUEST_PARTS = "willSearchFromRequestPart";
    public static final String SEARCH_FROM_MY_WAREHOUSE = "willSearchFromMyWareHouse";
    public static final String SEARCH_FROM_WAREHOUSES = "willSearchFromWareHouses";
    public static final String SEARCH_FROM_ADD_USED_PARTS = "willSearchFromAddUsedPars";
    public static final String REQUESTED_PARTS = "requestedParts";
    public static final String WAREHOUSE_ACTION ="warehouseAction";

    public static final String REASSIGN_UNAVAILABLE_PARTS = "unavailableParts";
    public static final String REASSIGN_NEEDED_PARTS = "neededParts";
    public static final String UNKNOWN_LOCATION = "Unknown Location";
    public static final String SERVICE_ORDER_ID = "serviceOrderId";

    public static final String TAPPED = "Tapped";
    public static final String KNOWN_SSL_ISSUE = "SSL handshake aborted";
    public static final int HTTP_STATUS_CODE_UNAUTHORIZED = 401;

    public static final String TAG_WORKER_OFFLINE = "oneTimeWorkerOffline";
    public static final String TAG_WORKER_OFFLINE_REFACTOR = "oneTimeWorkerOfflineRefactor";
    public static final String TAG_ATTACHMENT_WORKER_OFFLINE = "oneTimeWorkerOfflineForAttachment";
    public static final String TAG_NOTES_WORKER_OFFLINE = "oneTimeWorkerOfflineForNotes";
    public static final int MINUTE_IN_MILLIS = 60000;
    public static final long TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000L;
    public static final long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000L;
    public static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L;

    public static final float MAX_ZOOM = 18;

    public static final String WORKER_UNIQUE_NAME = "worker_offline_requests";
    public static final String WORKER_UNIQUE_NAME_REFACTOR = "worker_offline_requests_refactor";
    public static final String WORKER_UNIQUE_NAME_ATTACHMENT = "worker_offline_requests_for_attachments";
    public static final String WORKER_UNIQUE_NAME_NOTES = "worker_offline_requests_for_notes";

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_DISPATCH = 1;
    public static final int STATUS_HOLD = 2;
    public static final int STATUS_SCHEDULED = 3;
    public static final int STATUS_DEFAULT = 4;

    public static final String REQUEST_UPDATE_LABOR = "UpdateLabor";
    public static final String OPEN_LICENCE_URI = "https://www.ecisolutions.com/legal/mobiletech-google-terms-and-conditions";


    public static class ErrorCode {
        public static final int INVALID_CREDENTIALS = 1;
    }

    public enum INCOMPLETE_REQUEST_STATUS {
        WAITING(0), IN_PROGRESS(1), SUCCESS(2), FAIL(3);

        private final int value;

        private INCOMPLETE_REQUEST_STATUS(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum CALL_STATUS_TRIMMED {
        PENDING("P"), SCHEDULED("S"), HOLD("H");

        private final String value;

        private CALL_STATUS_TRIMMED(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum REQUEST_TYPE {
        SERVICE_CALLS(0), ACTIONS(1), UPDATE_DETAILS(2), ON_HOLD_CALLS(3), SCHEDULE_CALL(4), RELEASE_CALL(5), COMPLETE_CALL(6), INCOMPLETE_CALL(7);
        private final int value;

        private REQUEST_TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum USED_PART_USAGE_STATUS {
        USED(1), NEEDED(2), PENDING(3);
        private final int value;

        private USED_PART_USAGE_STATUS(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum UPDATE_LABOR_STATUS {
        DISPATCH(1), ARRIVE(2);

        private final int value;

        private UPDATE_LABOR_STATUS(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
