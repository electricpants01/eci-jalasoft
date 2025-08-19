package com.jumptech.tracklib.data;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Ignore;

import com.jumptech.tracklib.repository.LineRepository;

import java.util.ArrayList;
import java.util.List;

public class Delivery {

    public long id;
    public long stop_key;
    public String name;
    public String address;
    public String display;
    public String note;
    public String db_type;
    @Ignore
    public DeliveryType type;
    public int line_count;

    @Ignore
    public List<Line> lines = new ArrayList<>();
    @Ignore
    public boolean hasPlates;

    public boolean isEditable() {
        return id < 0;
    }

    /**
     * Indicates if the delivery's line can show the qty loaded
     *
     * @param line line to evaluate
     * @return a Bollean
     */
    public boolean canShowQtyLoaded(Line line) {
        return !isEditable() && line._qty_loaded >= 0 && !line._qty_loaded.equals(line._qty_target);
    }

    public boolean hasLines(Context context) {
        return !LineRepository.fetchLines(context,id, null, null).isEmpty();
    }

    public void loadLines(Context context) {
        List<Line> lines = LineRepository.fetchLines(context, id, null, null);
        for(Line line: lines){
            this.hasPlates = line.hasPlates(context, id);
        }
        this.lines = lines;
    }

    public int numberLinesPlatesScanCompleted() {
        int sum = 0;
        for (Line line : lines) {
            if (line.isScanCompleted()) sum++;
        }
        return sum;
    }

    public boolean isLinesPlatesScanCompleted() {
        return numberLinesPlatesScanCompleted() == line_count;
    }
}
