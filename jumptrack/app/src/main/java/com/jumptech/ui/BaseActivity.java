package com.jumptech.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.repository.UtilRepository;
import com.jumptech.tracklib.utils.CaptureAPI;
import com.jumptech.tracklib.utils.factories.AlertDialogFactory;
import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.android.Capture;
import com.socketmobile.capture.android.events.ConnectionStateEvent;
import com.socketmobile.capture.client.CaptureClient;
import com.socketmobile.capture.client.ConnectionState;
import com.socketmobile.capture.client.DataEvent;
import com.socketmobile.capture.client.DeviceClient;
import com.socketmobile.capture.client.DeviceState;
import com.socketmobile.capture.client.DeviceStateEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends of @link {SimpleBaseActivity} class and allows setup UI with specific components
 *
 * @link{setOnNavigationItemSelectedListener} method is called and provided with a proper
 * navigation listener @link{onNavigationItemSelectedListener} (to handle Scanner/Support/etc) options,
 * so that call to @link{addNavigationFragment} method is setup with common @link{menu_container_view}
 * menu for all common screens.
 */
public abstract class BaseActivity extends SimpleBaseActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener;
    private CaptureAPI captureAPI;
    private AlertDialog alertDialog;
    private TrackPreferences preferences;
    private boolean scannerReady;

    protected BaseActivity(String tag) {
        super(tag);
        onNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                drawerLayout.closeDrawers();
                if (menuItem.getItemId() == R.id.ScannerTypeMenuItem) {
                    setupScannerType();
                } else if (menuItem.getItemId() == R.id.LogoutMenuItem) {
                    new AlertDialog.Builder(BaseActivity.this)
                            .setCancelable(false)
                            .setMessage(R.string.logoutConfirm)
                            .setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        UtilRepository.clearAllExceptRoute(BaseActivity.this);
                                        RouteRepository.nukeTable(BaseActivity.this);
                                        new TrackPreferences(BaseActivity.this).setAuth(null, null);
                                        new TrackPreferences(BaseActivity.this).removeSignatureInProgress();
                                        startActivity(new Intent(BaseActivity.this, LoginActivity.class));
                                        finish();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show();
                } else if (menuItem.getItemId() == R.id.AboutAndSupportMenuItem) {
                    startActivity(new Intent(BaseActivity.this, SupportActivity.class));
                } else if (menuItem.getItemId() == R.id.selectDefaultMap){
                    AlertDialogFactory.selectDefaultMapApp(BaseActivity.this, Util.isPackageInstalled("com.waze", getPackageManager())).show();
                }
                return false;
            }
        };
        setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = new TrackPreferences(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(R.string.install_socket_mobile_app_message)
                .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Capture.installCompanion(BaseActivity.this);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        preferences.setScanner(Scanner.CAMERA);
                        scannerSelected();
                    }
                });
        alertDialog = builder.create();
    }

    /**
     * Adds a Navigation Fragment to the activity
     *
     * @param toolbarId      toolbar's resource ID
     * @param drawerLayoutId drawerLayout's resource ID
     * @param containerId    container's resource ID
     */
    protected void addNavigationFragment(int toolbarId, int drawerLayoutId, int containerId) {
        super.addNavigationFragment(toolbarId, drawerLayoutId, containerId, R.menu.menu_container_view);
    }

    private void setupScannerType() {
        Scanner current = preferences.getScanner();
        List<String> userScanner = new ArrayList<>();
        final List<Scanner> scanners = new ArrayList<>();
        for (Scanner scanner : com.jumptech.jumppod.model.Scanner.values()) {
            if (!scanner.enabled()) continue;
            userScanner.add(getString(scanner.getUserStr()));
            scanners.add(scanner);
        }

        new AlertDialog.Builder(BaseActivity.this)
                .setTitle(getString(R.string.setScanner))
                .setSingleChoiceItems(userScanner.toArray(new String[userScanner.size()]), scanners.indexOf(current), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Scanner scanner = scanners.get(which);
                        preferences.setScanner(scanner);
                        if (scanner != Scanner.SOCKET_MOBILE) {
                            captureAPI = null;
                        }
                        scannerSelected();
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    protected void scannerSelected() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onData(DataEvent event) {
        DeviceClient device = event.getDevice();
        String data = event.getData().getString();
        Log.d(TAG, "onData: " + data);
        if (captureAPI != null && preferences.getScanner() == Scanner.SOCKET_MOBILE)
            captureAPI.onCaptureAPIData(data);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onCaptureDeviceStateChange(DeviceStateEvent event) {
        DeviceClient device = event.getDevice();
        DeviceState state = event.getState();

        switch (state.intValue()) {
            case DeviceState.READY:
                Log.d(TAG, "Device: READY");
                scannerReady = true;
                if (captureAPI != null && preferences.getScanner() == Scanner.SOCKET_MOBILE) {
                    captureAPI.onDeviceReady();
                    Toast.makeText(getApplicationContext(), R.string.socket_mobile_ready_use, Toast.LENGTH_SHORT).show();
                }
                break;
            case DeviceState.AVAILABLE:
                Log.d(TAG, "Device: AVAILABLE");
                break;
            case DeviceState.GONE:
                Log.d(TAG, "Device: GONE");
                break;
            case DeviceState.OPEN:
                Log.d(TAG, "Device: OPEN");
                scannerReady = false;
                retryScannerConnection();
                break;
            default:
                // Device not ready for use
        }
    }

    private void retryScannerConnection() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!scannerReady) {
                    Capture.restart(BaseActivity.this);
                }
            }
        }, 3000);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onCaptureServiceConnectionStateChange(ConnectionStateEvent event) {
        ConnectionState state = event.getState();
        CaptureClient client = event.getClient();

        if (state.hasError()) {
            CaptureError error = state.getError();
            if (captureAPI != null && preferences.getScanner() == Scanner.SOCKET_MOBILE)
                captureAPI.onCaptureAPIError(error, state);
            switch (error.getCode()) {
                case CaptureError.BLUETOOTH_NOT_ENABLED:
                    Log.d(TAG, "Error service: BLUETOOTH_NOT_ENABLED");
                    break;
                case CaptureError.COMPANION_NOT_INSTALLED:
                    Log.d(TAG, "Error service: COMPANION_NOT_INSTALLED");
                    break;
                case CaptureError.SERVICE_NOT_RUNNING:
                    Log.d(TAG, "Error service: SERVICE_NOT_RUNNING");
                    break;
                case CaptureError.UNABLE_TO_PARSE_RESPONSE:
                    Log.d(TAG, "Error service: UNABLE_TO_PARSE_RESPONSE");
                    break;
            }
        }

        switch (state.intValue()) {
            case ConnectionState.CONNECTING:
                Log.d(TAG, "Service CONNECTING");
                break;
            case ConnectionState.CONNECTED:
                Log.d(TAG, "Service CONNECTED");
                break;
            case ConnectionState.READY:
                Log.d(TAG, "Service READY");
                break;
            case ConnectionState.DISCONNECTING:
                Log.d(TAG, "Service DISCONNECTING");
                break;
            case ConnectionState.DISCONNECTED:
                Log.d(TAG, "Service DISCONNECTED");
                break;
        }
    }

    public void handleSocketScannerError(CaptureError captureError, ConnectionState connectionState) {
        switch (captureError.getCode()) {
            case CaptureError.BLUETOOTH_NOT_ENABLED:
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                break;
            case CaptureError.COMPANION_NOT_INSTALLED:
                openPlayStore();
                break;
            case CaptureError.SERVICE_NOT_RUNNING:
                if (connectionState.isDisconnected()) {
                    if (Capture.notRestartedRecently()) {
                        Capture.restart(this);
                    } else {
                        openSocketMobileApp();
                    }
                }
                break;
            case CaptureError.UNABLE_TO_PARSE_RESPONSE:

                break;
        }
    }

    public void setCaptureAPIListener(Context context) {
        this.captureAPI = (CaptureAPI) context;
        if (captureAPI != null && !Capture.get().isConnected()) {
            Capture.restart(context);
        }
    }

    public void openSocketMobileApp() {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.socketmobile.companion", PackageManager.GET_ACTIVITIES);
            Toast.makeText(getApplicationContext(), R.string.open_socket_mobile_app_message, Toast.LENGTH_SHORT).show();
        } catch (PackageManager.NameNotFoundException e) {
            openPlayStore();
        }
    }

    public void openPlayStore() {
        if (!alertDialog.isShowing()) {
            alertDialog.show();
        }
    }
}
