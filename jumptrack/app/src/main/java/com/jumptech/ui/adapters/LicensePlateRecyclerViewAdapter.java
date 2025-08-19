package com.jumptech.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Plate;

import java.util.List;

/**
 * Licences plates item's list adapter
 */
public class LicensePlateRecyclerViewAdapter extends RecyclerView.Adapter<LicensePlateRecyclerViewAdapter.ViewHolder> {

    private static final String LINE_TEMPLATE = "Plate: %s";
    private List<Plate> platesList;

    public LicensePlateRecyclerViewAdapter(final List<Plate> plates) {
        this.platesList = plates;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.license_plate_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String plate = platesList.get(position).plate;
        holder.txtLicensePlate.setText(String.format(LINE_TEMPLATE, plate));
        holder.cbxSelected.setChecked(platesList.get(position).scanned);
    }

    @Override
    public int getItemCount() {
        return this.platesList != null ? this.platesList.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final CheckBox cbxSelected;
        public final TextView txtLicensePlate;
        public final ImageView imgScan;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            cbxSelected = (CheckBox) view.findViewById(R.id.cbxSelected);
            txtLicensePlate = (TextView) view.findViewById(R.id.txtLicensePlate);
            imgScan = (ImageView) view.findViewById(R.id.imgScan);
        }
    }
}
