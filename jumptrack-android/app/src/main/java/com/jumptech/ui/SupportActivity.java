package com.jumptech.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jumptech.android.util.FileManager;
import com.jumptech.android.util.PermissionHelper;
import com.jumptech.android.util.Preferences;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.BuildConfig;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.utils.IntentHelper;
import com.jumptech.tracklib.utils.PermissionUtil;

import com.jumptech.ui.adapters.PermissionListItem;
import com.jumptech.ui.adapters.PermissionRecyclerViewAdapter;
import com.jumptech.ui.customDialogs.LocationDialog;
import com.socketmobile.capture.android.Capture;
import com.socketmobile.capture.client.DeviceClient;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class SupportActivity extends FragmentActivity implements LocationDialog.LocationDialogListener {

    private static final String TAG = SupportActivity.class.getSimpleName();
    private static final float MIN_SIZE_SP = 12;

    private static final Object lock = new Object();
    private static Boolean _connectNetwork = null;
    private static Boolean _connectServer = null;
    private static Boolean _connectAuthentiated = null;

    private RecyclerView permissionsRecyclerView;
    private List<PermissionListItem> permissionList;
    private PermissionRecyclerViewAdapter permissionsAdapter;
    private PermissionListItem currentPermissionToRequest;
    private LocationDialog locationDialog;
    private boolean isPermissionDenied = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.support_activity);

        final TrackPreferences pref = new TrackPreferences(this);
        if (pref.getLogin() != null) {
            setupPermissionsList((RecyclerView) findViewById(R.id.permissionList));
            synchronized (lock) {
                _connectNetwork = Util.isConnected(this);
            }
            setCheckBoxViewStatus(R.id.networkIndicator, _connectNetwork);

            //send request to server and set server connection and auth indicators
            (new Thread() {
                @Override
                public void run() {
                    //TODO DeliveryNotificationResponse response = (DeliveryNotificationResponse)app.getNewServer().sendRequest(request);
                    synchronized (lock) {
                        _connectServer = true;
                        _connectAuthentiated = true; //TODO _login = response.success() ? "yes" : "no";
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            setCheckBoxViewStatus(R.id.connectIndicator, _connectServer);
                            setCheckBoxViewStatus(R.id.authIndicator, _connectAuthentiated);
                            setTextUsername();
                        }
                    });
                }
            }).start();

            //	 Bluetooth status
            {
                setCheckBoxViewStatus(R.id.bluetoothAvailableIndicator, BluetoothAdapter.getDefaultAdapter() != null);
                Scanner type = pref.getScanner();
                if (type == Scanner.SOCKET_MOBILE) {
                    setCheckBoxViewStatus(R.id.bluetoothWaitingIndicator, Capture.get().isConnected());
                    if (!Capture.get().getDevices().isEmpty()) {
                        Iterator<DeviceClient> iterator = Capture.get().getDevices().iterator();
                        ((TextView) findViewById(R.id.textViewBluetoothDeviceName)).setText(iterator.next().getDeviceGuid());
                    }
                }
            }
            try {
                displayContactUsContent();
            } catch (IOException e) {
                Log.e(TAG, "Can't load contact us", e);
            }
        } else {
            findViewById(R.id.supportSection).setVisibility(View.GONE);
        }

        setupAboutContent(this, findViewById(R.id.aboutInf));


    }

    /*
     * This method initializes the Permission adapter which handle request permission
     *
     * @param viewById a RecyclerView identifier
     */
    private void setupPermissionsList(RecyclerView viewById) {
        permissionList = new ArrayList<>();
        permissionList.add(new PermissionListItem(getApplicationContext(), R.string.support_permission_camera_title, Manifest.permission.CAMERA, R.string.support_should_allow_camera_msg));
        if (new TrackPreferences(this).getGpsEnabled()) {
            permissionList.add(new PermissionListItem(getApplicationContext(), R.string.support_permission_location_title, Manifest.permission.ACCESS_FINE_LOCATION, R.string.support_should_allow_gps_msg));
        }
        permissionList.add(new PermissionListItem(getApplicationContext(), R.string.support_permission_storage_title, Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.support_should_allow_storage_msg));

        permissionsRecyclerView = viewById;
        permissionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        permissionsRecyclerView.setHasFixedSize(true);
        permissionsAdapter = new PermissionRecyclerViewAdapter(permissionList, new IOnItemClickListener() {
            @Override
            public void onItemClick(PermissionListItem item) {
                Log.i(TAG, "handle for:" + item.getPermissionTitle());
                if (!item.isPermissionGranted()) {
                    Preferences.setIsDialogDenied(SupportActivity.this, false);
                    currentPermissionToRequest = item;
                    if (item.getPermissionKey().equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            if (!PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(SupportActivity.this)) {
                                showLocationPermissionSettingsAlert(true);
                            }
                        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            if (!PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(SupportActivity.this)) {
                                showLocationPermissionSettingsAlert(true);
                            }
                        } else {
                            if (!PermissionHelper.INSTANCE.verifyTrackPermissions(SupportActivity.this)) {
                                showLocationPermissionSettingsAlert(false);
                            }
                        }
                    } else {
                        PermissionUtil.requestPermission(SupportActivity.this, item.getPermissionKey(), PermissionUtil.REQUEST_PERMISSION);
                    }

                } else {
                    currentPermissionToRequest = null;
                }
            }
        });

        permissionsRecyclerView.setAdapter(permissionsAdapter);
        permissionsAdapter.notifyDataSetChanged();
    }


    private void requestPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            requestAndroidSPermissions();
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            requestAndroidRAndAbovePermissions();
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                requestAndroidQPermissions();
            } else {
                requestAndroidPAndBelow();
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestAndroidQPermissions() {
        if (PermissionHelper.INSTANCE.verifyLocationBackgroundPermissions(this)) {
            dismissLocationDialog();
        } else {
            if (isPermissionDenied) {
                goToSettings();
            } else {
                requestForegroundAndBackgroundLocationPermissions();
            }
        }
    }

    private void requestForegroundAndBackgroundLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PermissionUtil.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION);
    }

    private void requestFineAndCoraseLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionUtil.REQUEST_FINE_AND_COARSE_LOCATION);
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

    private void requestForegroundLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PermissionUtil.REQUEST_TRACK_PERMISSION);
    }

    private void goToSettings() {
        IntentHelper.goToSettings(SupportActivity.this);
    }

    private void dismissLocationDialog() {
        if (locationDialog != null && locationDialog.getShowsDialog()) {
            locationDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.REQUEST_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentPermissionToRequest.updateStatus(getApplicationContext());
                    permissionsAdapter.notifyDataSetChanged();
                } else {
                    PermissionUtil.displaySimpleGrantPermissionMessage(SupportActivity.this, currentPermissionToRequest.getPermissionDenyMessage());
                }
                return;
            }
            case PermissionUtil.REQUEST_TRACK_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocationPermission();
                    } else {
                        currentPermissionToRequest.updateStatus(getApplicationContext());
                        permissionsAdapter.notifyDataSetChanged();
                        dismissLocationDialog();
                    }
                } else {
                    PermissionUtil.displaySimpleGrantPermissionMessage(SupportActivity.this, currentPermissionToRequest.getPermissionDenyMessage());
                    isPermissionDenied = true;
                }
                return;
            }
            case PermissionUtil.REQUEST_TRACK_BACKGROUND_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentPermissionToRequest.updateStatus(getApplicationContext());
                    permissionsAdapter.notifyDataSetChanged();
                    dismissLocationDialog();
                } else {
                    PermissionUtil.displaySimpleGrantPermissionMessage(SupportActivity.this, currentPermissionToRequest.getPermissionDenyMessage());
                    isPermissionDenied = true;
                }
                return;
            }
            case PermissionUtil.REQUEST_FINE_AND_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocationPermission();
                    } else {
                        currentPermissionToRequest.updateStatus(getApplicationContext());
                        permissionsAdapter.notifyDataSetChanged();
                        dismissLocationDialog();
                    }
                } else {
                    PermissionUtil.displaySimpleGrantPermissionMessage(SupportActivity.this, currentPermissionToRequest.getPermissionDenyMessage());
                    isPermissionDenied = true;
                }
                return;
            }
            case PermissionUtil.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentPermissionToRequest.updateStatus(getApplicationContext());
                    permissionsAdapter.notifyDataSetChanged();
                    dismissLocationDialog();
                } else {
                    PermissionUtil.displaySimpleGrantPermissionMessage(SupportActivity.this, currentPermissionToRequest.getPermissionDenyMessage());
                    isPermissionDenied = true;
                }
                return;
            }

        }

    }




    private void showLocationPermissionSettingsAlert(boolean isAndroidQ) {
        if (locationDialog != null) {
            locationDialog.dismiss();
        }
        locationDialog = new LocationDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean(LocationDialog.IS_ANDROID_Q_AND_ABOVE, isAndroidQ);
        locationDialog.setArguments(bundle);
        locationDialog.setCancelable(false);
        locationDialog.show(getSupportFragmentManager(), "LocationDialogOnSupportActivity");
    }


    private void requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PermissionUtil.REQUEST_TRACK_BACKGROUND_PERMISSION);
    }

    @Override
    public void onAccept() {
        requestPermissions();
    }

    @Override
    public void onReject() {
        Preferences.setIsDialogDenied(this, true);
        if (locationDialog != null && locationDialog.getShowsDialog()) {
            locationDialog.dismiss();
        }
    }

    public interface IOnItemClickListener {
        void onItemClick(PermissionListItem item);
    }

    /**
     * Reads a resource file and populates the respective view to display it.
     */
    private void displayContactUsContent() throws IOException {
        TextView txtContactUs = ((TextView) findViewById(R.id.txtContactUs));
        txtContactUs.setText(Html.fromHtml(FileManager.getContentFromRawResource(this, R.raw.contact_us)));
        txtContactUs.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setCheckBoxViewStatus(int id, boolean status) {
        ((CheckBox) findViewById(id)).setChecked(status);
    }

    /**
     * Sets the username login
     */
    private void setTextUsername() {
        final String username = new TrackPreferences(this).getLogin();
        if (username != null) {
            ((TextView) findViewById(R.id.lblLoggedIn)).append(" as ");
            final TextView txtUsername = (TextView) findViewById(R.id.txtLoggedIn);
            ViewTreeObserver viewTreeObserver = txtUsername.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    float scaleDensity = getResources().getDisplayMetrics().scaledDensity;
                    txtUsername.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    float size = Util.getFitTextSize(txtUsername.getPaint(), txtUsername.getMeasuredWidth(), username);
                    if (size < MIN_SIZE_SP * scaleDensity) {
                        size = MIN_SIZE_SP * scaleDensity;
                        txtUsername.setSingleLine(false);
                    }
                    txtUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, size / scaleDensity);
                    txtUsername.append(username);
                }
            });
        }
    }

    private void setupAboutContent(final Activity activity, View layout) {
        ((TextView) findViewById(R.id.aboutVersion)).setText(BuildConfig.VERSION_NAME);
        TextView tv = (TextView) layout.findViewById(R.id.aboutOpenSource);
        tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = (WebView) LayoutInflater.from(v.getContext()).inflate(R.layout.webview, null);
                webView.loadUrl("file:///android_asset/open_source_licenses.html");
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.getSettings().setSupportZoom(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.getSettings().setDisplayZoomControls(false);
                new AlertDialog.Builder(activity, R.style.FullscreenAlertDialogStyle)
                        .setTitle(v.getContext().getString(R.string.thirdPartyLicenses))
                        .setView(webView)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
        TextView tv_tou = (TextView) layout.findViewById(R.id.aboutTermsOfUse);
        tv_tou.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tv_tou.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent eulaIntent = new Intent(activity, EulaActivity.class);
                eulaIntent.putExtra(EulaActivity.DISPLAY_TERMS_OF_USE_READONLY_KEY, true);
                activity.startActivity(eulaIntent);
            }
        });
    }
}
