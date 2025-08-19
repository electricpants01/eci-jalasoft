package com.jumptech.tracklib.repository;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.jumptech.android.util.Util;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.room.TrackDB;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

public class SignatureRepository {

    public static void signatureFinalize(Context context, String signee, String crumb, Date signed) {
        //grab unique reference for finalized signature
        long signatureKey = -1;
        {
            Cursor cursor = null;
            try {
                cursor = TrackDB.getInstance(context).getSignatureDao().getActiveSignature();
                if (!cursor.moveToFirst()) throw new TrackException("No active signature.");
                signatureKey = cursor.getLong(0);
            } catch (TrackException e) {
                e.printStackTrace();
            } finally {
                Util.close(cursor);
            }
        }

        signatureKey = updateActiveSignature(context, signatureKey, signee, UtilRepository.format(signed), crumb, UUID.randomUUID().toString());
        PhotoRepository.updateActivePhoto(context, signatureKey);

        //determine site key of active delivery
        long siteKey = -1;
        Long baseDeliveryKey = null;
        {
            Cursor cur = null;
            try {
                String queryString =
                        "select max(s._site_key), max(s._base_delivery_key) " +
                                "from " + TrackDB.TBL_DELIVERY + " d " +
                                "join " + TrackDB.TBL_STOP + " s on d._stop_key = s._key " +
                                "where d._signing = 1 ";

                SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString);
                cur = TrackDB.getInstance(context).getGenericDao().genericQuery(query);

                if (!cur.moveToFirst()) throw new TrackException("Could not determine site");
                int c = -1;
                siteKey = cur.getLong(++c);
                baseDeliveryKey = (cur.isNull(++c) ? null : cur.getLong(c));
            } catch (TrackException e) {
                e.printStackTrace();
            } finally {
                Util.close(cur);
            }
        }

        long stopKey = StopRepository.addStop(context, (int) siteKey, signatureKey, baseDeliveryKey);

        DeliveryRepository.updateStopAndSigning(context, stopKey);

    }

    public static long updateActiveSignature(Context context, long signatureKey, String signee, String signed, String crumb, String reference) {
        com.jumptech.tracklib.room.entity.Signature signature = TrackDB.getInstance(context).getSignatureDao().getFromId(signatureKey);
        if (signature != null && signatureKey != Signature.NEW) {
            signature.setSignee(signee);
            signature.setSigned(signed);
            signature.setCrumb(crumb);
            signature.setReference(reference);
            TrackDB.getInstance(context).getSignatureDao().update(signature);
        } else {
            signatureKey = TrackDB.getInstance(context).getSignatureDao().insertActiveSignatureUpdated(signee, signed, crumb, reference);
            if (signature != null) {
                com.jumptech.tracklib.room.entity.Signature signatureToUpdate = TrackDB.getInstance(context).getSignatureDao().getFromId(signatureKey);
                signatureToUpdate.setNote(signature.getNote());
                signatureToUpdate.setPath(signature.getPath());
                TrackDB.getInstance(context).getSignatureDao().update(signatureToUpdate);
            }
        }
        TrackDB.getInstance(context).getSignatureDao().deleteFromId(Signature.NEW);
        return signatureKey;
    }

    public static Signature newSignature(Context context) {
        return fetchSignature(context, Signature.NEW);
    }

    public static Signature fetchSignature(Context context, long signatureKey) {
        Cursor cursor = TrackDB.getInstance(context).getSignatureDao().signature(signatureKey);
        return cursor.moveToFirst() ? signature(cursor) : new Signature();
    }

    public static void signatureNewAssert(Context context) {
        if (TrackDB.getInstance(context).getSignatureDao().getFromId(Signature.NEW) == null)
            TrackDB.getInstance(context).getSignatureDao().insertNewAssert(Signature.NEW, false);
    }

    public static void updateNote(Context context, long id, String note) {
        if (TrackDB.getInstance(context).getSignatureDao().updateNote(id, note) <= 0) {
            com.jumptech.tracklib.room.entity.Signature signature = new com.jumptech.tracklib.room.entity.Signature();
            signature.setNote(note);
            signature.setId(id);
            TrackDB.getInstance(context).getSignatureDao().insert(signature);
        }
    }

    public static void updateImage(Context context, long id, String imagePath) {
        signatureNewAssert(context);
        TrackDB.getInstance(context).getSignatureDao().updatePath(id, imagePath);
    }

    public static Cursor getPathsFromSignature(Context context, long signatureId) {
        return TrackDB.getInstance(context).getSignatureDao().getPathsFromSignature(signatureId);
    }

    public static Signature signature(Cursor cursor) {
        Signature signature = new Signature();
        int c = -1;
        signature._id = cursor.getLong(++c);
        signature._note = cursor.getString(++c);
        signature._signee = cursor.getString(++c);
        signature._path = cursor.getString(++c);
        try {
            signature._signed = UtilRepository.parseDate(cursor.getString(++c));
        } catch (ParseException pe) {
            Log.e("ndb", "signed", pe);
        }
        signature._crumb = cursor.getString(++c);
        signature._reference = cursor.getString(++c);

        return signature;
    }

    public static void deleteFromId(Context context, long id) {
        TrackDB.getInstance(context).getSignatureDao().deleteFromId(id);
    }

    public static void signatureUploaded(Context context, Long signatureKey) {
        TrackDB.getInstance(context).getSignatureDao().updateUploaded(signatureKey);
    }
}
