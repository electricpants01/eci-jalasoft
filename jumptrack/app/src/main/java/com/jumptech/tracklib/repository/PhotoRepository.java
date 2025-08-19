package com.jumptech.tracklib.repository;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.jumptech.android.util.Util;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Photo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoRepository {

    public static Map<String, List<String>> getPhotosUnsynced(Context context) {
        Cursor cur = null;
        try {
            String queryString = "select g._reference, h._path " +
                    "from " + TrackDB.TBL_PHOTO + " h " +
                    "join " + TrackDB.TBL_SIGNATURE + " g on h._signature_id = g._id " +
                    "where h._uploaded = 0 and g._reference IS NOT NULL";
            SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, null);
            cur = TrackDB.getInstance(context).getPhotoDao().getPhotosUnsynced(query);

            Map<String, List<String>> ref_paths = new HashMap<>();
            while (cur.moveToNext()) {
                int c = -1;
                String ref = cur.getString(++c);
                if (!ref_paths.containsKey(ref)) ref_paths.put(ref, new ArrayList<String>());
                ref_paths.get(ref).add(cur.getString(++c));
            }
            return ref_paths;
        } finally {
            Util.close(cur);
        }
    }

    public static void updatePhoto(Context context, String path, boolean uploaded) {
        TrackDB.getInstance(context).getPhotoDao().updatePhoto(path, uploaded);
    }

    public static void updateActivePhoto(Context context, long signatureId) {
        TrackDB.getInstance(context).getPhotoDao().updateActivePhoto(signatureId);
    }

    public static int photoNewCount(Context context) {
        Cursor cursor = null;
        try {
            cursor = TrackDB.getInstance(context).getPhotoDao().getNewPhotosCount(Signature.NEW);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            Util.close(cursor);
        }
    }

    public static String[] photos(Context context, long signatureKey) {
        Cursor cursor = null;
        try {
            cursor = TrackDB.getInstance(context).getPhotoDao().getPhotosPath(signatureKey);
            List<String> photos = new ArrayList<>();
            while (cursor.moveToNext()) photos.add(cursor.getString(0));
            return photos.toArray(new String[photos.size()]);
        } finally {
            Util.close(cursor);
        }
    }

    public static Cursor getPhotosPath(Context context) {
        return TrackDB.getInstance(context).getPhotoDao().getPhotosPathWithoutOrder(Signature.NEW);
    }

    public static void insertPhoto(Context context, String path) {
        Photo photo = new Photo();
        photo.setPath(path);
        photo.setUploaded(false);
        photo.setSignatureId(Signature.NEW);
        TrackDB.getInstance(context).getPhotoDao().insert(photo);
    }

    public static void deleteFromSignature(Context context, long signaturedId) {
        TrackDB.getInstance(context).getPhotoDao().deleteFromSignature(signaturedId);
    }

}
