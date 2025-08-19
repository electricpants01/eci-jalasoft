package com.jumptech.tracklib.data;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.jumptech.android.util.NotificationHelper;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.UtilTrack;
import com.jumptech.tracklib.comms.TrackAuthException;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.db.Business;
import com.jumptech.tracklib.repository.CrumbRepository;
import com.jumptech.tracklib.room.entity.Crumb;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.ui.BundleTrack;
import com.jumptech.ui.LauncherActivity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataService extends Service {

    private ScheduledExecutorService _syncService;
    private int[] fib ={1,2,3,5,8,13,21,34,55};
    private static final String TAG_CRUMBS = "CRUMBS";

    private LocationListener _gpsListener = null;
    private LocationManager _gpsManager = null;

    private static Location _intervalLocation = null;

    private boolean _gpsEnabled = false;
    private long _gpsMinTime_ms = 5000;
    private float _gpsMinDistance_m = 1;

    private static final String TAG = DataService.class.getName();
    public static final String CRUMB_FILENAME = "crumb.csv";
    private static final Object lock = new Object();
    private long _currentFibDelay = 0 ;
    private boolean _hasErrorOnService = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        Log.i(TAG_CRUMBS, "creating service");
        _gpsListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(Location location) {
                try {
                    Log.i(TAG, "" + location);
                    synchronized (lock) {
                        //if this is our best interval
                        if (_intervalLocation == null
                                || !_intervalLocation.hasAccuracy()
                                || (location.hasAccuracy() && location.getAccuracy() < _intervalLocation.getAccuracy())
                        ) _intervalLocation = location;
                    }

                    String data = encodeCSV(location);
                    if ( data == null ) return;
                    else data += "\r\n";
                    /**
                     * Add encodedCSV to the db to sync later
                     */
                    CrumbRepository.INSTANCE.addCrumb(
                            new Crumb(0, data, 0, (new Date()).getTime()),
                            getApplicationContext()
                    );
                } catch (Exception e) {
                    Log.e(DataService.class.getName(), "Unable to log location.", e);
                }
            }
        };
        _syncService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: " + intent);
        final Intent notificationIntent = new Intent(this, LauncherActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationHelper.registerChannel(this);
        Notification notification = NotificationHelper.generateNotification(this,
                pendingIntent,
                getString(R.string.notification_gps_title),
                getString(R.string.notification_gps_title),
                getString(R.string.notification_gps_description),
                R.drawable.icon_white,
                false);


        startForeground(NotificationHelper.DATA_SERVICES_NOTIFICATION_ID,
                notification);
        if (intent != null) {
            _gpsEnabled = intent.getBooleanExtra(BundleTrack.GPS_ENABLED, _gpsEnabled);
            _gpsMinDistance_m = intent.getFloatExtra(BundleTrack.GPS_MIN_DISTANCE, _gpsMinDistance_m);
            _gpsMinTime_ms = intent.getLongExtra(BundleTrack.GPS_MIN_TIME, _gpsMinTime_ms);
        }

        if (_gpsEnabled && PermissionUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (_gpsManager == null)
                _gpsManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (_gpsManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    _gpsManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, _gpsMinTime_ms, _gpsMinDistance_m, _gpsListener);
                }
            if (_gpsManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    _gpsManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, _gpsMinTime_ms, _gpsMinDistance_m, _gpsListener);
                }
        } else if (_gpsManager != null) {
            _gpsManager.removeUpdates(_gpsListener);
        }
        long currentDelay = _currentFibDelay;
        Log.i(TAG_CRUMBS, "Current Delay: " + currentDelay);
        if (currentDelay == 0) {
            Log.i(TAG_CRUMBS, "RESTARTING thread");
            _syncService.execute(new Runnable() {
                private Business _business = Business.from(DataService.this);
                private final long _finalRetryTime = 60; // default on error
                private final long _syncRateMinutes = 5; // default on no error
                private long _currentSyncTime = 5; // variable depending on fibonacci serie
                private int _index = -1;
                private boolean _hasErrorOnThread = false;

                @Override
                public void run() {
                    long currentFibonacciDelay = _currentFibDelay;
                    if (!_hasErrorOnService || (_index >= 0)) {
                        try {
                            _hasErrorOnThread = false;
                            _business.syncQuiet();
                        } catch (Exception e) {
                            _hasErrorOnThread = true;
                            _hasErrorOnService = true;
                            _index += 1;
                            if(!(e instanceof TrackException || e instanceof  TrackAuthException)){
                                Log.i(TAG_CRUMBS, "Unknown exception");
                                return;
                            }
                            if (_index >= fib.length) {
                                Log.i(TAG_CRUMBS, "Final time reached");
                                _syncService.schedule(this, _finalRetryTime, TimeUnit.MINUTES);
                            } else {
                                Log.i(TAG_CRUMBS, _index + " " + fib[(int) _index] + " " +e.getMessage());
                                _currentSyncTime = fib[(int) _index];
                                _currentFibDelay = _currentSyncTime;
                                _syncService.schedule(this, _currentSyncTime, TimeUnit.MINUTES);
                            }
                        } finally {
                            if (!_hasErrorOnThread) {
                                Log.i(TAG_CRUMBS, "Running no error");
                                _currentSyncTime = _syncRateMinutes;
                                _index = -1;
                                _hasErrorOnService = false;
                                _currentFibDelay = _currentSyncTime;
                                _syncService.schedule(this, _syncRateMinutes, TimeUnit.MINUTES);
                            }
                        }

                    }
                }
            });
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        gpsStop();
        Log.i(TAG_CRUMBS, "shutdown");
        _syncService.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void gpsStop() {
        Log.i(TAG, "stopGps");
        if (_gpsManager != null && _gpsListener != null) {
            _gpsManager.removeUpdates(_gpsListener);
        }
    }

    public static void startGpsInterval() {
        synchronized (lock) {
            _intervalLocation = null;
        }
    }

    public static String getGpsInterval() {
        synchronized (lock) {
            return _intervalLocation == null ? null : encodeCSV(_intervalLocation);
        }
    }

    public static String encodeCSV(Location location) {
        if(location != null && location.getProvider() != null){
            List<String> data = new ArrayList<>();
            data.add("" + UtilTrack.formatServer(new Date(location.getTime())));
            data.add("" + location.getLongitude());
            data.add("" + location.getLatitude());
            data.add("" + location.getAltitude());
            data.add("" + location.getSpeed());
            data.add("" + location.getBearing());
            data.add("" + location.getAccuracy());
            data.add(location.getProvider());
            data.add("" + UtilTrack.formatServer(new Date()));

            StringBuilder sbRow = new StringBuilder();
            for (String d : data) {
                if (sbRow.length() > 0) {
                    sbRow.append(",");
                }
                sbRow.append(encodeCSV(d));
            }
            return sbRow.toString();
        }
        return null;
    }

    private static String encodeCSV(String s) {
        if (s == null || !s.matches(".*[\",].*")) return s;
        return "\"" + s.replaceAll("\"", "\"\"") + "\"";
    }

}