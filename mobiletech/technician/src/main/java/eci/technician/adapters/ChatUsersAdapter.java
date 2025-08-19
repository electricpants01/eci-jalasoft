package eci.technician.adapters;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import eci.signalr.messenger.ConversationUser;
import eci.technician.databinding.ChatUserBinding;

import java.util.ArrayList;
import java.util.List;

public class ChatUsersAdapter extends RecyclerView.Adapter<ChatUsersAdapter.ViewHolder> implements Filterable {
    private List<ConversationUser> baseItems;
    private List<ConversationUser> actualItems;
    private IChatUserSelectedListener listener;
    private Filter filter;

    public ChatUsersAdapter(List<ConversationUser> items, IChatUserSelectedListener listener) {
        this.baseItems = items;
        this.actualItems = items;
        this.listener = listener;
        this.filter = new MessagingUserFilter();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ChatUserBinding binding = ChatUserBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ConversationUser item = actualItems.get(position);
        holder.binding.setUser(item);
    }

    @Override
    public int getItemCount() {
        return actualItems.size();
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    public interface IChatUserSelectedListener {
        void onSelect(ConversationUser user);
    }

    public interface IChatUserClickListener {
        void onClick(View view);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ChatUserBinding binding;

        public ViewHolder(final View itemView) {
            super(itemView);

            binding = DataBindingUtil.bind(itemView);
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onSelect(actualItems.get(getAdapterPosition()));
                    }
                }
            });
        }
    }

    private class MessagingUserFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();

            String filterString = charSequence.toString().trim().toLowerCase();

            if (filterString.isEmpty()) {
                results.values = baseItems;
                results.count = baseItems.size();
            } else {
                List<ConversationUser> filteredList = new ArrayList<>();
                int count = baseItems.size();
                for (int i = 0; i < count; i++) {
                    ConversationUser user = baseItems.get(i);
                    if (user.getChatName().toLowerCase().contains(filterString)) {
                        filteredList.add(user);
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            actualItems = ((List<ConversationUser>) filterResults.values);
            notifyDataSetChanged();
        }
    }
}