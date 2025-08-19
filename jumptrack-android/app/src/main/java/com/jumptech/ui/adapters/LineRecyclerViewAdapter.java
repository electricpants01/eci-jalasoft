package com.jumptech.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.Line;

import java.util.List;

public class LineRecyclerViewAdapter extends RecyclerView.Adapter<LineRecyclerViewAdapter.ViewHolder> {

    private OnLineListener listener;
    private Context mContext;
    private List<Line> lines;

    /**
     * Stores the information of the delivery
     */
    private Delivery delivery;

    public LineRecyclerViewAdapter(Context context, List<Line> lines, Delivery delivery) {
        this.mContext = context;
        this.lines = lines;
        this.delivery = delivery;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.line_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Line line = lines.get(position);
        holder.bind(line);
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    public void setOnLineClickListener(OnLineListener listener) {
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final ImageView imgLoaded;
        final TextView productName;
        final TextView productSKU;
        final TextView descriptionText;
        final TextView txtQtyLoaded;
        final TextView acceptedQtyText;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            imgLoaded = view.findViewById(R.id.imgLoaded);
            productName = view.findViewById(R.id.productName);
            productSKU = view.findViewById(R.id.productSKU);
            descriptionText = view.findViewById(R.id.descriptionText);
            txtQtyLoaded = view.findViewById(R.id.txtQtyLoaded);
            acceptedQtyText = view.findViewById(R.id.acceptedQtyText);
        }

        public void bind(final Line line) {
            productName.setText(line._name);
            {
                setQuantity(line.isScanCompleted(), line);

                if (delivery.canShowQtyLoaded(line)) {
                    txtQtyLoaded.setText(String.valueOf(line._qty_loaded));
                } else {
                    mView.findViewById(R.id.lyLoaded).setVisibility(View.GONE);
                }
            }

            productSKU.setVisibility(line._product_no != null ? View.VISIBLE : View.GONE);
            productSKU.setText(line._product_no);

            descriptionText.setVisibility(line._desc != null ? View.VISIBLE : View.GONE);
            descriptionText.setText(line._desc);
            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onLineClick(line);
                    }
                }
            });
        }

        private void setQuantity(boolean completed, Line line) {
            if (completed) {
                acceptedQtyText.setText(String.valueOf(line._qty_accept));
                acceptedQtyText.setTextColor(ContextCompat.getColor(mContext, R.color.delivery_line_qty_normal));
            } else {
                acceptedQtyText.setText(itemView.getContext().getString(R.string.format_quantity, line._qty_accept, line._qty_target));
                acceptedQtyText.setTextColor(ContextCompat.getColor(mContext, R.color.delivery_line_qty_off));
            }
        }
    }

    public interface OnLineListener {
        void onLineClick(Line line);
    }
}
