package com.jumptech.tracklib.db;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.networking.JTRepository;
import com.jumptech.tracklib.comms.CommandPrompt;
import com.jumptech.tracklib.comms.TrackAuthException;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Command;
import com.jumptech.tracklib.data.DataService;
import com.jumptech.tracklib.data.Route;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.PhotoRepository;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.repository.SignatureRepository;
import com.jumptech.tracklib.repository.StopRepository;
import com.jumptech.tracklib.repository.UtilRepository;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.ui.BundleTrack;
import com.jumptech.ui.GdrActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Business {
	private static final String TAG = "bus";
	private static final String TAG_CRUMBS = "CRUMBS";
	private static final String CRUMB_UPLOAD_FILENAME = "crumb-upload.csv";

    /**
     * Object used to synchronize event of the sync()
     */
    public static final Object synchronize = new Object();

    /**
     * Object used to synchronize event of the syncCrumb()
     */
    public static final Object syncCrumb = new Object();

    /*
     * context sensitive
     */
    Context _context;

    private Business(Context context) {
        _context = context;
    }

    public static Business from(Context context) {
        return new Business(context);
    }


    /*
     * Data operations
     */
	public void syncQuiet() throws TrackException {
		try {
			sync();
		}
		catch(TrackAuthException trackAuthException ){
			Log.e(TAG, "sync failed", trackAuthException);
			Log.e(TAG_CRUMBS, "sync failed", trackAuthException);
			throw new TrackAuthException();
		} catch (TrackException trackException){
            Log.e(TAG_CRUMBS, "sync failed",trackException);
		    throw new TrackException();
        } catch (Exception e){
            Log.e(TAG_CRUMBS, "another exception",e);
            throw e;
		}
	}


    /**
     * @return null on success, reason id on failure
     */
    public Integer sync() throws TrackException {
        return sync(false);
    }

    synchronized private Integer sync(boolean signatureOnly) throws TrackException {
        synchronized (Business.synchronize) {
            Log.i(TAG, "sync");
            //TODO assert can sync
            //TODO freeze signature start
            if (!syncSignature()) return R.string.promptSyncSignature;
            if (!syncPhoto()) return R.string.promptSyncPhoto;
            if (signatureOnly) return null;
            if (!syncStopOrder()) return R.string.promptSyncStopOrder;
            if (!syncCrumb()) return R.string.promptSyncGPS;
            return null;
        }
    }

    private boolean syncSignature() throws TrackException {
        Cursor cursor = TrackDB.getInstance(_context).getStopDao().getNonSyncedStops();
        while (cursor.moveToNext()) {
            Log.i(TAG, "syncing signature");
            Stop stop = StopRepository.stop(cursor);
            CommandPrompt cmdPrompt = JTRepository.signature(stop, _context);
            RouteRepository.storeRoute(_context, cmdPrompt);
            SignatureRepository.signatureUploaded(_context, stop.signature_key);
        }
        return true;
    }

    private boolean syncPhoto() throws TrackException {
        Map<String, List<String>> ref_paths = PhotoRepository.getPhotosUnsynced(_context);

        for (Map.Entry<String, List<String>> entry : ref_paths.entrySet()) {
            String last = entry.getValue().get(entry.getValue().size() - 1);
            for (String path : entry.getValue()) {
                Log.i(TAG, "Sync photo - key: " + entry.getKey());
                JTRepository.photo(entry.getKey(), path, path != last, _context); //intentional string address compare
                PhotoRepository.updatePhoto(_context, path, true);
            }
        }
        return true;
    }

    private boolean syncStopOrder() throws TrackException {
        //if we don't need to sync

        if (RouteRepository.fetchRoute(_context)._orderUploaded)
            return true;

        Log.i(TAG, "syncing stop order");

        List<Long> stopKeys = new ArrayList<>();
        Cursor cursor = StopRepository.inTransitStops(_context);
        while (cursor.moveToNext()) {
            stopKeys.add(cursor.getLong(0));
        }

        JTRepository.stopOrder(stopKeys, _context);

        RouteRepository.updateRouteOrderUpload(_context, true);

        return true;
    }

    private boolean syncCrumb() throws TrackException {
        Log.i(TAG, "syncing crumbs");
        try {
            Route route = RouteRepository.fetchRoute(_context);
            Log.i(TAG, "attempting upload");
            JTRepository.crumbs(route._key, route._finished, _context);
            return true;
        } catch (Exception e) {
            throw new TrackException(e);
        }
    }

    public static boolean hasGPSData(Context cntx) {
        synchronized (Business.syncCrumb) {
            File upload = new File(cntx.getFilesDir(), DataService.CRUMB_FILENAME);
            return upload.exists();
        }
    }

    public void assertService(boolean finished) {
        Intent intent = new Intent(_context, DataService.class);

        if (!finished) {
            TrackPreferences pref = new TrackPreferences(_context);
            intent.putExtra(BundleTrack.GPS_ENABLED, pref.getGpsEnabled());
            // intent.putExtra(BundleTrack.GPS_MIN_DISTANCE, pref.);
            // intent.putExtra(BundleTrack.GPS_MIN_TIME, pref.);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                _context.startForegroundService(intent);
            } else {
                _context.startService(intent);
            }
        } else _context.stopService(intent);
    }

    public void routeExit() throws TrackException {
        routeFinish();
        UtilRepository.clearAllExceptRoute(_context);
        for (File file : routeFiles()) file.delete();
        for (File file : _context.getExternalFilesDir(null).listFiles()) file.delete();
    }

    public File[] routeFiles() {
        return _context.getFilesDir().listFiles(new FileFilter() {
                                                    @Override
                                                    public boolean accept(File file) {
                                                        return !"gaClientId".equals(file.getName());
                                                    }
                                                }
        );
    }

    // finish route and stop service and delete files
    public void routeFinish() {
        if (!RouteRepository.fetchRoute(_context)._finished) {
            Log.i("CRUMBS","Finishing a route");
            assertService(true);
            RouteRepository.clearPending(_context);
            RouteRepository.clearCompleted(_context);
            RouteRepository.updateFinishedRoutes(_context);
            Util.deleteFilesInDir(_context.getFilesDir().getAbsolutePath());
        }
    }

    public void signatureStop(long stopKey) {
        signatureStart();
        DeliveryRepository.signatureStop(_context, stopKey);
    }

    public void signatureDelivery(Collection<Long> deliveryKeys) {
        signatureStart();
        for (Long deliveryKey : deliveryKeys)
            DeliveryRepository.signatureDelivery(_context, deliveryKey);
    }

    public void signatureDelivery(long deliveryKey) {
        signatureStart();
        DeliveryRepository.signatureDelivery(_context, deliveryKey);
    }

    private void signatureStart() {
        DataService.startGpsInterval();
        DeliveryRepository.signatureDeliveryClear(_context);
    }

    public void signatureFinalize(String signee) throws TrackException {
        SignatureRepository.signatureFinalize(_context, signee, DataService.getGpsInterval(), new Date());
    }

    public void execute(Command command) {
        if (command == null) return;

        try {

            switch (command) {
                case ROUTE_UPDATE:
                    RouteRepository.clearPending(_context);
                    if (sync(true) == null) {
                        JTRepository.routeSynchronous(RouteRepository.fetchRoute(_context)._key, _context);
                    }
                    break;
                case ROUTES_GOTO:
                    routeFinish();
                    if (sync() == null) {
                        _context.startActivity(new Intent(_context, GdrActivity.class));
                    }
                    break;
            }
            RouteRepository.updateCommand(_context, null);
        } catch (TrackException e) {
            Log.e(TAG, "unable to process command", e);
        }

    }


}
