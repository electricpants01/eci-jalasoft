package com.jumptech.tracklib.data;

import android.content.Context;
import androidx.room.Ignore;
import com.jumptech.tracklib.repository.PlateRepository;
import java.util.List;

public class Line {

    //Core
    public Long _key;
    public Integer _qty_target;
    public Integer _qty_loaded;
    public String _name;
    public String _product_no;
    public String _uom;
    public String _desc;
    public String _scan;

    //Client
    public Integer _qty_accept;
    public String partialReason;
    @Ignore
    public String[] _partial_reason;
    public Boolean _scanning;

    public boolean isScanCompleted() {
        if (_qty_target == null) {
            return true;
        }
        return _qty_accept.equals(_qty_target) || _qty_accept > _qty_target;
    }


    public boolean hasPlates(Context context, Long deliveryKey) {
        final List<Plate> plates = PlateRepository.getPlatesWithoutScan(context, deliveryKey, _key);
        return !plates.isEmpty();
    }
}
