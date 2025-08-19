package eci.technician.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import eci.technician.databinding.UnnavailablePartItemBinding;
import eci.technician.models.data.Parts;

public class MissingPartsAdapter extends RecyclerView.Adapter<MissingPartsAdapter.ViewHolder> {
    private List<Parts> missingParts;
    private String header;

    public MissingPartsAdapter(List<Parts> missingParts) {
        this.missingParts = missingParts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        UnnavailablePartItemBinding binding = UnnavailablePartItemBinding.inflate(inflater, parent, false);
        return new MissingPartsAdapter.ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Parts item = missingParts.get(position);
        holder.binding.setItem(item);
    }

    @Override
    public int getItemCount() {
        return missingParts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        UnnavailablePartItemBinding binding;

        ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
