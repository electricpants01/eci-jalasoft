package eci.technician.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import eci.signalr.messenger.Conversation;
import eci.signalr.messenger.Message;
import eci.technician.models.TechnicianItem;
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest;
import eci.technician.models.attachments.persistModels.AttachmentItemEntity;
import eci.technician.models.create_call.CallType;
import eci.technician.models.data.UsedPart;
import eci.technician.models.data.UsedProblemCode;
import eci.technician.models.data.UsedRepairCode;
import eci.technician.models.filters.CallPriorityFilter;
import eci.technician.models.filters.CallTechnicianFilter;
import eci.technician.models.filters.FilterCriteria;
import eci.technician.models.filters.GroupCallType;
import eci.technician.models.filters.TechnicianCallType;
import eci.technician.models.order.ActivityCode;
import eci.technician.models.order.CallPriority;
import eci.technician.models.order.CancelCode;
import eci.technician.models.order.CompletedServiceOrder;
import eci.technician.models.order.EquipmentMeter;
import eci.technician.models.order.EquipmentRealmMeter;
import eci.technician.models.order.GroupCallServiceOrder;
import eci.technician.models.order.HoldCode;
import eci.technician.models.order.IncompleteCode;
import eci.technician.models.order.IncompleteRequests;
import eci.technician.models.order.Part;
import eci.technician.models.order.ProblemCode;
import eci.technician.models.order.RepairCode;
import eci.technician.models.order.ServiceCallLabor;
import eci.technician.models.order.ServiceCallMeter;
import eci.technician.models.order.ServiceCallProperty;
import eci.technician.models.order.ServiceCallTemporalData;
import eci.technician.models.order.ServiceOrder;
import eci.technician.models.order.StoredLabor;
import eci.technician.models.order.TechnicianWarehousePart;
import eci.technician.models.serviceCallNotes.persistModels.NoteIncompleteRequest;
import eci.technician.models.serviceCallNotes.persistModels.ServiceCallNoteEntity;
import eci.technician.models.time_cards.PayPeriod;
import eci.technician.models.time_cards.Shift;
import eci.technician.models.time_cards.ShiftDetails;
import eci.technician.tools.Constants;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class DatabaseRepository {
    private static DatabaseRepository INSTANCE;
    private static final String TAG = "DatabaseRepository";
    private static final String EXCEPTION = "Exception";
    private Realm realm;

    private DatabaseRepository() {
        realm = Realm.getDefaultInstance();
    }

    public static synchronized DatabaseRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseRepository();
        }
        return INSTANCE;
    }

    public RealmResults<CancelCode> getCancelCodes() {
        return realm.where(CancelCode.class).findAll().sort("code");
    }

    public RealmResults<ProblemCode> getProblemCodes() {
        return realm.where(ProblemCode.class).findAll();
    }

    public RealmResults<PayPeriod> getPayPeriods() {
        return realm.where(PayPeriod.class).findAll();
    }

    public PayPeriod getPayPeriodById(String id) {
        return realm.where(PayPeriod.class).equalTo(PayPeriod.PAY_PERIOD_ID, id).findFirst();
    }

    public Shift getShiftById(String id) {
        return realm.where(Shift.class).equalTo(Shift.SHIFT_ID, id).findFirst();
    }

    public RealmResults<Shift> getShiftsById(String id) {
        return realm.where(Shift.class).equalTo(Shift.PAY_PERIOD_ID, id).sort(Shift.DATE, Sort.DESCENDING).findAll();
    }

    public RealmResults<ShiftDetails> getShiftDetailsById(String id) {
        return realm.where(ShiftDetails.class).equalTo(ShiftDetails.SHIFT_ID, id).sort(ShiftDetails.TIMESTAMP, Sort.DESCENDING).findAll();
    }

    public RealmLiveData<ServiceOrder> getServiceOrderLiveDataByNumberId(int callNumberId) {
        return new RealmLiveData<ServiceOrder>(realm.where(ServiceOrder.class)
                .equalTo(ServiceOrder.CALL_NUMBER_ID, callNumberId)
                .findAllAsync());
    }

    public RealmLiveData<GroupCallServiceOrder> getGroupCallsServiceOrderLiveDataByNumberId(int callNumberId) {
        return new RealmLiveData<GroupCallServiceOrder>(realm.where(GroupCallServiceOrder.class)
                .equalTo(ServiceOrder.CALL_NUMBER_ID, callNumberId)
                .findAllAsync());
    }

    public RealmLiveData<IncompleteRequests> getIncompleteRequestInProgress(int callNumberId) {
        return new RealmLiveData<IncompleteRequests>(realm.where(IncompleteRequests.class)
                .equalTo(IncompleteRequests.CALL_ID, callNumberId)
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .findAllAsync());
    }

    public GroupCallServiceOrder getGroupServiceOrderById(int serviceCallId) {
        return realm.where(GroupCallServiceOrder.class).equalTo(GroupCallServiceOrder.CALL_NUMBER_ID, serviceCallId).findFirst();
    }

    public RealmLiveData<IncompleteCode> getIncompleteCodesLiveData() {
        return new RealmLiveData<IncompleteCode>(realm
                .where(IncompleteCode.class)
                .equalTo("active", true)
                .findAllAsync());
    }

    public RealmLiveData<IncompleteRequests> getIncompleteRequest() {
        return new RealmLiveData<IncompleteRequests>(realm.where(IncompleteRequests.class)
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .sort(IncompleteRequests.DATE_ADDED, Sort.ASCENDING)
                .findAllAsync());
    }

    public RealmResults<IncompleteRequests> getIncompleteRequestSync() {
        return realm.where(IncompleteRequests.class)
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .sort(IncompleteRequests.DATE_ADDED, Sort.ASCENDING)
                .findAll();
    }

    public IncompleteRequests getServiceCallDetails(String callNumberCode, int type) {
        return realm.where(IncompleteRequests.class).equalTo(IncompleteRequests.CALL_NUMBER_CODE, callNumberCode).equalTo(IncompleteRequests.ITEM_TYPE, type).findFirst();
    }

    public RealmLiveData<IncompleteRequests> getIncompleteServiceCallsRequests() {
        return new RealmLiveData<IncompleteRequests>(realm.where(IncompleteRequests.class)
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .equalTo(IncompleteRequests.REQUEST_CATEGORY, Constants.REQUEST_TYPE.SERVICE_CALLS.getValue())
                .findAllAsync());
    }

    public RealmLiveData<IncompleteRequests> getIncompleteActions() {
        return new RealmLiveData<>(realm.where(IncompleteRequests.class).equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .equalTo(IncompleteRequests.REQUEST_CATEGORY, Constants.REQUEST_TYPE.ACTIONS.getValue())
                .findAllAsync());
    }

    public StoredLabor getStoredLabor(int callID) {
        return realm.where(StoredLabor.class).equalTo("callID", callID).findFirst();
    }

    public OrderedRealmCollection<RepairCode> getRepairCodes() {
        return realm.where(RepairCode.class).findAll();
    }


    public ActivityCode getActivityCodeById(int id) {
        return realm.where(ActivityCode.class).equalTo(ActivityCode.ACTIVITY_CODE_ID, id).findFirst();
    }

    public OrderedRealmCollection<HoldCode> getHoldCodes() {
        RealmQuery<HoldCode> where = realm.where(HoldCode.class);
        where = where.notEqualTo("onHoldCodeId", 1).
                notEqualTo("onHoldCodeId", 105).
                notEqualTo("onHoldCodeId", 108).
                notEqualTo("onHoldCodeId", 120);
        return where.findAll().sort("onHoldCode");
    }

    public void changeIncompleteCodeToOriginalState() {
        OrderedRealmCollection<IncompleteCode> incompleteCodesChecked = realm.where(IncompleteCode.class)
                .equalTo(IncompleteCode.COLUMNS.IS_CHECKED, Boolean.TRUE)
                .findAll();
        realm.executeTransaction(realm -> {
            for (IncompleteCode incompleteCode : incompleteCodesChecked) {
                incompleteCode.setChecked(false);
            }
        });
    }

    public RealmLiveData<HoldCode> getHoldCodesWithAllowTechAssign() {
        return new RealmLiveData<HoldCode>(realm.where(HoldCode.class)
                .equalTo(HoldCode.ALLOW_TECH_ASSIGN, Boolean.TRUE)
                .findAll()
                .sort(HoldCode.HOLD_CODE_NAME_QUERY));
    }

    public RealmResults<HoldCode> getHoldCodesWithAllowTechAssignFirstTime() {
        return realm.where(HoldCode.class)
                .equalTo(HoldCode.ALLOW_TECH_ASSIGN, Boolean.TRUE)
                .findAll()
                .sort(HoldCode.HOLD_CODE_NAME_QUERY);
    }

    public OrderedRealmCollection<HoldCode> getHoldCodesWithAllowTechRelease() {
        return realm.where(HoldCode.class)
                .equalTo(HoldCode.ALLOW_TECH_RELEASE, Boolean.TRUE)
                .findAll()
                .sort("onHoldCode");
    }


    public ServiceCallTemporalData getServiceCallTemporaryData(int serviceCallId) {
        ServiceCallTemporalData serviceCallTemporalData;
        ServiceCallTemporalData serviceCallTemporalDataFromRealm = realm
                .where(ServiceCallTemporalData.class)
                .equalTo(ServiceCallTemporalData.COLUMNS.CALL_NUMBER_ID, serviceCallId)
                .findFirst();
        if (serviceCallTemporalDataFromRealm != null) {
            serviceCallTemporalData = serviceCallTemporalDataFromRealm;
        } else {
            ServiceCallTemporalData newServiceCallTemporalData = new ServiceCallTemporalData();
            newServiceCallTemporalData.setId(serviceCallId);
            realm.beginTransaction();
            serviceCallTemporalData = realm.copyToRealmOrUpdate(newServiceCallTemporalData);
            realm.commitTransaction();
        }
        return serviceCallTemporalData;
    }

    public ServiceCallTemporalData getServiceCallTemporaryDataImproved(int serviceCallId) {
        Realm realm = Realm.getDefaultInstance();
        ServiceCallTemporalData serviceCallTemporalData = null;
        try {
            ServiceCallTemporalData serviceCallTemporalDataFromRealm = realm
                    .where(ServiceCallTemporalData.class)
                    .equalTo(ServiceCallTemporalData.COLUMNS.CALL_NUMBER_ID, serviceCallId)
                    .findFirst();
            if (serviceCallTemporalDataFromRealm != null) {
                serviceCallTemporalData = serviceCallTemporalDataFromRealm;
            } else {
                ServiceCallTemporalData newServiceCallTemporalData = new ServiceCallTemporalData();
                newServiceCallTemporalData.setId(serviceCallId);
                realm.beginTransaction();
                serviceCallTemporalData = realm.copyToRealmOrUpdate(newServiceCallTemporalData);
                realm.commitTransaction();
            }
        } catch (Exception e) {
            Log.e("DatabaseRepository", "Exception", e);
        } finally {
            realm.close();
        }
        return serviceCallTemporalData;
    }

    /**
     * This method will remove the local data for a SC
     * Ex: Repair codes added, problems codes added. local used parts added. etc
     *
     * @param serviceCallNumberId (The service call id -> service call number id)
     */
    public void deleteLocalDataForServiceCall(int serviceCallNumberId) {
        changeIncompleteCodeToOriginalState();

        final RealmResults<UsedRepairCode> masterRepairCodes = realm
                .where(UsedRepairCode.class)
                .equalTo(UsedRepairCode.CALL_ID, serviceCallNumberId)
                .findAll();
        final ServiceCallProperty serviceCallProperty = realm.where(ServiceCallProperty.class)
                .equalTo(ServiceCallProperty.CALL_ID, serviceCallNumberId)
                .findFirst();
        final RealmResults<ServiceCallMeter> serviceCallMeters = realm.where(ServiceCallMeter.class)
                .equalTo(ServiceCallMeter.CALL_ID, serviceCallNumberId)
                .findAll();
        final RealmResults<UsedProblemCode> usedProblemCodes = realm.where(UsedProblemCode.class)
                .equalTo(UsedProblemCode.CALL_ID, serviceCallNumberId)
                .findAll();
        final RealmResults<UsedRepairCode> usedRepairCodes = realm.where(UsedRepairCode.class)
                .equalTo(UsedRepairCode.CALL_ID, serviceCallNumberId)
                .findAll();
        final RealmResults<UsedPart> usedPartsToDelete = realm.where(UsedPart.class)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .equalTo(UsedPart.CALL_ID, serviceCallNumberId)
                .findAll();
        final RealmResults<ServiceCallTemporalData> serviceCallTemporalData = realm
                .where(ServiceCallTemporalData.class)
                .equalTo(ServiceCallTemporalData.COLUMNS.CALL_NUMBER_ID, serviceCallNumberId)
                .findAll();
        final RealmResults<UsedPart> sentParts = realm.where(UsedPart.class)
                .equalTo(UsedPart.SENT, true)
                .equalTo(UsedPart.CALL_ID, serviceCallNumberId)
                .findAll();
        realm.executeTransaction(realm -> {
            masterRepairCodes.deleteAllFromRealm();
            if (serviceCallProperty != null) {
                serviceCallProperty.deleteFromRealm();
            }
            if (serviceCallMeters != null) {
                serviceCallMeters.deleteAllFromRealm();
            }
            if (usedProblemCodes != null) {
                usedProblemCodes.deleteAllFromRealm();
            }
            if (usedRepairCodes != null) {
                usedRepairCodes.deleteAllFromRealm();
            }
            if (usedPartsToDelete != null) {
                usedPartsToDelete.deleteAllFromRealm();
            }
            if (serviceCallTemporalData != null) {
                serviceCallTemporalData.deleteAllFromRealm();
            }
            for (UsedPart sentPart : sentParts) {
                sentPart.setDeletable(false);
                sentPart.setLocalUsageStatusId(sentPart.getUsageStatusId());
            }
        });
    }

    public void setInitialStateForCancelCodes() {
        RealmResults<CancelCode> codes = getCancelCodes();
        for (CancelCode code : codes) {
            realm.executeTransaction(realm -> code.setChecked(false));
        }
    }

    /**
     * getActiveServiceCalls()
     * Will return the activeServiceCalls saved in realm
     * ServiceOrders that are not completed
     */
    public RealmResults<ServiceOrder> getActiveServiceCalls() {
        return realm.where(ServiceOrder.class).equalTo(ServiceOrder.COMPLETED, false).findAll();
    }

    public RealmLiveData<GroupCallServiceOrder> getGroupCalls() {
        return new RealmLiveData<>(realm.where(GroupCallServiceOrder.class).findAllAsync());
    }

    public RealmResults<Part> getPartsWithZeroQuality() {
        return realm.where(Part.class).findAll();
    }

    public RealmResults<EquipmentRealmMeter> getEquipmentMeters(int equipmentMeter) {
        return realm.where(EquipmentRealmMeter.class).equalTo(EquipmentMeter.EQUIPMENT_ID, equipmentMeter).findAll();
    }

    public RealmResults<TechnicianWarehousePart> getUsedParts() {
        return realm.where(TechnicianWarehousePart.class).findAll();
    }

    @Nullable
    public TechnicianWarehousePart getTechnicianWarehousePartById(int id) {
        Realm realm = Realm.getDefaultInstance();
        TechnicianWarehousePart result = realm.where(TechnicianWarehousePart.class).equalTo(TechnicianWarehousePart.COLUMNS.ITEM_ID, id).findFirst();
        realm.close();
        return result;
    }

    public OrderedRealmCollection<ServiceOrder> getOrderedServiceOrdersWithDispatchFirst() {
        String[] fieldNames = {ServiceOrder.STATUS_ORDER, ServiceOrder.CALL_DATE};
        Sort[] sort = {Sort.ASCENDING, Sort.DESCENDING};
        return realm.where(ServiceOrder.class).sort(fieldNames, sort).equalTo(ServiceOrder.COMPLETED, false).findAll();
    }

    public OrderedRealmCollection<UsedPart> getNotDeletedNeededPartsFromEAutomate(int serviceOrderId) {
        return realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, serviceOrderId)
                .equalTo(UsedPart.SENT, true)
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, Constants.USED_PART_USAGE_STATUS.NEEDED.getValue())
                .equalTo(UsedPart.DELETABLE, false)
                .findAll();
    }

    public RealmLiveData<Part> getPartsForNeededParts() {
        return new RealmLiveData(realm.where(Part.class)
                .distinct(Part.ITEM_ID)
                .sort(Part.ITEM, Sort.ASCENDING)
                .findAllAsync());
    }


    /**
     * getOrderedServiceOrderWithDispatchFirstSync()
     * will return the serviceOrder list already saved in realm
     * The list will only contain serviceOrders that are not marked as Completed
     */
    public RealmLiveData<ServiceOrder> getOrderedServiceOrderWithDispatchFirstSync() {
        String[] fieldNames = {ServiceOrder.STATUS_ORDER, ServiceOrder.CALL_DATE};
        Sort[] sort = {Sort.ASCENDING, Sort.DESCENDING};
        return new RealmLiveData<ServiceOrder>(realm.where(ServiceOrder.class)
                .sort(fieldNames, sort)
                .equalTo(ServiceOrder.COMPLETED, false)
                .findAllAsync());
    }

    public void deleteNeededPartsAddedLocally(int serviceOrderCallNumberId) {
        RealmResults<UsedPart> neededPartAddedLocally = realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, serviceOrderCallNumberId)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .equalTo(UsedPart.SENT, false)
                .equalTo(UsedPart.USAGE_STATUS_ID, Constants.USED_PART_USAGE_STATUS.NEEDED.getValue())
                .findAll();
        for (UsedPart usedPart : neededPartAddedLocally) {
            realm.executeTransaction(realm -> usedPart.deleteFromRealm());
        }
    }


    public RealmLiveData<ServiceCallLabor> getAllLaborsForServiceCall(int serviceOrderCallNumberId) {
        return new RealmLiveData<ServiceCallLabor>(realm.where(ServiceCallLabor.class)
                .equalTo(ServiceCallLabor.CALL_ID, serviceOrderCallNumberId)
                .findAllAsync());
    }

    public RealmLiveData<AttachmentIncompleteRequest> getIncompleteAttachmentRequestByServiceCall(int serviceOrderCallNumberId) {
        return new RealmLiveData<AttachmentIncompleteRequest>(realm.where(AttachmentIncompleteRequest.class)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .equalTo(AttachmentIncompleteRequest.COLUMNS.CALL_NUMBER_ID, serviceOrderCallNumberId)
                .findAllAsync());
    }

    public RealmLiveData<AttachmentIncompleteRequest> getIncompleteAttachmentRequest() {
        return new RealmLiveData<AttachmentIncompleteRequest>(realm.where(AttachmentIncompleteRequest.class)
                .equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .sort(AttachmentIncompleteRequest.COLUMNS.DATE_ADDED, Sort.ASCENDING)
                .findAllAsync());
    }

    public RealmLiveData<NoteIncompleteRequest> getIncompleteNotesRequest() {
        return new RealmLiveData<NoteIncompleteRequest>(realm.where(NoteIncompleteRequest.class)
                .equalTo(NoteIncompleteRequest.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or()
                .equalTo(NoteIncompleteRequest.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or()
                .equalTo(NoteIncompleteRequest.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .sort(NoteIncompleteRequest.DATE_ADDED, Sort.ASCENDING)
                .findAllAsync());
    }

    public RealmResults<IncompleteRequests> getIncompleteRequestList() {
        return realm.where(IncompleteRequests.class).equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or().equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .or().equalTo(IncompleteRequests.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .findAll();
    }

    public RealmResults<AttachmentIncompleteRequest> getAttachmentIncompleteRequestList() {
        return realm.where(AttachmentIncompleteRequest.class).equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue())
                .or().equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.IN_PROGRESS.getValue())
                .or().equalTo(AttachmentIncompleteRequest.COLUMNS.STATUS, Constants.INCOMPLETE_REQUEST_STATUS.FAIL.getValue())
                .findAll();
    }

    public void deleteDataOnLogout() {
        Realm realmInstance = Realm.getDefaultInstance();
        try {
            realmInstance.executeTransaction(realm -> {
                realm.delete(ActivityCode.class);
                realm.delete(AttachmentIncompleteRequest.class);
                realm.delete(AttachmentItemEntity.class);
                realm.delete(IncompleteRequests.class);
                realm.delete(CallPriorityFilter.class);
                realm.delete(CallTechnicianFilter.class);
                realm.delete(CancelCode.class);
                realm.delete(CompletedServiceOrder.class);
                realm.delete(HoldCode.class);
                realm.delete(NoteIncompleteRequest.class);
                realm.delete(Part.class);
                //realm.delete(PartUsageStatus.class);
                realm.delete(ProblemCode.class);
                realm.delete(RepairCode.class);
                realm.delete(ServiceCallLabor.class);
                realm.delete(ServiceCallProperty.class);
                realm.delete(ServiceCallTemporalData.class);
                realm.delete(ServiceOrder.class);
                realm.delete(StoredLabor.class);
                realm.delete(TechnicianItem.class);
                realm.delete(TechnicianWarehousePart.class);
                realm.delete(UsedPart.class);
                realm.delete(FilterCriteria.class);
                realm.delete(CallPriority.class);
                realm.delete(PayPeriod.class);
                realm.delete(Shift.class);
                realm.delete(ShiftDetails.class);
            });
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION, e);
        }
    }

    public RealmLiveData<ActivityCode> getActivityCodes() {
        return new RealmLiveData<>(realm.where(ActivityCode.class).sort(ActivityCode.ACTIVITY_CODE_NAME_QUERY).findAllAsync());
    }

    public RealmLiveData<UsedProblemCode> getUsedProblemCodes(int orderId) {
        return new RealmLiveData<>(realm.where(UsedProblemCode.class)
                .equalTo(UsedProblemCode.CALL_ID, orderId)
                .sort(UsedProblemCode.PROBLEM_CODE_NAME)
                .findAllAsync());
    }

    public void deleteUsedProblemCode(UsedProblemCode usedProblemCode) {
        realm.executeTransaction(realm -> usedProblemCode.deleteFromRealm());
    }

    public RealmLiveData<UsedRepairCode> getUsedRepairCodes(int orderId) {
        return new RealmLiveData<>(realm.where(UsedRepairCode.class)
                .equalTo(UsedRepairCode.CALL_ID, orderId)
                .sort(UsedRepairCode.REPAIR_CODE_NAME)
                .findAllAsync());
    }

    public void deleteUsedRepairCode(UsedRepairCode usedRepairCode) {
        realm.executeTransaction(realm1 -> usedRepairCode.deleteFromRealm());
    }


    public RealmLiveData<Message> getMessages(String conversationId) {
        return new RealmLiveData<>(realm.where(Message.class).equalTo(Message.CONVERSATION_ID_QUERY_NAME, conversationId).sort("messageTime", Sort.DESCENDING).findAllAsync());
    }

    public RealmLiveData<Conversation> getConversations() {
        return new RealmLiveData<>(realm.where(Conversation.class).sort("updateTime", Sort.DESCENDING).findAllAsync());
    }

    public RealmLiveData<UsedPart> getNeededParts(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class).
                equalTo(UsedPart.CALL_ID, orderId).
                equalTo(UsedPart.USAGE_STATUS_ID, UsedPart.NEEDED_STATUS_CODE).
                findAllAsync());
    }

    public RealmLiveData<UsedPart> getNeededParts(int orderId, int usageStatusId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .notEqualTo(UsedPart.SENT, true)
                .equalTo(UsedPart.USAGE_STATUS_ID, usageStatusId)
                .findAllAsync());
    }

    public RealmLiveData<UsedPart> getNeededPartsByCallId(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.NEEDED_STATUS_CODE)
                .equalTo(UsedPart.ADDED_LOCALLY, false)
                .sort(UsedPart.SENT)
                .findAllAsync());
    }

    public RealmLiveData<UsedPart> getNeededPartsByCallIdForHold(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.NEEDED_STATUS_CODE)
                .sort(UsedPart.SENT)
                .findAllAsync());
    }

    public RealmLiveData<UsedPart> getNeededPartsByCallIdForIncomplete(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.NEEDED_STATUS_CODE)
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .sort(UsedPart.SENT)
                .findAllAsync());
    }

    public RealmLiveData<UsedPart> getPendingPartsByCallId(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.PENDING_STATUS_CODE)
                .findAllAsync());
    }

    public RealmLiveData<UsedPart> getUsedPartsByCallId(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .equalTo(UsedPart.LOCAL_USAGE_STATUS_ID, UsedPart.USED_STATUS_CODE)
                .findAllAsync());
    }


    public RealmLiveData<UsedPart> getNeededParts(int orderId, int usageStatusId, int holdCodeId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .notEqualTo(UsedPart.SENT, true)
                .equalTo(UsedPart.USAGE_STATUS_ID, usageStatusId)
                .equalTo(UsedPart.HOLD_CODE_ID, holdCodeId)
                .findAllAsync());
    }

    public RealmLiveData<UsedPart> getUsedPartsForAssist(int orderId) {
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId)
                .equalTo(UsedPart.USAGE_STATUS_ID, Constants.USED_PART_USAGE_STATUS.USED.getValue())
                .or()
                .equalTo(UsedPart.USAGE_STATUS_ID, Constants.USED_PART_USAGE_STATUS.USED.getValue())
                .equalTo(UsedPart.ADDED_LOCALLY, true)
                .findAllAsync()
        );
    }

    public RealmLiveData<UsedPart> getUsedPartsData(int orderId) {
        String[] fieldNames = {UsedPart.ADDED_LOCALLY, UsedPart.WAREHOUSE_NAME};
        Sort[] sort = {Sort.DESCENDING, Sort.DESCENDING};
        return new RealmLiveData<>(realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, orderId).and()
                .beginGroup()
                .equalTo(UsedPart.USAGE_STATUS_ID, UsedPart.USED_STATUS_CODE)
                .or()
                .beginGroup()
                .equalTo(UsedPart.USAGE_STATUS_ID, UsedPart.PENDING_STATUS_CODE)
                .endGroup()
                .endGroup()
                .sort(fieldNames, sort)
                .findAllAsync()
        );
    }

    public RealmLiveData<TechnicianWarehousePart> getTechnicianWarehouseParts() {
        return new RealmLiveData<>(realm.where(TechnicianWarehousePart.class).findAllAsync());
    }

    public RealmLiveData<TechnicianWarehousePart> getTechnicianWarehousePartsWithoutCustomer() {
        return new RealmLiveData<>(realm.where(TechnicianWarehousePart.class)
                .sort(TechnicianWarehousePart.COLUMNS.ITEM)
                .findAllAsync());
    }

    public RealmResults<UsedPart> getUsedPartfrompart(int partId) {
        Realm ralm1 = Realm.getDefaultInstance();
        RealmResults<UsedPart> parts = realm.where(UsedPart.class).equalTo(UsedPart.ITEM_ID, partId).equalTo(UsedPart.ADDED_LOCALLY, true).findAll();
        ralm1.close();
        return parts;
    }

    public RealmResults<ServiceCallLabor> getCurrentAssistants(int orderId) {
        return realm.where(ServiceCallLabor.class)
                .equalTo("callId", orderId)
                .findAll();
    }

    public void deleteUsedParts(int serviceOrderCallNumberId) {
        Realm realm = Realm.getDefaultInstance();
        final RealmResults<UsedPart> usedPartsToDelete = realm.where(UsedPart.class)
                .equalTo(UsedPart.CALL_ID, serviceOrderCallNumberId)
                .findAll();
        realm.executeTransaction(it -> {
            if (usedPartsToDelete != null) {
                usedPartsToDelete.deleteAllFromRealm();
            }
        });
        realm.close();
    }

    public RealmLiveData<ServiceCallNoteEntity> getServiceCallNotesByCallId(int callId) {
        return new RealmLiveData<>(
                realm.where(ServiceCallNoteEntity.class)
                        .equalTo(ServiceCallNoteEntity.CALL_ID, callId)
                        .sort(ServiceCallNoteEntity.LAST_UPDATE, Sort.DESCENDING)
                        .findAllAsync());
    }
}
