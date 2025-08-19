package com.jumptech.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jumptech.jumppod.R;

import java.util.List;

public class PartialReasonsAdapter extends RecyclerView.Adapter<PartialReasonsAdapter.ViewHolder> {

    private List<String> lstReasons;
    private List<Boolean> lstChecked;
    private int mSelectedItem = 0;
    private OnReasonChangedListener reasonChangedListener;
    private boolean enabled = true;

    public PartialReasonsAdapter(List<String> exceptions, List<Boolean> lstChecked, OnReasonChangedListener listener) {
        this.lstReasons = exceptions;
        this.lstChecked = lstChecked;
        reasonChangedListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_reasons_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bind(lstReasons.get(position), position);
    }

    @Override
    public int getItemCount() {
        return lstReasons.size();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        notifyDataSetChanged();
    }

    public String getCurrentItem() {
        return mSelectedItem >= 0 ? lstReasons.get(mSelectedItem) : null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final CheckedTextView cbReason;

        public ViewHolder(View view) {
            super(view);
            cbReason = (CheckedTextView) view.findViewById(R.id.rbOption);
        }

        public void bind(String reason, final int position) {
            cbReason.setText(reason);
            cbReason.setChecked(lstChecked.get(position));
            cbReason.setEnabled(enabled);
            cbReason.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cbReason.setChecked(!cbReason.isChecked());
                    reasonChangedListener.onChanged(position, cbReason.isChecked());
                }
            });
        }
    }

    public interface OnReasonChangedListener {
        void onChanged(int which, boolean isChecked);
    }
}