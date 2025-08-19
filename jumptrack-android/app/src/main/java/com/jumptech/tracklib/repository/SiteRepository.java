package com.jumptech.tracklib.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.google.gson.JsonObject;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.room.entity.Site;

public class SiteRepository {

    public static void storeSite(Context context, JsonObject site) {
        ContentValues cv = UtilRepository.getPrimitiveAsContent(site);
        Site siteEntity = new Site();
        for (String key : cv.keySet()) {
            switch (key) {
                case "_key":
                    siteEntity.setKey(cv.getAsInteger(key));
                    break;
                case "_account":
                    siteEntity.setAccount(cv.getAsString(key));
                    break;
                case "_name":
                    siteEntity.setName(cv.getAsString(key));
                    break;
                case "_address1":
                    siteEntity.setAddress1(cv.getAsString(key));
                    break;
                case "_address2":
                    siteEntity.setAddress2(cv.getAsString(key));
                    break;
                case "_address3":
                    siteEntity.setAddress3(cv.getAsString(key));
                    break;
                case "_city":
                    siteEntity.setCity(cv.getAsString(key));
                    break;
                case "_state":
                    siteEntity.setState(cv.getAsString(key));
                    break;
                case "_zip":
                    siteEntity.setZip(cv.getAsString(key));
                    break;
                case "_address":
                    siteEntity.setAddress(cv.getAsString(key));
                    break;
                case "_phone":
                    siteEntity.setPhone(cv.getAsString(key));
            }
        }
        TrackDB.getInstance(context).getSiteDao().insert(siteEntity);
    }

    public static Site fetchSite(Context context, long siteKey) {
        return TrackDB.getInstance(context).getSiteDao().getFromId(siteKey);
    }

    public static void guiPostProcess(Context context) {
        TrackDB.getInstance(context).getSiteDao().updateAddress();
    }

}
