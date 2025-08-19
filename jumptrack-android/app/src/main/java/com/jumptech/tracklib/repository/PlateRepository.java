package com.jumptech.tracklib.repository;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jumptech.tracklib.data.Plate;
import com.jumptech.tracklib.room.TrackDB;
import java.util.List;

public class PlateRepository {

    public static void storePlate(Context context, JsonArray plates, long keyLine) {
        com.jumptech.tracklib.room.entity.Plate plateEntity;
        for (JsonElement plate : plates) {
            if (!plate.isJsonNull()) {
                TrackDB.getInstance(context).getPlateDao().insertPlate(keyLine, plate.getAsString());
            }
        }
    }

    public static boolean updatePlateScan(Context context, Plate plate) {
        if (plate.scanned) return false;
        int result = updateScannedPlate(context, plate.plate_key, true);
        if (result == 0) return false;
        LineRepository.updateQty(context, plate.line_key);
        return true;
    }

    public static int updateScannedPlate(Context context, long rowId, boolean scanned) {
        return TrackDB.getInstance(context).getPlateDao().updateScannedPlate(rowId, scanned);
    }

    public static List<Plate> getPlatesWithoutScan(Context context, Long deliveryKey, Long lineKey) {
        return getPlates(context, deliveryKey, lineKey, null, null);
    }

    public static Plate fetchPlateWitScan(Context context, Long deliveryKey, String scan) {
        return TrackDB.getInstance(context).getPlateDao().getPlates(deliveryKey, null, scan, null).get(0);
    }

    public static List<Plate> getPlateByLineId(Context context, Long lineId){
        return TrackDB.getInstance(context).getPlateDao().getPlates(null, lineId, null, null);
    }

    private static List<Plate> getPlates(Context context, Long deliveryKey, Long lineKey, String scan, Boolean scanned) {
        return TrackDB.getInstance(context).getPlateDao().getPlates(deliveryKey, lineKey, scan, scanned);

    }

    public static void updatePlateResetStop(Context context, long stopKey) {
        DeliveryRepository.deleteMediaDelivery(context);
        DeliveryRepository.signatureDeliveryClear(context);
        TrackDB.getInstance(context).getPlateDao().resetPlate(stopKey);
        TrackDB.getInstance(context).getLineDao().resetLine(stopKey);
    }
}
