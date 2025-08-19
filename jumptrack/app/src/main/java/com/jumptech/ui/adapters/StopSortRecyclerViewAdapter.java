package com.jumptech.ui.adapters;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Prompt;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.repository.StopRepository;
import com.jumptech.ui.StopActivity;

import java.util.Collections;
import java.util.List;

public class StopSortRecyclerViewAdapter extends RecyclerView.Adapter<StopSortRecyclerViewAdapter.ViewHolder> implements ViewTouchHelper.ITouchListener{

    private final List<Stop> stopData;
    private ItemTouchHelper itemTouchHelper;
    private Context appContext;

    public StopSortRecyclerViewAdapter(Context appContext, List<Stop> stops) {
        this.appContext = appContext;
        this.stopData = stops;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stop_sort_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Stop stop = stopData.get(position);

        //TODO set for real
        boolean ordered = true;
        changeStopStatusVisibility(holder.stopStatus, ordered);

        holder.invoiceIdText.setText(stop.name);
        Util.changeTextViewColorDependingOnOrder(holder.invoiceIdText, ordered);
        holder.invoiceAddress.setText(stop.address);
        Util.changeTextViewColorDependingOnOrder(holder.invoiceAddress, ordered);

        if (stop.getWindow_id() != null) {
            stop = StopRepository.obtainWindowTime(appContext, stop);
            StopActivity.evaluateWindowTimeList(this.appContext,
                    stop.getWindowTimeList(),
                    stop.getWindowDisplay(),
                    holder.txtWindowDisplay,
                    holder.stopWindowError);
        } else {
            holder.txtWindowDisplay.setVisibility(View.GONE);
            holder.stopWindowError.setVisibility(View.GONE);
        }


        holder.txtWindowDisplay.setVisibility(TextUtils.isEmpty(stop.getWindowDisplay()) ? View.GONE : View.VISIBLE);
        holder.txtWindowDisplay.setText(stop.getWindowDisplay());
        holder.bind();
    }

    @Override
    public int getItemCount() {
        return stopData.size();
    }

    @Override
    public void remove(int position) {
        stopData.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void swap(int firstPosition, int secondPosition){
        Collections.swap(stopData, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    private void changeStopStatusVisibility(View view, boolean ordered) {
        view.setVisibility(ordered ? View.GONE : View.VISIBLE);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView stopStatus;
        public final TextView invoiceIdText;
        public final TextView invoiceAddress;
        public final TextView txtWindowDisplay;
        public final ImageView stopWindowError;
        public final ImageView btnSwap;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            stopStatus = (ImageView) view.findViewById(R.id.stopStatus);
            invoiceIdText = (TextView) view.findViewById(R.id.invoiceIdText);
            invoiceAddress = (TextView) view.findViewById(R.id.invoiceAddress);
            txtWindowDisplay = (TextView) view.findViewById(R.id.window);
            stopWindowError = (ImageView) view.findViewById(R.id.stopWindowError);
            btnSwap = (ImageView) view.findViewById(R.id.chevron);
        }

        public void bind() {
            btnSwap.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(ViewHolder.this);
                    }
                    return false;
                }
            });
        }
    }
}
