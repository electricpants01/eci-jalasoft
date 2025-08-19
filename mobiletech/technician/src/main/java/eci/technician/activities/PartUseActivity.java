package eci.technician.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import java.util.Locale;
import java.util.UUID;

import eci.technician.BaseActivity;
import eci.technician.R;
import eci.technician.activities.addParts.AddPartsActivity;
import eci.technician.databinding.ActivityPartUseBinding;
import eci.technician.helpers.AppAuth;
import eci.technician.models.data.UsedPart;
import eci.technician.models.order.TechnicianWarehousePart;
import eci.technician.repository.DatabaseRepository;
import eci.technician.tools.Constants;
import io.realm.Realm;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PartUseActivity extends BaseActivity {
    private static final String TAG = "PartUseActivity";
    private static final String EXCEPTION = "Exception logger";
    private int orderId;
    private int partId;
    private int warehouseId;
    private String partName;
    private String partDescription;
    private int holdCodeId = 0;
    private boolean isRequestedPart;
    private Realm realm;
    private boolean addPartAsPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        ActivityPartUseBinding binding;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_part_use);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (!getIntent().hasExtra(Constants.EXTRA_ORDER_ID) && !getIntent().getBooleanExtra(Constants.EXTRA_REQUEST, false)) {
            finish();
            return;
        }

        double availableQuantity;
        boolean request;

        orderId = getIntent().getIntExtra(Constants.EXTRA_ORDER_ID, 0);
        partId = getIntent().getIntExtra(Constants.EXTRA_PART_ID, 0);
        request = getIntent().getBooleanExtra(Constants.EXTRA_REQUEST, false);
        isRequestedPart = getIntent().getBooleanExtra(Constants.REQUESTED_PARTS, false);
        partName = getIntent().getStringExtra(Constants.EXTRA_PART_NAME);
        warehouseId = getIntent().getIntExtra(Constants.WAREHOUSE_ID, 0);
        addPartAsPending = getIntent().getBooleanExtra(AddPartsActivity.EXTRA_PENDING_PART, false);
        partDescription = getIntent().getStringExtra(Constants.EXTRA_PART_DESCRIPTION);
        availableQuantity = getIntent().getDoubleExtra(Constants.EXTRA_AVAILABLE_QUANTITY, 0);
        holdCodeId = getIntent().getIntExtra(Constants.EXTRA_HOLD_CODE_ID, 0);

        binding.txtAvailable.setText(String.format(Locale.getDefault(), "%.0f", availableQuantity));
        binding.editTxtDescription.setText(partDescription);
        binding.txtQuantity.setText("1");

        binding.btnUse.setVisibility(request ? GONE : VISIBLE);
        binding.btnNeed.setVisibility(request ? VISIBLE : GONE);
        binding.editTxtDescription.setVisibility(request && AppAuth.getInstance().getTechnicianUser().isAllowUnknownItems() && !isRequestedPart ? VISIBLE : GONE);
        binding.partDescriptionTxt.setVisibility(request && AppAuth.getInstance().getTechnicianUser().isAllowUnknownItems() && !isRequestedPart ? VISIBLE : GONE);

        binding.btnUse.setOnClickListener(view -> {
            double quantity;
            String description;
            try {
                description = binding.editTxtDescription.getText().toString();
                quantity = Double.parseDouble(binding.txtQuantity.getText().toString());
                if (quantity == 0) {
                    throw new IllegalArgumentException();
                }
                if (quantity > availableQuantity) {
                    showWarningMessage(quantity, description);
                    return;
                }
                if (addPartAsPending) {
                    addPendingPart(quantity, description);
                    return;
                }
                usePart(quantity, false, description);


            } catch (Exception ex) {
                Log.e(TAG, EXCEPTION, ex);
                binding.txtQuantity.setError(getResources().getString(R.string.quantity_error));
            }
        });

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnNeed.setOnClickListener(v -> {
            double quantity;
            String description;
            try {
                description = binding.editTxtDescription.getText().toString();
                quantity = Double.parseDouble(binding.txtQuantity.getText().toString());
                if (quantity == 0) {
                    throw new IllegalArgumentException();
                }
                usePart(quantity, true, description);
            } catch (Exception ex) {
                Log.e(TAG, EXCEPTION, ex);
                binding.txtQuantity.setError(getResources().getString(R.string.quantity_error));
            }
        });
    }

    private void addPendingPart(double quantity, String description) {
        TechnicianWarehousePart partFromTechnicianWarehouse = DatabaseRepository.getInstance().getTechnicianWarehousePartById(partId);

        Realm realmInstance = Realm.getDefaultInstance();
        try {
            UsedPart usedPart = new UsedPart();
            usedPart.setCallId(orderId);
            usedPart.setItemId(partId);
            usedPart.setQuantity(quantity);
            usedPart.setLocalUsageStatusId(UsedPart.PENDING_STATUS_CODE);
            usedPart.setUsageStatusId(UsedPart.PENDING_STATUS_CODE);
            usedPart.setSent(false);
            usedPart.setAddedLocally(true);
            usedPart.setPartName(partName);
            usedPart.setWarehouseID(AppAuth.getInstance().getTechnicianUser().getWarehouseId());
            usedPart.setCustomId(UUID.randomUUID().toString());
            usedPart.setActionType("insert");
            usedPart.setWarehouseName("");
            usedPart.setPartDescription(partDescription);

            realmInstance.executeTransaction(realm1 -> {
                realm1.insertOrUpdate(usedPart);
                if (partFromTechnicianWarehouse != null) {
                    double currentUsed = partFromTechnicianWarehouse.getUsedQty();
                    partFromTechnicianWarehouse.setUsedQty((int) (currentUsed + quantity));
                }
            });
        }catch (Exception e){
            Log.e(TAG, EXCEPTION, e);
        }finally {
            realmInstance.close();
        }

        finish();
    }

    private void showWarningMessage(final double quantity, final String description) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.negative_quantity);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> usePart(quantity, false, description));
        builder.setNegativeButton(R.string.back, null);
        builder.create().show();
    }

    private void usePart(double quantity, boolean need, String description) {

        if (!isRequestedPart) {
            Realm realmInstance = Realm.getDefaultInstance();
            TechnicianWarehousePart partFromTechnicianWarehouse = DatabaseRepository.getInstance().getTechnicianWarehousePartById(partId);

            UsedPart usedPart = new UsedPart();
            usedPart.setCallId(orderId);
            usedPart.setItemId(partId);
            usedPart.setQuantity(quantity);
            usedPart.setLocalUsageStatusId(need ? 2 : 1);
            usedPart.setUsageStatusId(need ? 2 : 1);
            usedPart.setSent(false);
            usedPart.setAddedLocally(true);
            usedPart.setPartName(partName);
            usedPart.setWarehouseID(warehouseId);
            usedPart.setCustomId(UUID.randomUUID().toString());
            usedPart.setActionType("insert");
            usedPart.setWarehouseName("");
            if (holdCodeId != 0) {
                usedPart.setHoldCodeId(holdCodeId);
            }
            if (need && AppAuth.getInstance().getTechnicianUser().isAllowUnknownItems()) {
                usedPart.setLocalDescription(description);
                usedPart.setPartDescription(description);
            } else {
                usedPart.setPartDescription(partDescription);
            }
            usedPart.setAddedLocally(true);
            realmInstance.executeTransaction(realm1 -> {
                realm1.insertOrUpdate(usedPart);
                if (partFromTechnicianWarehouse != null) {
                    double currentUsed = partFromTechnicianWarehouse.getUsedQty();
                    partFromTechnicianWarehouse.setUsedQty((int) (currentUsed + quantity));
                }
            });
            realmInstance.close();
        }
        getIntent().putExtra(Constants.EXTRA_QUANTITY, quantity);
        setResult(RESULT_OK, getIntent());
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
