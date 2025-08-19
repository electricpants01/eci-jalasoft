package com.jumptech.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jumptech.android.util.Message;
import com.jumptech.android.util.NotificationHelper;
import com.jumptech.android.util.PermissionHelper;
import com.jumptech.android.util.Preferences;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.Broadcast;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.tracklib.comms.TrackAuthException;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Command;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.DeliveryType;
import com.jumptech.tracklib.data.Prompt;
import com.jumptech.tracklib.data.Prompt.Type;
import com.jumptech.tracklib.data.Route;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.db.Business;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.PromptRepository;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.repository.StopRepository;
import com.jumptech.tracklib.utils.CaptureAPI;
import com.jumptech.tracklib.utils.IntentHelper;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.tracklib.utils.scan.ScanLauncher;
import com.jumptech.ui.customDialogs.LocationDialog;
import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.android.Capture;
import com.socketmobile.capture.client.ConnectionState;

import java.lang.ref.WeakReference;

public class RouteActivity extends BaseActivity implements CaptureAPI, LocationDialog.LocationDialogListener {

    private static final String TAG = RouteActivity.class.getSimpleName();
    private static final String TAG_CRUMBS = "CRUMBS";
    /* Stores the last Tab index chosen by the user */
    public static final String LAST_TAB = "LAST_TAB";

    private ListView _listView = null;

    /*
     * Stores the Optimization data
     */
    private Prompt status;

    private static final int REQUEST_STOP_SORT = 1000;
    private TabHost tabHost;
    private Menu screenMenu;
    private TrackPreferences preferences;

    private AlertDialog permissionDialog;
    private SwipeRefreshLayout swipeRefreshLayoutRoute;
    private TextView loadRouteTextView;
    private View loadingRoutesPanel;
    private TextView loadingRoutesTextView;
    private LocationDialog locationDialog;
    private boolean isPermissionDenied = false;


    public RouteActivity() {
        super(TAG);
    }

    @Override
    public void onAccept() {
        requestPermissions();
    }

    @Override
    public void onReject() {
        Preferences.setIsDialogDenied(this, true);
        if (locationDialog != null && locationDialog.getShowsDialog()) {
            locationDialog.setShowsDialog(false);
            locationDialog.dismiss();
        }
    }

    private enum Tab {
        UNDELIVERED(R.string.inTransitTabLabel, R.drawable.tab_notdelivered) {
            @Override
            Cursor getCursor(Context context) {
                return StopRepository.inTransitStops(context);
            }

            @Override
            public boolean getMessageEnabled() {
                return true;
            }
        },
        FINISHED(R.string.completedTabLabel, R.drawable.tab_delivered) {
            @Override
            Cursor getCursor(Context context) {
                return StopRepository.completedStops(context);
            }
        },
        UNSYNCED(R.string.notSyncedTabLabel, R.drawable.tab_synced) {
            @Override
            Cursor getCursor(Context context) {
                return StopRepository.notSyncedStops(context);
            }
        },
        ;

        int _label;
        int _icon;

        Tab(int label, int icon) {
            _label = label;
            _icon = icon;
        }

        public int getLabel() {
            return _label;
        }

        public int getIcon() {
            return _icon;
        }

        public boolean getMessageEnabled() {
            return false;
        }

        abstract Cursor getCursor(Context context);

    }

    private BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                switch (Broadcast.fromAction(intent.getAction())) {
                    case ROUTE_SYNC:
                        //Dismiss 'change route notification' if exists
                        NotificationHelper.dismissNotificationByID(context, NotificationHelper.CHANGE_ROUTE_NOTIFICATION_ID);

                        routePrompt();
                        break;
                    default:
                }
            } catch (IllegalArgumentException e) {
                //intent not for us
                Log.e(TAG, TrackException.MSG_EXCEPTION, e);
            } catch (NullPointerException e) {
                //intent not for us
                Log.e(TAG, TrackException.MSG_EXCEPTION, e);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_activity);
        preferences = new TrackPreferences(this);
        preferences.setRoutesInProgress();
        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView);
        }

        swipeRefreshLayoutRoute = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayoutRoute);
        loadRouteTextView = findViewById(R.id.loadRouteTextView);
        loadingRoutesPanel = findViewById(R.id.loadingPanel);
        swipeRefreshLayoutRoute.setEnabled(false);
        loadingRoutesTextView = findViewById(R.id.loadingTextView);
        swipeRefreshLayoutRoute.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    swipeRefreshLayoutRoute.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    swipeRefreshLayoutRoute.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                Rect rect = new Rect();
                swipeRefreshLayoutRoute.getDrawingRect(rect);
                swipeRefreshLayoutRoute.setProgressViewOffset(false, 0, rect.centerY() - getResources().getDimensionPixelSize(R.dimen.swipe_padding) - (swipeRefreshLayoutRoute.getProgressCircleDiameter() / 2));
            }
        });

        swipeRefreshLayoutRoute.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                _listView.setVisibility(View.GONE);
                foregroundSync(true);
            }
        });

        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        Route route = RouteRepository.fetchRoute(this);
        setTitle(route._name);

        final TextView textViewOptimizationStatus = (TextView) findViewById(R.id.routeStatus);
        {
            status = PromptRepository.prompt(this, Type.OPTIMIZATION);
            if (status == null) textViewOptimizationStatus.setText("");
            else {
                textViewOptimizationStatus.setTextColor(ContextCompat.getColor(this, status.getStyle().getColor()));
                textViewOptimizationStatus.setText(status.getMessage());
            }
        }

        ActivityUtil.showEnvironment(this);

        _listView = (ListView) findViewById(android.R.id.list);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent toRouteIntent = new Intent(RouteActivity.this, StopActivity.class);
                toRouteIntent.putExtra(BundleTrack.KEY_STOP, id);
                toRouteIntent.putExtra(BundleTrack.BUNDLE_INTENT_FROM_ROUTE, true);
                startActivity(toRouteIntent);

            }
        });

        getTabHost().setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                Tab tab = Tab.valueOf(tabId);
                textViewOptimizationStatus.setVisibility((textViewOptimizationStatus.getText() != "" && tab.getMessageEnabled()) ? TextView.VISIBLE : TextView.GONE);
                _listView.setAdapter(new StopListAdapter(RouteActivity.this, tab.getCursor(RouteActivity.this), status));
                TextView txtNotifyStatus = (TextView) findViewById(R.id.notifyStatus);
                //TODO: Improve the status using mechanism to validate GPS data in real time
                txtNotifyStatus.setVisibility(tab.equals(Tab.UNSYNCED) && Business.hasGPSData(RouteActivity.this) ? View.VISIBLE : View.GONE);
                enableOrDisableMenuOptions();
                startSocketMobile();
            }
        });

        for (Tab tab : Tab.values()) {
            getTabHost().addTab(getTabHost()
                    .newTabSpec(tab.name())
                    .setIndicator(getString(tab.getLabel()), getResources().getDrawable(tab.getIcon()))
                    .setContent(android.R.id.tabcontent));
        }

        Business.from(this).assertService(route._finished);
        if (savedInstanceState != null) {
            getTabHost().setCurrentTab(savedInstanceState.getInt(LAST_TAB));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAST_TAB, getTabHost().getCurrentTab());
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R ){
            requestAndroidSPermissions();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestAndroidRAndAbovePermissions();
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                requestAndroidQPermissions();
            } else {
                requestAndroidPAndBelow();
            }
        }
    }

    private void requestAndroidPAndBelow() {
        if (PermissionHelper.INSTANCE.verifyTrackPermissions(this)) {
            dismissLocationDialog();
        } else {
            if (isPermissionDenied) {
                goToSettings();
            } else {
                requestForegroundLocationPermissions();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestAndroidQPermissions() {
        if (PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(this)) {
            dismissLocationDialog();
        } else {
            if (isPermissionDenied || !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                goToSettings();
            } else {
                requestForegroundAndBackgroundLocationPermissions();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestAndroidRAndAbovePermissions() {
        if (PermissionHelper.INSTANCE.verifyTrackPermissions(this)) {
            if (PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(this)) {
                dismissLocationDialog();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    requestBackgroundLocationPermission();
                } else {
                    goToSettings();
                }
            }
        } else {
            if (isPermissionDenied) {
                goToSettings();
            } else {
                requestForegroundLocationPermissions();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestAndroidSPermissions() {
        if (PermissionHelper.INSTANCE.verifyTrackPermissions(this)) {
            if (PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(this)) {
                dismissLocationDialog();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    requestBackgroundLocationPermission();
                } else {
                    goToSettings();
                }
            }
        } else {
            if (isPermissionDenied) {
                goToSettings();
            } else {
                requestFineAndCoraseLocationPermissions();
            }
        }
    }


    private void requestForegroundAndBackgroundLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PermissionUtil.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION);
    }

    private void requestForegroundLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PermissionUtil.REQUEST_TRACK_PERMISSION);
    }

    private void requestFineAndCoraseLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionUtil.REQUEST_FINE_AND_COARSE_LOCATION);
    }

    private void dismissLocationDialog() {
        if (locationDialog != null && locationDialog.getShowsDialog()) {
            locationDialog.dismiss();
        }
    }

    private void goToSettings() {
        IntentHelper.goToSettings(RouteActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.REQUEST_CAMERA_FOR_SCANNER: {
                Integer msgResult = PermissionUtil.checkForCameraPermission(grantResults);
                if (msgResult == null) {
                    startScanning();
                } else {
                    PermissionUtil.displayGrantPermissionMessage(RouteActivity.this, msgResult, R.string.camera_access_required, R.string.open_settings, R.string.cancel);
                }
                return;
            }
            case PermissionUtil.REQUEST_TRACK_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocationPermission();
                    } else {
                        dismissLocationDialog();
                    }
                } else {
                    isPermissionDenied = true;
                }
                return;
            }
            case PermissionUtil.REQUEST_TRACK_BACKGROUND_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dismissLocationDialog();
                } else {
                    isPermissionDenied = true;
                }
                return;
            }
            case PermissionUtil.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dismissLocationDialog();
                } else {
                    isPermissionDenied = true;
                }
                return;
            }
            case PermissionUtil.REQUEST_FINE_AND_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocationPermission();
                    } else {
                        dismissLocationDialog();
                    }
                }else {
                    isPermissionDenied = true;
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PermissionUtil.REQUEST_TRACK_BACKGROUND_PERMISSION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(_receiver, new IntentFilter(Broadcast.ROUTE_SYNC.getAction()));

        refreshStopList();
        startSocketMobile();

        Log.i(TAG_CRUMBS, "routeactivity onResume");
        if ( Util.isConnected(this) ) {
            new SyncTask(this).execute();
        }
        enableOrDisableMenuOptions();

        //Dismiss 'change route notification' if exists
        NotificationHelper.dismissNotificationByID(this, NotificationHelper.CHANGE_ROUTE_NOTIFICATION_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(this)) {
                showLocationPermissionSettingsAlert(true);
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            if (!PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(this)) {
                showLocationPermissionSettingsAlert(true);
            }
        } else {
            if (!PermissionHelper.INSTANCE.verifyTrackPermissions(this)) {
                showLocationPermissionSettingsAlert(false);
            }
        }
    }

    private void showLocationPermissionSettingsAlert(boolean isAndroidQAndAbove) {
        if (locationDialog != null) {
            if (!locationDialog.getShowsDialog()) {
                locationDialog.show(getSupportFragmentManager(), "LocationDialogOnRouteActivity");
            }
        } else {
            locationDialog = new LocationDialog();
            Bundle bundle = new Bundle();
            bundle.putBoolean(LocationDialog.IS_ANDROID_Q_AND_ABOVE, isAndroidQAndAbove);
            locationDialog.setArguments(bundle);
            locationDialog.setCancelable(false);
            locationDialog.show(getSupportFragmentManager(), "LocationDialogOnRouteActivity");
        }
    }

    private void startSocketMobile() {
        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && getTabHost().getCurrentTab() == 0) {
            setCaptureAPIListener(this);
        }
    }

    private void refreshStopList() {
        _listView.setAdapter(new StopListAdapter(RouteActivity.this, Tab.values()[getTabHost().getCurrentTab()].getCursor(this), status));
    }

    @Override
    protected void onPause() {
        setCaptureAPIListener(null);
        unregisterReceiver(_receiver);
        Cursor cursor = ((StopListAdapter) _listView.getAdapter()).getCursor();
        if (cursor != null) {
            cursor.close();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        //if this route has pending stops
        if (RouteRepository.routeHasStop(this, false)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.leaveRouteQuestion)
                    .setCancelable(false)
                    .setMessage(R.string.stopDeliveriesWithInvoices)
                    .setPositiveButton(R.string.leaveRoute, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!hasUnSyncData()) {
                                Business.from(RouteActivity.this).routeFinish();
                                refreshStopList();
                                startActivity(new Intent(RouteActivity.this, GdrActivity.class));
                                finish();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show();
        } else {
            Business.from(RouteActivity.this).routeFinish();
            new ExitSyncTask(this).execute();
        }
    }

    private boolean hasUnSyncData() {
        Cursor cursor = StopRepository.notSyncedStops(this);
        if ((cursor != null) && (cursor.getCount() > 0)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.promptSyncFailed))
                    .setPositiveButton(R.string.try_again, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG_CRUMBS, "hasUnSyncData on click()");
                            new SyncTask(RouteActivity.this).execute();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.screenMenu = menu;
        enableOrDisableMenuOptions();
        return super.onPrepareOptionsMenu(this.screenMenu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.deliverystoplistmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.invoiceListScanMenuItem) {
            startScanning();
            return true;
        } else if (item.getItemId() == R.id.invoiceListEditMenuItem) {
            startActivityForResult(new Intent(this, StopSortActivity.class), REQUEST_STOP_SORT);
            return true;
        } else if (item.getItemId() == R.id.invoiceListSyncNowMenuItem) {
            foregroundSync(false);
            return true;
        } else if (item.getItemId() == R.id.unscheduledDeliveryMenuItem) {
            performUnscheduled(DeliveryType.DROPOFF);
            return true;
        } else if (item.getItemId() == R.id.unscheduledPickupMenuItem) {
            performUnscheduled(DeliveryType.PICKUP);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        Menu menu = navView.getMenu();
        menu.findItem(R.id.LogoutMenuItem).setVisible(false);
        menu.findItem(R.id.selectDefaultMap).setVisible(false);

        menu.findItem(R.id.ExitRouteMenuItem).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onBackPressed();
                return false;
            }
        });
    }

    public TabHost getTabHost() {
        return tabHost;
    }

    private void startScanning() {
        if (PermissionUtil.hasPermission(this, Manifest.permission.CAMERA)) {
            switch (preferences.getScanner()) {
                case CAMERA:
                    ScanLauncher.LaunchScanner(this, IntentIntegrator.ONE_D_CODE_TYPES);
                    break;
                default:
            }
        } else {
            PermissionUtil.requestPermission(this, Manifest.permission.CAMERA, PermissionUtil.REQUEST_CAMERA_FOR_SCANNER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "requestCode: " + requestCode);
        Log.i(TAG, "resultCode: " + resultCode);

        switch (requestCode) {
            case REQUEST_STOP_SORT:
                if (RESULT_OK == resultCode) {
                    refreshStopList();
                }
                break;
            case IntentIntegrator.REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    handleScan(result.getContents());
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "not scan");
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private static class StopListAdapter extends CursorAdapter {

        /*
         * Stores the Optimization data
         */
        Prompt status;
        Context context;

        /*
         * Default constructor
         *
         * @param context an instance of the App context
         * @param cursor the current cursor to read the DB
         * @param status the Optimization data
         */
        public StopListAdapter(Context context, Cursor cursor, Prompt status) {
            super(context, cursor);
            this.status = status;
            this.context = context;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Stop stop = StopRepository.stop(cursor);
            boolean ordered = isNormal(stop);

            updateStopStatusVisibility(view, ordered);

            TextView companyNameText = view.findViewById(R.id.companyNameText);
            companyNameText.setText(stop.name);

            TextView address = view.findViewById(R.id.address);
            address.setText(stop.address);

            for (TextView textView : new TextView[]{companyNameText, address}) {
                Util.changeTextViewColorDependingOnOrder(textView, ordered);
            }

            ((TextView) view.findViewById(R.id.countText)).setText(String.valueOf(stop.delivery_count));

            TextView windowTextView = view.findViewById(R.id.windowTextView);
            ImageView errorIconImageView = view.findViewById(R.id.errorIconImageView);
            if (stop.getWindow_id() != null) {
                stop = StopRepository.obtainWindowTime(context, stop);
                StopActivity.evaluateWindowTimeList(this.context, stop.getWindowTimeList(),
                        stop.getWindowDisplay(),
                        windowTextView,
                        errorIconImageView);
            } else {
                windowTextView.setVisibility(View.GONE);
                errorIconImageView.setVisibility(View.GONE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.stop_item, null);
        }

        /**
         * Verifies if the Stop is valid to display as normal item list
         *
         * @param stop the current Stop data
         * @return a boolean value which defines valid Stop
         */
        private boolean isNormal(Stop stop) {
            return (status == null) || (status._style.compareTo(Prompt.Style.ERROR) != 0) || stop.isOrdered();
        }

        /**
         * Updates the Stop status visibility
         *
         * @param view    a View UI component
         * @param ordered a boolean value to define the text color
         */
        private void updateStopStatusVisibility(View view, boolean ordered) {
            view.findViewById(R.id.stopStatus).setVisibility(ordered ? ImageView.GONE : ImageView.VISIBLE);
        }
    }

    private void routePrompt() {
        ActivityUtil.checkLoginSession(RouteActivity.this);
        final Prompt prompt = PromptRepository.prompt(this, Type.PROMPT);
        if (prompt != null) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(prompt.getMessage())
                    .setMessage(R.string.routeUpdateOnServerMessageContent)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fetchRouteDueNotification(prompt);
                        }
                    })
                    .create()
                    .show();
        }
    }

    private void fetchRouteDueNotification(Prompt prompt) {
        final Route route = RouteRepository.fetchRoute(RouteActivity.this);
        if (route._command != null) {
            loadingRoutesPanel.setVisibility(View.VISIBLE);
            loadingRoutesTextView.setVisibility(View.VISIBLE);
            loadingRoutesTextView.setText(R.string.fetchRouteNoWait);
            new UpdateRouteTask(this).execute(route._command);
        }
        PromptRepository.deleteFromId(RouteActivity.this, prompt._key);
    }

    private void foregroundSync(Boolean isSwipe) {
        if (!isSwipe) {
            loadingRoutesPanel.setVisibility(View.VISIBLE);
            loadingRoutesTextView.setText(R.string.submitServer);
        }
        loadRouteTextView.setText(R.string.submitServer);
        Log.i(TAG_CRUMBS, "foregroundSync");
        new SyncTask(this).execute();
    }

    /**
     * Performs the unscheduled delivery/pickup options
     *
     * @param type type of delivery
     */
    private void performUnscheduled(DeliveryType type) {
        Long lastDeliveryKey = DeliveryRepository.lastScheduledDelivery(RouteActivity.this);
        startActivity(new Intent(RouteActivity.this, DeliveryActivity.class).
                putExtras(DeliveryRepository.addDelivery(RouteActivity.this, lastDeliveryKey, type, true)));
    }

    /**
     * Displays unscheduled delivery/pickup options in screen menu and enable/disable Reorder Stops
     */
    private void enableOrDisableMenuOptions() {
        Log.i(TAG, "enableOrDisableMenuOptions");
        Long lastDeliveryKey = DeliveryRepository.lastScheduledDelivery(RouteActivity.this);
        if (this.screenMenu == null) {
            return;
        }
        MenuItem menuItem = this.screenMenu.findItem(R.id.unscheduledDeliveryMenuItem);
        menuItem.setVisible(preferences.getUnscheduledDropoffEnabled() && lastDeliveryKey != null);
        menuItem = this.screenMenu.findItem(R.id.unscheduledPickupMenuItem);
        menuItem.setVisible(preferences.getUnscheduledPickupEnabled() && lastDeliveryKey != null);
        menuItem = this.screenMenu.findItem(R.id.invoiceListEditMenuItem);
        menuItem.setVisible(preferences.getStopSortEnabled() && getTabHost().getCurrentTab() == 0);

        menuItem = this.screenMenu.findItem(R.id.invoiceListScanMenuItem);
        menuItem.setVisible(preferences.getScanner().isManualTrigger() && getTabHost().getCurrentTab() == 0);
        menuItem = this.screenMenu.findItem(R.id.bluetoothScannerMenuItem);
        menuItem.setVisible(preferences.getScanner().isBluetooth() && getTabHost().getCurrentTab() == 0);
        menuItem.setIcon(!Capture.get().getDevices().isEmpty() ? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_red);
    }


    @Override
    protected void scannerSelected() {
        enableOrDisableMenuOptions();
        startSocketMobile();
    }

    public boolean handleScan(String scan) {
        Log.i(TAG, "scanned: " + scan);

        Delivery delivery = DeliveryRepository.fetchDeliveryScan(this, scan);
        if (delivery != null) {
            startActivity(new Intent(this, StopActivity.class).putExtra(BundleTrack.KEY_STOP, delivery.stop_key).putExtra(BundleTrack.KEY_DELIVERY, delivery.id));
        } else {
            Message.show(R.string.scanDeliveryNotFound, this);
        }
        return true;
    }

    @Override
    public void onCaptureAPIData(String data) {
        handleScan(data);
    }

    @Override
    public void onCaptureAPIError(CaptureError captureError, ConnectionState connectionState) {
        handleSocketScannerError(captureError, connectionState);
    }

    @Override
    public void onDeviceReady() {
        enableOrDisableMenuOptions();
    }

    private void dismissProgressDialog() {
        loadingRoutesPanel.setVisibility(View.GONE);
        loadRouteTextView.setVisibility(View.GONE);
    }

    private static class SyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<RouteActivity> activityReference;

        public SyncTask(RouteActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Integer message = null;
            try {
                RouteActivity activity = activityReference.get();
                if (activity == null || activity.isFinishing())
                    return R.string.promptSyncFailed;
                message = Business.from(activity).sync();
                Log.i(TAG_CRUMBS, "do in background SyncTask");
            } catch (TrackException e) {
                Log.e(TAG, "sync failure", e);
                message = R.string.promptSyncFailed;
            }
            return message;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            final RouteActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;
            activity._listView.setVisibility(View.VISIBLE);
            activity.dismissProgressDialog();
            activity.swipeRefreshLayoutRoute.setRefreshing(false);
            activity.refreshStopList();
            if (result != null) {
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setMessage(result)
                        .setPositiveButton(R.string.try_again, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.foregroundSync(false);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show();
            }
            activity.routePrompt();
        }
    }

    private static class UpdateRouteTask extends AsyncTask<Command, Void, Integer> {

        private WeakReference<RouteActivity> activityReference;

        public UpdateRouteTask(RouteActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Integer doInBackground(Command... params) {
            try {
                RouteActivity activity = activityReference.get();
                if (activity == null || activity.isFinishing())
                    return R.string.deliveryStopListFromServerError;
                Log.i(TAG_CRUMBS, "UpdateRouteTask");
                Business.from(activity).execute(params[0]);
                return null;
            } catch (Exception e) {
                Log.e(TAG, "auth failure", e);
                return R.string.deliveryStopListFromServerError;
            }
        }

        @Override
        protected void onPostExecute(Integer error) {
            super.onPostExecute(error);
            RouteActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;
            activity.dismissProgressDialog();
            if (error == null) {
                Log.e(TAG, "error: " + error);
                activity.refreshStopList();
            } else {
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setMessage(error)
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
            }
        }
    }

    public static class ExitSyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<RouteActivity> activityReference;

        public ExitSyncTask(RouteActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            //make sure there isn't a reason for us to leave
            Integer message = null;
            try {
                RouteActivity activity = activityReference.get();
                if (activity == null || activity.isFinishing())
                    return null;
                Log.i(TAG_CRUMBS, "doinbackground ExitSyncTask");
                message = Business.from(activity).sync();
            } catch (TrackAuthException e) {
                Log.e(TAG, "TrackAuthException", e);
            } catch (TrackException e) {
                Log.e(TAG, "sync failure", e);
                message = R.string.promptSyncFailed;
            }
            return message;
        }

        @Override
        protected void onPostExecute(Integer message) {
            super.onPostExecute(message);
            final RouteActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;
            if (message != null) {
                new AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.onBackPressed();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .setMessage(message)
                        .create()
                        .show();
            } else {
                try {
                    Business.from(activity).routeExit();
                    activity.startActivity(new Intent(activity, GdrActivity.class));
                    activity.finish();
                } catch (Exception e) {
                    Log.e(TAG, TrackException.MSG_EXCEPTION, e);
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setPositiveButton(R.string.try_again, null)
                            .setMessage(R.string.promptSyncFailed)
                            .create()
                            .show();
                }
            }
        }
    }
}
