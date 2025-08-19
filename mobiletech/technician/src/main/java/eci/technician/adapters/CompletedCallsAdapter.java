package eci.technician.adapters;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eci.technician.databinding.CompletedCallBinding;
import eci.technician.models.order.CompletedServiceOrder;

import java.util.List;

import io.realm.Realm;

public class CompletedCallsAdapter extends RecyclerView.Adapter<CompletedCallsAdapter.ViewHolder> {
    private List<CompletedServiceOrder> items;
    private CompletedCallListener listener;

    public CompletedCallsAdapter(List<CompletedServiceOrder> items, CompletedCallListener listener) {
        this.listener = listener;
        this.items = items;
    }

    public interface CompletedCallListener{
        public void onCompletedCallTap(CompletedServiceOrder completedServiceOrder);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        CompletedCallBinding binding = CompletedCallBinding.inflate(inflater, parent, false);
        return new CompletedCallsAdapter.ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CompletedServiceOrder item = items.get(position);
        holder.binding.setItem(item);
        holder.binding.cardContainer.setOnClickListener(view -> {
            listener.onCompletedCallTap(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CompletedCallBinding binding;

        ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
