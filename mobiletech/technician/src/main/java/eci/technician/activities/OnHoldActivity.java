package eci.technician.activities;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.os.CountDownTimer;

import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eci.technician.BaseActivity;
import eci.technician.R;
import eci.technician.adapters.HoldCodesListAdapter;
import eci.technician.databinding.ActivityOnHoldBinding;
import eci.technician.helpers.AppAuth;
import eci.technician.helpers.NetworkConnection;
import eci.technician.helpers.api.retroapi.RetrofitRepository;
import eci.technician.interfaces.IHoldCodesListener;
import eci.technician.models.data.UsedPart;
import eci.technician.models.order.HoldCode;
import eci.technician.models.order.IncompleteRequests;
import eci.technician.models.order.ServiceCallLabor;
import eci.technician.models.order.ServiceOrder;
import eci.technician.models.order.StatusChangeModel;
import eci.technician.repository.DatabaseRepository;
import eci.technician.tools.Constants;
import eci.technician.workers.OfflineManager;
import io.realm.Realm;
import io.realm.RealmResults;

public class OnHoldActivity extends BaseActivity implements IHoldCodesListener {
    private ActivityOnHoldBinding binding;
    private int orderId;
    private int equipmentId;
    private String orderStatus;
    private String callNumberCode;
    private ServiceOrder serviceOrder;
    private int serviceOrderCallNumberId;
    private String selectedHoldCode;
    private String getSelectedHoldCodeDescription;
    private int selectedHoldCodeId;
    NetworkConnection connection;
    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        RetrofitRepository.RetrofitRepositoryObject.INSTANCE.getInstance().getHoldCodes();
        selectedHoldCodeId = 0;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_on_hold);

        binding.listHoldCodes.setLayoutManager(new LinearLayoutManager(this));

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (!getIntent().hasExtra(Constants.EXTRA_ORDER_ID)) {
            finish();
            return;
        }
        connection = new NetworkConnection(getBaseContext());
        connection.observe(this, aBoolean -> {
            if (aBoolean) {
                serviceOrder = realm.where(ServiceOrder.class).equalTo(ServiceOrder.CALL_NUMBER_ID, serviceOrderCallNumberId).findFirst();
                new CountDownTimer(1500, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        //do nothing
                    }

                    @Override
                    public void onFinish() {
                        AppAuth.getInstance().setConnected(true);
                    }
                }.start();
            } else {
                AppAuth.getInstance().setConnected(false);
            }
        });

        orderId = getIntent().getIntExtra(Constants.EXTRA_ORDER_ID, 0);
        orderStatus = getIntent().getStringExtra(Constants.EXTRA_ORDER_STATUS);
        equipmentId = getIntent().getIntExtra(Constants.EXTRA_EQUIPMENT_ID, 0);
        callNumberCode = getIntent().getStringExtra(Constants.EXTRA_CALL_NUMBER_CODE);
        serviceOrderCallNumberId = getIntent().getIntExtra(Constants.EXTRA_CALL_NUMBER_ID, 0);
        serviceOrder = realm.where(ServiceOrder.class).equalTo(ServiceOrder.CALL_NUMBER_ID, serviceOrderCallNumberId).findFirst();

        RealmResults<HoldCode> holdCodes = DatabaseRepository.getInstance().getHoldCodesWithAllowTechAssignFirstTime();
        initList(holdCodes);

        DatabaseRepository.getInstance().getHoldCodesWithAllowTechAssign().observe(this, this::initList);
    }

    private void initList(List<HoldCode> items) {
        final HoldCodesListAdapter adapter = new HoldCodesListAdapter(items, this);
        binding.listHoldCodes.setAdapter(adapter);
        if (binding.listHoldCodes.getAdapter() != null) {
            binding.listHoldCodes.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVITY_PART_NEED && resultCode == RESULT_OK && orderId != 0 && selectedHoldCodeId != 0) {
            StatusChangeModel statusChangeModel = new StatusChangeModel();
            statusChangeModel.setActionTime(new Date());
            statusChangeModel.setCallId(orderId);
            statusChangeModel.setHoldCodeTypeId(HoldCode.WAITING_FOR_PARTS_TYPE_ID);
            statusChangeModel.setCodeId(selectedHoldCodeId);
            changeStatus(statusChangeModel);
        }
    }

    private void changeStatus(StatusChangeModel statusChangeModel) {
        if (statusChangeModel.getHoldCodeTypeId() == HoldCode.WAITING_FOR_PARTS_TYPE_ID) {
            RealmResults<UsedPart> usedParts = realm
                    .where(UsedPart.class)
                    .equalTo(UsedPart.CALL_ID, statusChangeModel.getCallId())
                    .equalTo(UsedPart.USAGE_STATUS_ID, 2)
                    .findAll();

            if (usedParts.isEmpty()) {
                showMessageBox("", getString(R.string.no_needed_parts));
                return;
            }
            List<Map<String, Object>> usedPartMaps = new ArrayList<>(usedParts.size());
            for (UsedPart part : usedParts) {
                if (part.isSent()) continue;

                Map<String, Object> map = new HashMap<>();
                map.put("CallId", part.getCallId());
                map.put("ItemId", part.getItemId());
                map.put("Quantity", part.getQuantity());
                map.put("UsageStatusId", part.getUsageStatusId());
                usedPartMaps.add(map);
            }

            statusChangeModel.setUsedParts(usedPartMaps);
        } else {
            statusChangeModel.setUsedParts(new ArrayList<>());
        }
        realm.executeTransaction(realm -> {
            try {
                ServiceCallLabor serviceCallLabor = realm.where(ServiceCallLabor.class).equalTo(ServiceCallLabor.CALL_ID, serviceOrder.getCallNumber_ID()).equalTo(ServiceCallLabor.TECHNICIAN_ID, AppAuth.getInstance().getTechnicianUser().getTechnicianNumber()).findFirst();
                IncompleteRequests incompleteUnDispatchRequests = new IncompleteRequests(UUID.randomUUID().toString());
                incompleteUnDispatchRequests.setActionTime(new Date());
                incompleteUnDispatchRequests.setRequestType("OnHoldCall");
                incompleteUnDispatchRequests.setDateAdded(Calendar.getInstance().getTime());
                incompleteUnDispatchRequests.setCallId(orderId);
                incompleteUnDispatchRequests.setRequestCategory(Constants.REQUEST_TYPE.ON_HOLD_CALLS.getValue());
                incompleteUnDispatchRequests.setStatus(Constants.INCOMPLETE_REQUEST_STATUS.WAITING.getValue());
                incompleteUnDispatchRequests.setHoldCodeId(statusChangeModel.getCodeId());
                incompleteUnDispatchRequests.setCallNumberCode(callNumberCode);
                incompleteUnDispatchRequests.setHoldCodeTypeId(statusChangeModel.getHoldCodeTypeId());
                incompleteUnDispatchRequests.setCallStatusCode(serviceOrder.getStatusCode_Code().trim());
                serviceOrder.setStatusCode_Code("H  ");
                serviceOrder.setOnHoldCode(selectedHoldCode);
                serviceOrder.setOnHoldDescription(getSelectedHoldCodeDescription);
                serviceOrder.setDispatchTime(null);
                serviceOrder.setArriveTime(null);
                if (serviceCallLabor != null) {
                    serviceCallLabor.setDispatchTime(null);
                    serviceCallLabor.setArriveTime(null);
                }
                serviceOrder.setStatusCode(getString(R.string.callStatusOnHold));
                serviceOrder.setStatusOrder(serviceOrder.getStatusOrderForSorting());
                realm.insertOrUpdate(incompleteUnDispatchRequests);

                finish();
            } catch (Exception e) {
                serviceOrder = realm.where(ServiceOrder.class).equalTo(ServiceOrder.CALL_NUMBER_ID, serviceOrderCallNumberId).findFirst();
            }
        });

        if (AppAuth.getInstance().isConnected()) {
            OfflineManager.INSTANCE.retryWorker(this);
        }
    }

    @Override
    public void onHoldCodePressed(@NotNull HoldCode holdCodeItem) {
        if (AppAuth.getInstance().getTechnicianUser().getState() != 1 && AppAuth.getInstance().getTechnicianUser().getState() != 4 && AppAuth.getInstance().getTechnicianUser().getState() != 6) {
            showMessageBox("", getString(R.string.technician_not_clocked_in));
        } else {
            StatusChangeModel statusChangeModel = new StatusChangeModel();
            statusChangeModel.setActionTime(new Date());
            statusChangeModel.setCallId(orderId);

            if (holdCodeItem.isValid()) {
                selectedHoldCode = holdCodeItem.getOnHoldCode();
                getSelectedHoldCodeDescription = holdCodeItem.getDescription();
                selectedHoldCodeId = holdCodeItem.getOnHoldCodeId();
                int selectedHoldCodeTypeId = holdCodeItem.getTypeId();
                statusChangeModel.setCodeId(selectedHoldCodeId);
                statusChangeModel.setHoldCodeTypeId(selectedHoldCodeTypeId);
                if (holdCodeItem.getTypeId() == HoldCode.WAITING_FOR_PARTS_TYPE_ID) {
                    Intent intent = new Intent(OnHoldActivity.this, NeededPartsActivity.class);
                    intent.putExtra(Constants.EXTRA_HOLD_CODE_ID, selectedHoldCodeId);
                    intent.putExtra(Constants.EXTRA_ORDER_ID, orderId);
                    intent.putExtra(Constants.EXTRA_EQUIPMENT_ID, equipmentId);
                    intent.putExtra(Constants.EXTRA_ORDER_STATUS, orderStatus);
                    startActivityForResult(intent, Constants.ACTIVITY_PART_NEED);
                } else {
                    changeStatus(statusChangeModel);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
