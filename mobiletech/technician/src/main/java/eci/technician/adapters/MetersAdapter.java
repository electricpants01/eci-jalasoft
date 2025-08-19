package eci.technician.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.VolumeShaper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import eci.technician.BaseActivity;
import eci.technician.R;
import eci.technician.helpers.AppAuth;
import eci.technician.models.order.EquipmentMeter;
import eci.technician.models.order.ServiceCallMeter;

import java.util.Locale;

import io.realm.Realm;

public class MetersAdapter extends RecyclerView.Adapter<MetersAdapter.ViewHolder> {
    private EquipmentMeter[] meters;
    private Activity activity;
    private int orderId;
    private Context context;
    private String NULL_METER_VALUE = "0";

    public MetersAdapter() {
    }


    public MetersAdapter(EquipmentMeter[] meters, Activity activity, int orderId) {
        this.meters = meters;
        this.activity = activity;
        this.orderId = orderId;

        Realm realm = Realm.getDefaultInstance();
        for (EquipmentMeter meter : meters) {
            ServiceCallMeter dbMeter = realm.where(ServiceCallMeter.class)
                    .equalTo("serviceOrderId", orderId)
                    .equalTo("meterId", meter.getMeterId())
                    .findFirst();
            if (dbMeter != null) {
                meter.setUserLastMeter(dbMeter.getUserLastValue());
                meter.setMeterSet(true);
            }
        }
        realm.close();
        updateTotal();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        return new MetersAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.meter_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (meters == null) {
            holder.txtName.setText(context.getString(R.string.no_meter_data));
            return;
        }
        EquipmentMeter item = meters[position];
        holder.txtName.setText(String.format("%s %s", item.getMeterType(), (item.isRequiredMeterOnServiceCalls() && item.isRequired()) ? " *" : ""));
        double tempDisplayValue = 0.0;
        if (!AppAuth.getInstance().getTechnicianUser().isAllowMeterForce()) {
            if (item.getDisplay() == null && item.getUserLastMeter() == 0.0)
                holder.txtDisplay.setText(NULL_METER_VALUE);
            else {
                if (item.getDisplay() != null)
                    tempDisplayValue = item.getDisplay();
                if (tempDisplayValue > item.getUserLastMeter()) {
                    holder.txtDisplay.setText(String.format(Locale.getDefault(), "%.0f", tempDisplayValue));
                } else {
                    holder.txtDisplay.setText(String.format(Locale.getDefault(), "%.0f", item.getUserLastMeter()));
                }
            }
        } else {

            if (item.getMeterTypeId() == 6) {
                if (item.getUserLastMeter() != 0) {
                    holder.txtDisplay.setText(String.format(Locale.getDefault(), "%.0f", item.getUserLastMeter()));
                } else {
                    if (item.getDisplay() == null)
                        holder.txtDisplay.setText(NULL_METER_VALUE);
                    else
                        holder.txtDisplay.setText(String.format(Locale.getDefault(), "%.0f", item.getDisplay()));
                }
            } else {
                if (item.isMeterSet()) {
                    holder.txtDisplay.setText(String.format(Locale.getDefault(), "%.0f", item.getUserLastMeter()));
                } else {
                    if (item.getDisplay() == null)
                        holder.txtDisplay.setText(NULL_METER_VALUE);
                    else
                        holder.txtDisplay.setText(String.format(Locale.getDefault(), "%.0f", item.getDisplay()));
                }
            }
        }

        if (item.isMeterSet() && !holder.txtDisplay.getText().equals("-")) {
            holder.txtDisplay.setTextColor(context.getColor(R.color.meterTextColor));
        } else {
            holder.txtDisplay.setTextColor(Color.RED);
        }
    }

    @Override
    public int getItemCount() {
        if (meters == null) return 1;
        return meters.length;
    }

    private void updateMeter(final int adapterPosition) {
        BaseActivity.hideKeyboard(activity);
        final EquipmentMeter meter = meters[adapterPosition];

        if (meter.getDisplay() == null)
            meter.setInitialDisplay(0.0);
        else
            meter.setInitialDisplay(meter.getDisplay());
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.meter_update_dialog, null);

        builder.setView(dialogView);
        final EditText txtMeter = dialogView.findViewById(R.id.txtMeter);
        final TextView lastMeterValue = dialogView.findViewById(R.id.lastMeterValue);
        if (!AppAuth.getInstance().getTechnicianUser().isAllowMeterForce()) {
            if (meter.getDisplay() == null && meter.getUserLastMeter() == 0.0)
                txtMeter.setText("0");
            else {
                double tempDisplayValue = 0.0;
                if (meter.getDisplay() != null) tempDisplayValue = meter.getDisplay();
                if (tempDisplayValue > meter.getUserLastMeter()) {
                    txtMeter.setText(String.format(Locale.getDefault(), "%.0f", meter.getDisplay()));
                } else {
                    txtMeter.setText(String.format(Locale.getDefault(), "%.0f", meter.getUserLastMeter()));
                }
            }
        } else {

            if (meter.getMeterTypeId() == 6) {
                if (meter.getUserLastMeter() != 0) {
                    txtMeter.setText(String.format(Locale.getDefault(), "%.0f", meter.getUserLastMeter()));
                } else {
                    if (meter.getDisplay() == null)
                        txtMeter.setText(NULL_METER_VALUE);
                    else
                        txtMeter.setText(String.format(Locale.getDefault(), "%.0f", meter.getDisplay()));
                }
            } else {
                if (meter.isMeterSet()) {
                    txtMeter.setText(String.format(Locale.getDefault(), "%.0f", meter.getUserLastMeter()));
                } else {
                    if (meter.getDisplay() == null && meter.getUserLastMeter() == 0.0)
                        txtMeter.setText(NULL_METER_VALUE);
                    else
                        txtMeter.setText(String.format(Locale.getDefault(), "%.0f", meter.getDisplay()));
                }
            }
        }
        double tempLastMeterValue = meter.getUserLastMeter();
        if (meter.getDisplay() != null && meter.isMeterSet())
            tempLastMeterValue = meter.getDisplay();
        lastMeterValue.setText(String.format(Locale.getDefault(), "Last meter value: %.0f", tempLastMeterValue));

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog dialog = builder.create();
        final AlertDialog dialogAux = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            txtMeter.requestFocus();
            txtMeter.setSelection(txtMeter.getText().length());
        }

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double meterValue;
                BaseActivity.hideKeyboard(activity);
                try {
                    if (txtMeter.getText().equals(NULL_METER_VALUE))
                        meterValue = 0.0;
                    else
                        meterValue = Double.parseDouble(txtMeter.getText().toString());
                    if (meter.getDisplay() == null)
                        meter.setDisplay(0.0);
                    if (meterValue < meter.getDisplay()) {
                        if (AppAuth.getInstance().getTechnicianUser().isAllowMeterForce()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setTitle("")
                                    .setMessage(R.string.forced_meter_reading_warning)
                                    .setPositiveButton(R.string.force_meter_reading, (dialogInterface, i) -> {
                                        ((HideKeyboardListener) activity).hideKeyboard();
                                        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                meter.setUserLastMeter(meterValue);
                                                meter.setMeterSet(true);
                                            }
                                        });
                                        notifyItemChanged(adapterPosition);
                                        updateTotalForceMeter(meter.getMeterTypeId());
                                        Realm realm = Realm.getDefaultInstance();
                                        ServiceCallMeter dbMeter = realm.where(ServiceCallMeter.class)
                                                .equalTo("serviceOrderId", orderId)
                                                .equalTo("meterId", meter.getMeterId())
                                                .findFirst();
                                        realm.beginTransaction();
                                        if (dbMeter == null) {
                                            dbMeter = new ServiceCallMeter(orderId, meter.getMeterId(), meter.getDisplay(), meterValue);
                                        } else {
                                            dbMeter.setMeterValue(meter.getDisplay());
                                            dbMeter.setUserLastValue(meterValue);
                                        }
                                        realm.insertOrUpdate(dbMeter);
                                        realm.commitTransaction();
                                        realm.close();
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .setCancelable(false);
                            builder.show();
                        } else {
                            Toast.makeText(activity, R.string.meter_smaller_value_error, Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    ((HideKeyboardListener) activity).hideKeyboard();
                    Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            meter.setUserLastMeter(meterValue);
                            meter.setMeterSet(true);
                        }
                    });
                    notifyItemChanged(adapterPosition);
                    if (AppAuth.getInstance().getTechnicianUser().isAllowMeterForce()) {
                        updateTotalForceMeter(meter.getMeterTypeId());
                    } else {
                        updateTotal();
                    }
                    Realm realm = Realm.getDefaultInstance();
                    ServiceCallMeter dbMeter = realm.where(ServiceCallMeter.class)
                            .equalTo("serviceOrderId", orderId)
                            .equalTo("meterId", meter.getMeterId())
                            .findFirst();
                    realm.beginTransaction();
                    if (dbMeter == null) {
                        dbMeter = new ServiceCallMeter(orderId, meter.getMeterId(), meter.getDisplay(), meterValue);
                    } else {
                        dbMeter.setMeterValue(meter.getDisplay());
                        dbMeter.setUserLastValue(meterValue);
                    }
                    realm.insertOrUpdate(dbMeter);
                    realm.commitTransaction();
                    realm.close();
                    dialog.dismiss();

                } catch (NumberFormatException ignored) {
                }
            }
        });
    }

    public interface HideKeyboardListener {
        public void hideKeyboard();
    }


    private void updateTotal() {
        double total = 0;
        EquipmentMeter totalMeter = null;
        int totalMeterId = 0;
        for (int i = 0; i < meters.length; i++) {
            EquipmentMeter meter = meters[i];
            if (meter.getMeterTypeId() == 6) {
                totalMeter = meter;
                totalMeterId = i;
            } else {
                total += meter.getUserLastMeter();
            }
        }

        if (totalMeter != null) {
            final double totalFinal = total;
            EquipmentMeter finalTotalMeter = totalMeter;
            Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    finalTotalMeter.setDisplay(totalFinal);
                    finalTotalMeter.setMeterSet(true);
                }
            });
            notifyItemChanged(totalMeterId);
        }
    }

    private void updateTotalForceMeter(int meterTypeId) {
        double total = 0;
        EquipmentMeter totalMeter = null;
        int totalMeterId = 0;
        for (int i = 0; i < meters.length; i++) {
            EquipmentMeter meter = meters[i];
            if (meter.getMeterTypeId() == 6) {
                totalMeter = meter;
                totalMeterId = i;
            } else {
                total += meter.getUserLastMeter();
            }
        }

        if (totalMeter != null) {
            totalMeter.setDisplay(total);
            totalMeter.setMeterSet(true);
            if (meterTypeId != 6) {
                totalMeter.setUserLastMeter(0);
            }
            notifyItemChanged(totalMeterId);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDisplay;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtDisplay = itemView.findViewById(R.id.txtDisplay);

            if (meters != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateMeter(getAdapterPosition());
                    }
                });
            }
        }
    }
}
