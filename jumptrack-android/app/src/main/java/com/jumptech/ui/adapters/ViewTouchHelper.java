package com.jumptech.ui.adapters;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ViewTouchHelper extends ItemTouchHelper.SimpleCallback {
    private ITouchListener mAdapter;

    public ViewTouchHelper(ITouchListener movieAdapter){
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        this.mAdapter = movieAdapter;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        mAdapter.swap(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.remove(viewHolder.getAdapterPosition());
    }
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    public interface ITouchListener {
        void swap(int currentPosition, int targetPosition);
        void remove(int position);
    }
}
