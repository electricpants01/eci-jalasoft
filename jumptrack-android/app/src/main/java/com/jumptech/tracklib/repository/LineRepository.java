package com.jumptech.tracklib.repository;

import android.content.ContentValues;
import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jumptech.tracklib.data.Line;
import com.jumptech.tracklib.data.LineType;
import com.jumptech.tracklib.data.Product;
import com.jumptech.tracklib.room.TrackDB;
import java.util.ArrayList;
import java.util.List;

public class LineRepository {

    public static Line fetchLine(Context context, Long deliveryKey, String scan) {
        List<Line> lines = TrackDB.getInstance(context).getLineDao().getLines(deliveryKey, null, scan);
        if( lines.size() > 0){
            Line myLine = lines.get(0);
            myLine._partial_reason = UtilRepository.splitNotNull(myLine.partialReason, ";");
            return myLine;
        }
        return null;
    }

    public static List<Line> fetchLines(Context context, Long deliveryKey, Long lineKey, String scan){
        List<Line> lines = TrackDB.getInstance(context).getLineDao().getLines(deliveryKey, lineKey, scan);
        for( Line line : lines){
            line._partial_reason = UtilRepository.splitNotNull(line.partialReason, ";");
        }
        return lines;
    }

    public static void deleteByKey(Context context, long lineKey) {
        TrackDB.getInstance(context).getLineDao().deleteByKey(lineKey);
    }

    public static void deleteByDeliveryKey(Context context, long deliveryKey) {
        TrackDB.getInstance(context).getLineDao().deleteByDeliveryKey(deliveryKey);
    }

    public static void updateQty(Context context, long lineKey) {
        TrackDB.getInstance(context).getLineDao().updateScannedLine(lineKey);
    }

    public static Line fetchByID(Context context, long lineKey) {
        Line line = TrackDB.getInstance(context).getLineDao().getByID(lineKey);
        line._partial_reason = UtilRepository.splitNotNull(line.partialReason, ";");
        line._scanning = line._scanning != null && line._scanning;
        return line;
    }

    public static long addLine(Context context, long deliveryKey, Product product) {
        long rowId = TrackDB.getInstance(context).getLineDao().insertEmptyRow();
        long lineKey = -rowId;
        TrackDB.getInstance(context).getLineDao().deleteByKey(rowId);
        Long myLineId = TrackDB.getInstance(context).getLineDao().getFromId(lineKey);
        if ( myLineId != null) {
            TrackDB.getInstance(context).getLineDao().update(lineKey
                    , deliveryKey
                    , null
                    , product.name
                    , product.no
                    , product.uom
                    , product.desc
                    , 1);
        } else {
            TrackDB.getInstance(context).getLineDao().insert(lineKey
                    , deliveryKey
                    , null
                    , product.name
                    , product.no
                    , product.uom
                    , product.desc
                    , 1);
        }
        return lineKey;
    }

    public static void storeLine(Context context, JsonArray lines, int keyDelivery) {
        for (JsonElement line : lines)
            LineRepository.storeLine(context, line.getAsJsonObject(), keyDelivery);
    }

    private static void storeLine(Context context, JsonObject line, int keyDelivery) {
        LineType lineType = LineType.fromJsonObject(line);
        com.jumptech.tracklib.room.entity.Line lineEntity = new com.jumptech.tracklib.room.entity.Line();
        ContentValues cv = UtilRepository.getPrimitiveAsContent(line);
        lineEntity.setDeliveryKey(keyDelivery);
        lineEntity.setQtyAccept(lineType == LineType.LICENSE_PLATE ? 0 : cv.getAsInteger("_qty_target"));
        lineEntity.setScanning(false);
        if (!cv.containsKey("_qty_loaded")) {
            lineEntity.setQtyLoaded(-1);
        }
        for (String key : cv.keySet()) {
            switch (key) {
                case "_key":
                    lineEntity.setKey(cv.getAsLong(key));
                    break;
                case "_qty_target":
                    lineEntity.setQtyTarget(cv.getAsInteger(key));
                    break;
                case "_name":
                    lineEntity.setName(cv.getAsString(key));
                    break;
                case "_product_no":
                    lineEntity.setProductNo(cv.getAsString(key));
                    break;
                case "_uom":
                    lineEntity.setUom(cv.getAsString(key));
                    break;
                case "_desc":
                    lineEntity.setDesc(cv.getAsString(key));
                    break;
                case "_scan":
                    lineEntity.setScan(cv.getAsString(key));
                    break;
                case "_partial_reason":
                    lineEntity.setPartialReason(cv.getAsString(key));
                    break;
                case "_qty_loaded":
                    lineEntity.setQtyLoaded(cv.getAsInteger(key));
                    break;
            }
        }

        TrackDB.getInstance(context).getLineDao().insert(lineEntity);

        JsonElement je = line.get("license-plate");
        if (je != null && je.isJsonArray())
            PlateRepository.storePlate(context, je.getAsJsonArray(), line.get("key").getAsLong());

    }

    public static void updateQtyAndPartial(Context context, long lineKey, int acceptQty, ArrayList<String> partialReason) {
        StringBuilder sbReasons = null;
        for (String reason : partialReason) {
            if (sbReasons == null) sbReasons = new StringBuilder();
            else sbReasons.append(";");
            sbReasons.append(reason);
        }

        TrackDB.getInstance(context).getLineDao().updateQtyAndPartial(lineKey, acceptQty, sbReasons == null ? null : sbReasons.toString());
    }

}
