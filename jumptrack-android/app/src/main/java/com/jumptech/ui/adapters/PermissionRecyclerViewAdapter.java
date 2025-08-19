package com.jumptech.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jumptech.jumppod.R;
import com.jumptech.ui.SupportActivity;

import java.util.List;

public class PermissionRecyclerViewAdapter extends RecyclerView.Adapter<PermissionRecyclerViewAdapter.ViewHolder> {

    private final SupportActivity.IOnItemClickListener callBack;
    private List<PermissionListItem> permissionList;

    public PermissionRecyclerViewAdapter(List<PermissionListItem> permissionList, SupportActivity.IOnItemClickListener callBack) {
        this.permissionList = permissionList;
        this.callBack = callBack;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.permission_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.permissionListItem.setText(permissionList.get(position).getPermissionTitle());
        holder.permissionListItem.setChecked(permissionList.get(position).isPermissionGranted());
        holder.bind(permissionList.get(position), callBack);
    }

    @Override
    public int getItemCount() {
        return permissionList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final CheckedTextView permissionListItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            permissionListItem = (CheckedTextView) view.findViewById(R.id.permissionListItem);
        }

        public void bind(final PermissionListItem permissionListItem, final SupportActivity.IOnItemClickListener callBack) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callBack.onItemClick(permissionListItem);
                }
            });
        }
    }
}
