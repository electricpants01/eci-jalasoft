package com.jumptech.tracklib.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jumptech.tracklib.room.dao.CrumbDao;
import com.jumptech.tracklib.room.dao.DeliveryDao;
import com.jumptech.tracklib.room.dao.GenericDao;
import com.jumptech.tracklib.room.dao.LineDao;
import com.jumptech.tracklib.room.dao.PhotoDao;
import com.jumptech.tracklib.room.dao.PlateDao;
import com.jumptech.tracklib.room.dao.PromptDao;
import com.jumptech.tracklib.room.dao.RouteDao;
import com.jumptech.tracklib.room.dao.SignatureDao;
import com.jumptech.tracklib.room.dao.SiteDao;
import com.jumptech.tracklib.room.dao.StopDao;
import com.jumptech.tracklib.room.dao.WindowDao;
import com.jumptech.tracklib.room.dao.WindowTimeDao;
import com.jumptech.tracklib.room.entity.Crumb;
import com.jumptech.tracklib.room.entity.Delivery;
import com.jumptech.tracklib.room.entity.Line;
import com.jumptech.tracklib.room.entity.Photo;
import com.jumptech.tracklib.room.entity.Plate;
import com.jumptech.tracklib.room.entity.Prompt;
import com.jumptech.tracklib.room.entity.Route;
import com.jumptech.tracklib.room.entity.Signature;
import com.jumptech.tracklib.room.entity.Site;
import com.jumptech.tracklib.room.entity.Stop;
import com.jumptech.tracklib.room.entity.Window;
import com.jumptech.tracklib.room.entity.WindowTime;

import org.jetbrains.annotations.NotNull;

@Database(entities = {Delivery.class, Line.class, Photo.class, Plate.class,
        Prompt.class, Route.class, Signature.class, Site.class,
        Stop.class, Window.class, WindowTime.class, Crumb.class}, version = 6, exportSchema = false)
public abstract class TrackDB extends RoomDatabase {


    public static final String DB = "track.db";
    public static final String TBL_DELIVERY = "delivery";
    public static final String TBL_LINE = "line";
    public static final String TBL_ROUTE = "route";
    public static final String TBL_SIGNATURE = "signature";
    public static final String TBL_SITE = "site";
    public static final String TBL_STOP = "stop";
    public static final String TBL_WINDOW = "window";
    public static final String TBL_WINDOW_TIME = "windowTime";
    public static final String TBL_PHOTO = "photo";
    public static final String TBL_PLATE = "plate";
    public static final String TBL_PROMPT = "prompt";
    public static final String TBL_CRUMB = "crumb";

    public static final String[] TABLES = new String[]{TBL_DELIVERY, TBL_LINE, TBL_PHOTO, TBL_PLATE, TBL_ROUTE, TBL_SIGNATURE, TBL_SITE, TBL_STOP, TBL_PROMPT, TBL_WINDOW, TBL_WINDOW_TIME};

    private static final String DB_NAME = "track.db";

    private static TrackDB databaseInstance;

    public abstract DeliveryDao getDeliveryDao();

    public abstract LineDao getLineDao();

    public abstract PhotoDao getPhotoDao();

    public abstract PlateDao getPlateDao();

    public abstract PromptDao getPromptDao();

    public abstract RouteDao getRouteDao();

    public abstract SignatureDao getSignatureDao();

    public abstract SiteDao getSiteDao();

    public abstract StopDao getStopDao();

    public abstract WindowDao getWindowDao();

    public abstract WindowTimeDao getWindowTimeDao();

    public abstract GenericDao getGenericDao();

    public abstract CrumbDao getCrumbDao();

    public static TrackDB getInstance(Context context) {
        if (databaseInstance == null) {
            databaseInstance = Room.databaseBuilder(context.getApplicationContext(), TrackDB.class, DB_NAME)
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build();
        }
        return databaseInstance;
    }

    // DB Migrations
    private static final Migration MIGRATION_2_3 = new Migration(3, 4) {
        @Override
        public void migrate(
                @NotNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE delivery_temp (_key INTEGER, _stop_key INTEGER, _site_key INTEGER, _type TEXT,_delivery_cd TEXT, _delivery_note TEXT , _signing INTEGER,  PRIMARY KEY(_key))");
            database.execSQL("INSERT INTO delivery_temp (_key, _stop_key, _site_key, _type, _delivery_cd, _delivery_note, _signing) SELECT _key, _stop_key, _site_key, _type, _delivery_cd, _delivery_note, _signing FROM delivery");
            database.execSQL("ALTER TABLE delivery_temp RENAME TO delivery_new");

            database.execSQL("CREATE TABLE line_temp (_key INTEGER, _delivery_key INTEGER, _qty_target INTEGER, _name TEXT,_product_no TEXT, _uom TEXT , _desc TEXT, _scan TEXT,_qty_accept INTEGER, _partial_reason TEXT, _scanning INTEGER, _qty_loaded INTEGER, PRIMARY KEY(_key))");
            database.execSQL("INSERT INTO line_temp (_key , _delivery_key , _qty_target , _name ,_product_no , _uom  , _desc , _scan ,_qty_accept , _partial_reason , _scanning , _qty_loaded  ) SELECT _key , _delivery_key , _qty_target , _name ,_product_no , _uom  , _desc , _scan ,_qty_accept , _partial_reason , _scanning , _qty_loaded  FROM line");
            database.execSQL("ALTER TABLE line_temp RENAME TO line_new");

            database.execSQL("CREATE TABLE photo_temp ( _key INTEGER, _signature_id INTEGER, _path TEXT, _uploaded INTEGER, PRIMARY KEY (_key) )");
            database.execSQL("INSERT INTO photo_temp ( _signature_id , _path , _uploaded ) SELECT _signature_id , _path , _uploaded  FROM photo");
            database.execSQL("ALTER TABLE photo_temp RENAME TO photo_new");


            database.execSQL("CREATE TABLE plate_temp ( plate_key INTEGER, _line_key INTEGER, _plate TEXT, _scanned INTEGER, PRIMARY KEY (plate_key) )");
            database.execSQL("INSERT INTO plate_temp ( _line_key , _plate , _scanned  ) SELECT  _line_key , _plate , _scanned  FROM plate");
            database.execSQL("ALTER TABLE plate_temp RENAME TO plate_new");

            database.execSQL("CREATE TABLE prompt_temp ( _key INTEGER, type TEXT, code TEXT, style TEXT, message TEXT, PRIMARY KEY (_key) )");
            database.execSQL("INSERT INTO prompt_temp ( type , code , style , message  ) SELECT  type , code , style , message  FROM prompt");
            database.execSQL("ALTER TABLE prompt_temp RENAME TO prompt_new");

            database.execSQL("CREATE TABLE route_temp ( id INTEGER, name TEXT, command TEXT, finished INTEGER, order_uploaded INTEGER, PRIMARY KEY (id) )");
            database.execSQL("INSERT INTO route_temp (id , name , command , finished , order_uploaded  ) SELECT  id , name , command , finished , order_uploaded FROM route");
            database.execSQL("ALTER TABLE route_temp RENAME TO route_new");

            database.execSQL("CREATE TABLE signature_temp ( _id INTEGER, _note TEXT, _signee TEXT, _signed TEXT, _path TEXT, _crumb TEXT,_uploaded INTEGER, _reference TEXT, PRIMARY KEY (_id) )");
            database.execSQL("INSERT INTO signature_temp ( _id , _note , _signee , _signed , _path , _crumb ,_uploaded , _reference  ) SELECT  _id , _note , _signee , _signed , _path , _crumb ,_uploaded , _reference   FROM signature");
            database.execSQL("ALTER TABLE signature_temp RENAME TO signature_new");

            database.execSQL("CREATE TABLE site_temp ( _key INTEGER, _account TEXT, _name TEXT, _address1 TEXT, _address2 TEXT, _address3 TEXT,_city TEXT, _state TEXT,_zip TEXT, _address TEXT, _phone TEXT, PRIMARY KEY (_key) )");
            database.execSQL("INSERT INTO site_temp (_key , _account , _name , _address1 , _address2 , _address3 ,_city , _state ,_zip , _address   ) SELECT _key , _account , _name , _address1 , _address2 , _address3 ,_city , _state ,_zip , _address   FROM site");
            database.execSQL("ALTER TABLE site_temp RENAME TO site_new");

            database.execSQL("CREATE TABLE stop_temp ( _key INTEGER, _site_key INTEGER, _planned INTEGER, _sort INTEGER, _base_delivery_key INTEGER, _signature_key INTEGER, _window_id INTEGER ,PRIMARY KEY (_key) )");
            database.execSQL("INSERT INTO stop_temp (_key , _site_key , _planned , _sort , _base_delivery_key , _signature_key , _window_id  ) SELECT _key , _site_key , _planned , _sort , _base_delivery_key , _signature_key , _window_id   FROM stop");
            database.execSQL("ALTER TABLE stop_temp RENAME TO stop_new");

            database.execSQL("CREATE TABLE windowTime_temp ( _id INTEGER, _window_id INTEGER, _startSec INTEGER, _endSec INTEGER,PRIMARY KEY (_id) )");
            database.execSQL("INSERT INTO windowTime_temp (_id , _window_id , _startSec , _endSec) SELECT _id , _window_id , _startSec , _endSec FROM windowTime");
            database.execSQL("ALTER TABLE windowTime_temp RENAME TO windowTime_new");

            database.execSQL("CREATE TABLE window_temp ( _id INTEGER, _display TEXT, PRIMARY KEY (_id) )");
            database.execSQL("INSERT INTO window_temp (_id , _display) SELECT _id , _display FROM window");
            database.execSQL("ALTER TABLE window_temp RENAME TO window_new");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // creating delivery if needed
            database.execSQL("create table if not exists delivery (_key integer primary key not null, _stop_key integer, _site_key integer, _type text, _delivery_cd text, _delivery_note text, _signing integer)");
            database.execSQL("create table line_temp (_key integer primary key, _delivery_key integer, _qty_target integer, _name text, _product_no text, _uom text, _desc text, _scan text, _qty_accept integer, _partial_reason text, _scanning integer, _qty_loaded integer )");
            database.execSQL("insert into line_temp (_key, _delivery_key, _qty_target, _name, _product_no, _uom, _desc, _scan, _qty_accept, _partial_reason, _scanning, _qty_loaded) select _key , _delivery_key , _qty_target , _name ,_product_no , _uom  , _desc , _scan ,_qty_accept , _partial_reason , _scanning , _qty_loaded  FROM line ");
            database.execSQL("drop table line");
            database.execSQL("ALTER TABLE line_temp RENAME TO line");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // creating crumbs if needed
            database.execSQL("CREATE TABLE IF NOT EXISTS crumb(_id INTEGER PRIMARY KEY NOT NULL, _encodedCSV TEXT NOT NULL, _routeKey INTEGER NOT NULL, _date INTEGER NOT NULL)");
        }
    };


}
