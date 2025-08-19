package eci.technician.adapters;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eci.technician.databinding.ShiftDetailsBinding;
import eci.technician.models.time_cards.ShiftDetails;

import java.util.List;

public class ShiftsDetailsAdapter extends RecyclerView.Adapter<ShiftsDetailsAdapter.ViewHolder> {
    private List<ShiftDetails> shiftDetails;

    public ShiftsDetailsAdapter(List<ShiftDetails> shiftDetails) {
        this.shiftDetails = shiftDetails;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ShiftDetailsBinding binding = ShiftDetailsBinding.inflate(inflater, parent, false);
        return new ShiftsDetailsAdapter.ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ShiftDetails item = shiftDetails.get(position);
        holder.binding.setItem(item);
    }

    @Override
    public int getItemCount() {
        return shiftDetails.size();
    }

    public void setShiftDetails(List<ShiftDetails> shiftDetails) {
        this.shiftDetails = shiftDetails;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ShiftDetailsBinding binding;

        ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
