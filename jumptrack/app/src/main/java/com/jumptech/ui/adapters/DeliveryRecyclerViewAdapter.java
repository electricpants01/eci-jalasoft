package com.jumptech.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Delivery;
import java.util.List;
import java.util.Set;

public class DeliveryRecyclerViewAdapter extends RecyclerView.Adapter<DeliveryRecyclerViewAdapter.ViewHolder> {

    private final List<Delivery> deliveryData;
    private OnDeliveryListener listener;
    private Set<Long> deliveriesIdChecked;
    private boolean partialEnabled;

    public DeliveryRecyclerViewAdapter(List<Delivery> deliveries, Set<Long> deliveriesIdChecked) {
        this.deliveryData = deliveries;
        this.deliveriesIdChecked = deliveriesIdChecked;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.delivery_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Delivery delivery = deliveryData.get(position);
        holder.bind(delivery);
    }

    @Override
    public int getItemCount() {
        return deliveryData.size();
    }

    public void setOnDeliveryClickListener(OnDeliveryListener listener) {
        this.listener = listener;
    }

    public List<Delivery> getDeliveries() {
        return deliveryData;
    }

    public Set<Long> getDeliveriesIdChecked() {
        return deliveriesIdChecked;
    }

    public void setPartialEnable(boolean enabled) {
        partialEnabled = enabled;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final ImageView icon;
        final TextView invoiceIdText;
        final TextView invoiceNotesText;
        final TextView packageCountText;
        final CheckBox cbDeliverySelected;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            icon = view.findViewById(R.id.listicon);
            invoiceIdText = view.findViewById(R.id.invoiceIdText);
            invoiceNotesText = view.findViewById(R.id.invoiceIdNotes);
            packageCountText = view.findViewById(R.id.packageCountText);
            cbDeliverySelected = view.findViewById(R.id.cbxDeliverySelected);
        }

        public void bind(final Delivery delivery) {
            icon.setImageResource(delivery.type.getIcon());

            invoiceIdText.setText(delivery.display);
            invoiceIdText.setTextColor(ContextCompat.getColor(itemView.getContext(), delivery.type.getColor()));

            invoiceNotesText.setText(delivery.note);
            invoiceNotesText.setVisibility(delivery.note == null ? View.GONE : View.VISIBLE);

            if (delivery.hasPlates) {
                setQuantity(delivery, delivery.isLinesPlatesScanCompleted());
            } else {
                setQuantity(delivery, true);
            }

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDeliveryClick(delivery);
                    }
                }
            });
            cbDeliverySelected.setVisibility(partialEnabled ? View.VISIBLE : View.GONE);
            cbDeliverySelected.setOnCheckedChangeListener(null);
            cbDeliverySelected.setChecked(deliveriesIdChecked.contains(delivery.id));
            cbDeliverySelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        deliveriesIdChecked.add(delivery.id);
                    } else {
                        deliveriesIdChecked.remove(delivery.id);
                    }
                    if (listener != null && deliveriesIdChecked.size() < 2) {
                        listener.onDeliveryChecked(delivery, deliveriesIdChecked);
                    }
                }
            });
        }

        private void setQuantity(Delivery delivery, boolean completed) {
            if (completed) {
                packageCountText.setText(String.valueOf(delivery.line_count));
                packageCountText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.delivery_line_qty_normal));
            } else {
                packageCountText.setText(itemView.getContext().getString(R.string.format_quantity, delivery.numberLinesPlatesScanCompleted(), delivery.line_count));
                packageCountText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.delivery_line_qty_off));
            }
        }
    }

    public interface OnDeliveryListener {
        void onDeliveryClick(Delivery delivery);

        void onDeliveryChecked(Delivery delivery, Set<Long> deliveriesChecked);
    }
}
