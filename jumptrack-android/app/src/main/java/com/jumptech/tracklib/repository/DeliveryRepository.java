package com.jumptech.tracklib.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jumptech.android.util.Util;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.DeliveryType;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.ui.BundleTrack;
import java.util.ArrayList;
import java.util.List;

public class DeliveryRepository {

    public static void storeDelivery(Context context, JsonArray deliveries, int keyStop) {
        for (JsonElement delivery : deliveries)
            storeDelivery(context, delivery.getAsJsonObject(), keyStop);
    }

    private static void storeDelivery(Context context, JsonObject delivery, int keyStop) {
        JsonObject site = delivery.get("site").getAsJsonObject();
        SiteRepository.storeSite(context, site);
        com.jumptech.tracklib.room.entity.Delivery deliveryEntity = new com.jumptech.tracklib.room.entity.Delivery();
        LineRepository.storeLine(context, delivery.get("lines").getAsJsonArray(), delivery.get("key").getAsInt());
        ContentValues cv = UtilRepository.getPrimitiveAsContent(delivery);
        deliveryEntity.setSiteKey(site.get("key").getAsLong());
        deliveryEntity.setStopKey((long) keyStop);
        for (String key : cv.keySet()) {
            switch (key) {
                case "_key":
                    deliveryEntity.setKey(cv.getAsLong(key));
                    break;
                case "_type":
                    deliveryEntity.setType(cv.getAsString(key));
                    break;
                case "_delivery_cd":
                    deliveryEntity.setDeliveryCd(cv.getAsString(key));
                    break;
                case "_delivery_note":
                    deliveryEntity.setDeliveryNote(cv.getAsString(key));
                    break;
                case "_signing":
                    deliveryEntity.setSigning(cv.getAsInteger(key));
                    break;
            }
        }
        TrackDB.getInstance(context).getDeliveryDao().insert(deliveryEntity);
    }

    public static Delivery fetchDeliveryByKey(Context context, Long deliveryKey) {
        return fetchDelivery(context, null, deliveryKey, null);
    }

    public static Delivery fetchDeliveryScan(Context context, String scan) {
        return fetchDelivery(context, null, null, scan);
    }

    public static void removeDelivery(Context context, long deliveryKey) {
        LineRepository.deleteByDeliveryKey(context, deliveryKey);
        deleteByKey(context, deliveryKey);
        StopRepository.removeChildlessStop(context);
    }

    public static void deleteByKey(Context context, long deliveryKey) {
        TrackDB.getInstance(context).getDeliveryDao().deleteByKey(deliveryKey);
    }

    public static Delivery fetchDelivery(Context context, Long stopKey, Long deliveryKey, String deliveryCode) {
        Delivery myDelivery = TrackDB.getInstance(context).getDeliveryDao().fetchDelivery(stopKey,deliveryKey,deliveryCode).get(0);
        myDelivery.type = DeliveryType.valueOf(myDelivery.db_type);
        return myDelivery;
    }

    public static List<Delivery> getAllDeliveriesByStopKey(Context context, Long stopKey){
        List<Delivery> deliveries = TrackDB.getInstance(context).getDeliveryDao().fetchDelivery(stopKey, null, null);
        for( Delivery delivery: deliveries){
            delivery.type = DeliveryType.valueOf(delivery.db_type);
        }
        return deliveries;
    }

    public static void signatureStop(Context context, long stopKey) {
        TrackDB.getInstance(context).getDeliveryDao().signatureStop(stopKey, 1);
    }

    public static void signatureDelivery(Context context, long deliveryKey) {
        TrackDB.getInstance(context).getDeliveryDao().signatureDelivery(deliveryKey, 1);
    }

    public static void signatureDeliveryClear(Context context) {
        TrackDB.getInstance(context).getDeliveryDao().signatureDeliveryClear();
    }

    public static void updateStopAndSigning(Context context, long stopKey) {
        TrackDB.getInstance(context).getDeliveryDao().updateStopAndSigning(stopKey);
    }

    public static long addDelivery(Context context, long stopKey, long siteKey, DeliveryType type) {
        long rowId = TrackDB.getInstance(context).getDeliveryDao().insertEmptyRow();
        long deliveryKey = -rowId;
        TrackDB.getInstance(context).getDeliveryDao().deleteByKey(rowId);
        com.jumptech.tracklib.room.entity.Delivery delivery = TrackDB.getInstance(context).getDeliveryDao().getFromId(deliveryKey);
        if (delivery != null) {
            delivery.setStopKey(stopKey);
            delivery.setSiteKey(siteKey);
            delivery.setDeliveryCd(type.generateId());
            delivery.setType(type.toString());
            TrackDB.getInstance(context).getDeliveryDao().update(delivery);
        } else {
            TrackDB.getInstance(context).getDeliveryDao().insertDelivery(deliveryKey, stopKey, (int )siteKey, type.generateId(), type.toString());
        }
        return deliveryKey;
    }

    public static String signingName(Context context) {
        String signingName = TrackDB.getInstance(context).getDeliveryDao().getSigningName();
        return signingName == null ? "" : signingName;
    }

    public static Long lastScheduledDelivery(Context context) {
        Long lastScheduledDelivery = TrackDB.getInstance(context).getDeliveryDao().getLastScheduledDelivery();
        return lastScheduledDelivery;
    }

    /**
     * Adds delivery into the database and builds the data bundle for an intent
     *
     * @param context          context of application
     * @param baseDeliveryKey  a base delivery's identifier
     * @param deliveryType     a delivery's type
     * @param isNewUnscheduled indicates whether it is a new unscheduled delivery or pickup
     * @return a data bundle
     */
    public static Bundle addDelivery(Context context, long baseDeliveryKey, DeliveryType deliveryType, boolean isNewUnscheduled) {
        Delivery delivery = DeliveryRepository.fetchDeliveryByKey(context, baseDeliveryKey);
        Stop stop = StopRepository.fetchStop(context, delivery.stop_key);

        //long stopKey = addStop(db, stop.site_key, null, delivery.key);
        long stopKey = StopRepository.addStop(context, (int) stop.site_key, null, delivery.id);
        //long deliveryKey = addDelivery(db, stopKey, stop.site_key, deliveryType);
        long deliveryKey = DeliveryRepository.addDelivery(context, stopKey, stop.site_key, deliveryType);

        Bundle bundle = new Bundle();
        bundle.putLong(BundleTrack.KEY_STOP, stopKey);
        bundle.putLong(BundleTrack.KEY_DELIVERY, deliveryKey);
        bundle.putBoolean(BundleTrack.KEY_NEW_UNSCHEDULED, isNewUnscheduled);
        return bundle;
    }

    /**
     * Eliminates the photos and notes for current delivery
     *
     * @param context a valid instance of Context
     */
    public static void deleteMediaDelivery(Context context) {
        List<String> filesToDelete = new ArrayList<>();
        Cursor c = PhotoRepository.getPhotosPath(context);
        if (c.moveToFirst()) {
            collectFileToDelete(c, filesToDelete);
        }
        c = SignatureRepository.getPathsFromSignature(context, Signature.NEW);
        if (c.moveToFirst()) {
            collectFileToDelete(c, filesToDelete);
        }

        PhotoRepository.deleteFromSignature(context, Signature.NEW);
        SignatureRepository.deleteFromId(context, Signature.NEW);

        if (!filesToDelete.isEmpty()) {
            Util.deleteFilesInDir(context.getFilesDir().getAbsolutePath(), filesToDelete);
        }
    }

    private static void collectFileToDelete(Cursor c, List<String> filesToDelete) {
        do {
            filesToDelete.add(c.getString(0));
        } while (c.moveToNext());
    }


}
