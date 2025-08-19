package com.jumptech.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jumptech.android.util.Message;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.Plate;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.data.WindowTime;
import com.jumptech.tracklib.db.Business;
import com.jumptech.tracklib.repository.SiteRepository;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.PhotoRepository;
import com.jumptech.tracklib.repository.PlateRepository;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.repository.SignatureRepository;
import com.jumptech.tracklib.repository.StopRepository;
import com.jumptech.tracklib.room.entity.Site;
import com.jumptech.tracklib.utils.CaptureAPI;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.tracklib.utils.factories.AlertDialogFactory;
import com.jumptech.ui.adapters.DeliveryRecyclerViewAdapter;
import com.jumptech.ui.adapters.DividerItemDecoration;
import com.jumptech.tracklib.utils.scan.ScanLauncher;
import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.android.Capture;
import com.socketmobile.capture.client.ConnectionState;
import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopActivity extends BaseActivity implements CaptureAPI, Message.MessageListener {

    private static final String TAG = StopActivity.class.getSimpleName();

    public static final String PHOTO_QUANTITY = "%s/%s";
    private static final String CONFIRM_ACCEPT_ALL_TAG = "confirmAcceptAllTag";

    private ImageButton _photoButton;
    private Button btnAcceptAll;
    private RecyclerView deliveriesRecyclerView;
    private TextView txtPhotoInfo;
    private TextView tvDriverNote;
    private TextView phoneNumber;
    private Site site;
    private Integer maxPhotosAllowed;
    private boolean _finished = true;
    private DeliveryRecyclerViewAdapter deliveriesAdapter;
    private boolean partialEnabled = false;
    private Set<Long> deliveriesIdChecked = new HashSet<>();
    double latitude = 0;
    double longitude = 0;

    private Long stopKey;

    private TrackPreferences preferences;
    private Menu screenMenu;
    private boolean isPreviousScreenRoute = false;

    /**
     * Stores the stop item
     */
    private Stop mStop;

    public StopActivity() {
        super(TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_activity);
        preferences = new TrackPreferences(this);

        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView);
        }

        stopKey = getIntent().getLongExtra(BundleTrack.KEY_STOP, -1);
        isPreviousScreenRoute = getIntent().getBooleanExtra(BundleTrack.BUNDLE_INTENT_FROM_ROUTE, false);
        preferences.setDeliveryStopInProgress(stopKey);
        mStop = StopRepository.fetchStop(this, stopKey);
        if (mStop == null) {
            finish();
            return;
        } else {
            site = SiteRepository.fetchSite(this, mStop.site_key);
        }

        TextView tvWindow = findViewById(R.id.window);
        if (mStop.getWindow_id() != null) {
            StopActivity.evaluateWindowTimeList(StopActivity.this.getApplicationContext(),
                    mStop.getWindowTimeList(),
                    mStop.getWindowDisplay(),
                    tvWindow,
                    (ImageView) findViewById(R.id.stopWindowError));
        } else tvWindow.setVisibility(View.GONE);

        getIntent().putExtra(BundleTrack.KEY_SIGNATURE, mStop.signature_key != null ? mStop.signature_key : Signature.NEW);

        ((TextView) findViewById(R.id.stopName)).setText(mStop.name);
        ((TextView) findViewById(R.id.address)).setText(mStop.address);
        if (site.getPhone() != null) {
            phoneNumber = findViewById(R.id.phoneNumber);
            phoneNumber.setText(site.getPhone());
            if (checkNumber(site.getPhone())) {
                phoneNumber.setTextColor(R.color.text_color_hint);
                phoneNumber.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callContact(site.getPhone());
                    }
                });
            }
        }

        _photoButton = findViewById(R.id.cameraButton);
        deliveriesRecyclerView = findViewById(R.id.rvDeliveries);
        deliveriesRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        deliveriesRecyclerView.setHasFixedSize(true);
        deliveriesRecyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.list_divider));
        txtPhotoInfo = findViewById(R.id.txtPhotoInfo);
        maxPhotosAllowed = preferences.getImageMax();
        _finished = mStop.finished();
        if (_finished) {
            findViewById(R.id.signingLayout).setVisibility(View.GONE);
        } else {
            btnAcceptAll = findViewById(R.id.acceptAllButton);
            btnAcceptAll.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    acceptAll();
                }
            });
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocationName(mStop.address, 1);
                if (addresses.size() > 0) {
                    latitude = addresses.get(0).getLatitude();
                    longitude = addresses.get(0).getLongitude();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            findViewById(R.id.mapButton).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String navAddress = Util.EncodeValueWithURLEncoder(mStop.address);
                    try {
                        if (latitude != 0 && longitude != 0) {
                            if (isPackageInstalled(Util.wazePackageName, getPackageManager())) {
                                if (preferences.getDefaulMapApp().equals(Util.wazePackageName)) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude)).setPackage(Util.wazePackageName));
                                } else {
                                    if (preferences.getDefaulMapApp().equals(Util.googleMapsPackageName)) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + latitude + "," + longitude)).setPackage(Util.googleMapsPackageName));
                                    } else {
                                        AlertDialogFactory.createMapListDialog(StopActivity.this, latitude, longitude).show();
                                    }
                                }
                            } else {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + latitude + "," + longitude)).setPackage(Util.googleMapsPackageName));
                            }
                        }
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, TrackException.MSG_EXCEPTION, e);
                        Message.show(R.string.navigationUnavailable, StopActivity.this);
                    }
                }
            });

            SignatureTool.createCameraButton(this, _photoButton);

            findViewById(R.id.noteButton).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    NoteDialogFragment noteFragment = NoteDialogFragment.init(mStop.name, "");
                    noteFragment.setOnNoteSaveListener(new NoteDialogFragment.OnNoteSaveListener() {
                        @Override
                        public void onSaveNote(String note) {
                            setDriverNote(note);
                        }
                    });
                    noteFragment.show(getSupportFragmentManager(), NoteDialogFragment.FRAGMENT_NAME);
                }
            });

        }

        //forward to delivery when necessary
        {
            long deliveryKey = getIntent().getLongExtra(BundleTrack.KEY_DELIVERY, 0);
            if (deliveryKey != 0) {
                getIntent().removeExtra(BundleTrack.KEY_DELIVERY);
                startDelivery(deliveryKey);
            }
        }
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void acceptAll() {
        if (isAllDeliveriesPlatesScanCompleted()) {
            goSignatureScreen();
        } else {
            Message.showQuestion(R.string.unscanned_items_message, this, CONFIRM_ACCEPT_ALL_TAG);
        }
    }

    private boolean isAllDeliveriesPlatesScanCompleted() {
        List<Delivery> deliveries = deliveriesAdapter.getDeliveries();
        for (Delivery delivery : deliveries) {
            if (delivery.hasPlates && !delivery.isLinesPlatesScanCompleted()) {
                return false;
            }
        }
        return true;
    }

    private void goSignatureScreen() {
        if (partialEnabled) {
            Business.from(StopActivity.this).signatureDelivery(deliveriesAdapter.getDeliveriesIdChecked());
        } else {
            Business.from(StopActivity.this).signatureStop(stopKey);
        }
        preferences = new TrackPreferences(this);
        preferences.setLastScreenBeforeSignatureImage(TrackPreferences.enumLastScreenBeforeSignature.DELIVERY_STOP.name());
        startActivity(new Intent(StopActivity.this, SignatureImageActivity.class));
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        Menu menu = navView.getMenu();
        menu.findItem(R.id.LogoutMenuItem).setVisible(false);
        menu.findItem(R.id.ExitRouteMenuItem).setVisible(false);
        menu.findItem(R.id.selectDefaultMap).setVisible(Util.isPackageInstalled(Util.wazePackageName, getPackageManager()));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.REQUEST_CAMERA_FOR_SCANNER:
            case PermissionUtil.REQUEST_CAMERA: {
                Integer msgResult = PermissionUtil.checkForCameraPermission(grantResults);
                if (msgResult == null) {
                    if (requestCode == PermissionUtil.REQUEST_CAMERA) {
                        SignatureTool.startSignaturePhoto(StopActivity.this);
                    } else {
                        startScanning();
                    }
                } else {
                    PermissionUtil.displayGrantPermissionMessage(StopActivity.this, msgResult, R.string.camera_access_required, R.string.open_settings, android.R.string.cancel);
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && !_finished) {
            setCaptureAPIListener(this);
        }

        Signature signature = SignatureRepository.newSignature(this);
        tvDriverNote = findViewById(R.id.driverNote);
        setDriverNote(signature._note);

        setPhotoCounterView();

        setDeliveriesAdapter();

        enableOrDisableMenuOptions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setCaptureAPIListener(null);
    }

    @Override
    public void onBackPressed() {
        if (StopRepository.stopHasChange(this, stopKey)) {

            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.discardEditsTitle)
                    .setMessage(R.string.discardEdits)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (stopKey > 0) {
                                PlateRepository.updatePlateResetStop(StopActivity.this, stopKey);
                            } else {
                                DeliveryRepository.deleteMediaDelivery(StopActivity.this.getApplicationContext());
                                RouteRepository.clearPendingFromStopKey(StopActivity.this, stopKey);
                            }
                            if (isPreviousScreenRoute) {
                                StopActivity.super.onBackPressed();
                            } else {
                                goToRouteActivity();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
        } else if (preferences.isDeliveryStopInProgress()) {
            if (isPreviousScreenRoute) {
                StopActivity.super.onBackPressed();
            } else {
                goToRouteActivity();
            }
        } else super.onBackPressed();
    }

    private void goToRouteActivity() {
        Intent intent = new Intent(this, RouteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void scannerSelected() {
        enableOrDisableMenuOptions();
        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && !_finished) {
            setCaptureAPIListener(this);
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
        inflater.inflate(R.menu.selectdeliveryitemsmenu, menu);
        menu.findItem(R.id.partialEnableItem).setVisible(!_finished);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.partialEnableItem) {
            partialEnabled = !partialEnabled;
            if (partialEnabled) {
                item.setTitle(R.string.partial_disable);
                btnAcceptAll.setText(R.string.acceptSelected);
                btnAcceptAll.setEnabled(!deliveriesAdapter.getDeliveriesIdChecked().isEmpty());
            } else {
                item.setTitle(R.string.partial_enable);
                btnAcceptAll.setText(R.string.acceptAllLabel);
                btnAcceptAll.setEnabled(true);
            }
            deliveriesAdapter.setPartialEnable(partialEnabled);
            deliveriesAdapter.notifyDataSetChanged();
            return true;
        } else if (item.getItemId() == R.id.cameraMenuItem) {
            startScanning();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setDeliveriesAdapter() {
        final List<Delivery> deliveries = DeliveryRepository.getAllDeliveriesByStopKey(this, stopKey);
        deliveriesAdapter = new DeliveryRecyclerViewAdapter(deliveries, deliveriesIdChecked);
        deliveriesAdapter.setPartialEnable(partialEnabled);
        deliveriesAdapter.setOnDeliveryClickListener(new DeliveryRecyclerViewAdapter.OnDeliveryListener() {
            @Override
            public void onDeliveryClick(Delivery delivery) {
                startDelivery(delivery.id);
            }

            @Override
            public void onDeliveryChecked(Delivery delivery, Set<Long> deliveriesChecked) {
                btnAcceptAll.setEnabled(!deliveriesChecked.isEmpty());
            }
        });
        deliveriesRecyclerView.setAdapter(deliveriesAdapter);

        //back through stop if there are no more deliveries on it
        if (deliveries.isEmpty()) finish();
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

    private void enableOrDisableMenuOptions() {
        if (this.screenMenu == null) {
            return;
        }
        MenuItem menuItem = this.screenMenu.findItem(R.id.cameraMenuItem);
        menuItem.setVisible(isCameraVisible(preferences));

        MenuItem bluetoothMenuItem = this.screenMenu.findItem(R.id.bluetoothMenuItem);
        bluetoothMenuItem.setVisible(isBluetoothScannerVisible(preferences));
        bluetoothMenuItem.setIcon(!Capture.get().getDevices().isEmpty() ? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_red);
    }

    public boolean isCameraVisible(TrackPreferences pref) {
        return !_finished && pref.getScanner().isManualTrigger();
    }

    public boolean isBluetoothScannerVisible(TrackPreferences pref) {
        return !_finished && pref.getScanner().isBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SignatureTool.SIGNATURE_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
            setPhotoCounterView();
        } else if (requestCode == IntentIntegrator.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                handleScan(result.getContents());
            } else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "Not scan");
            }
        }
    }

    private void handleScan(String data) {
        final List<Delivery> deliveries = DeliveryRepository.getAllDeliveriesByStopKey(this, stopKey);
        boolean alreadyNotOnDelivery = false;
        boolean alreadyIgnored = false;
        for (Delivery delivery : deliveries) {
            Plate plate = PlateRepository.fetchPlateWitScan(this, delivery.id, data);
            if (plate != null) {
                if (!PlateRepository.updatePlateScan(this, plate)) {
                    if (!alreadyIgnored) {
                        Message.showInfo(R.string.item_ignored, R.string.itemAlreadyScanned, this);
                        alreadyIgnored = true;
                        alreadyNotOnDelivery = false;
                    }
                } else {
                    setDeliveriesAdapter();
                }
            } else {
                if (!alreadyNotOnDelivery) {
                    Message.showInfo(R.string.item_not_on_delivery, R.string.itemNotInDelivery, this);
                    alreadyIgnored = false;
                    alreadyNotOnDelivery = true;
                }
            }
        }
    }

    private void setPhotoCounterView() {
        int pending = PhotoRepository.photoNewCount(this);
        txtPhotoInfo.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
        txtPhotoInfo.setText(String.format(PHOTO_QUANTITY, String.valueOf(pending), maxPhotosAllowed.toString()));
    }

    private void setDriverNote(String driverNote) {
        if (tvDriverNote != null) {
            tvDriverNote.setVisibility(driverNote != null ? View.VISIBLE : View.GONE);
            tvDriverNote.setText(StringUtils.trimToEmpty(driverNote));
        }
    }

    private void startDelivery(long deliveryKey) {
        if (deliveryKey == 0) return;
        startActivity(new Intent(this, DeliveryActivity.class).putExtras(getIntent().getExtras()).putExtra(BundleTrack.KEY_DELIVERY, deliveryKey));
    }

    private boolean checkNumber(String phone) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        boolean isValidPhone = true;
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phone, "US");
            isValidPhone = phoneUtil.isValidNumber(phoneNumber);
            String formattedNumber = (phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
            this.phoneNumber.setText(formattedNumber);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
        return isValidPhone;
    }


    private void callContact(String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    /**
     * This methods evaluates WindowTime values against current device's time and Display WindowDisplay
     *
     * @param appContext      the application context
     * @param windowTimeList  the list of WindowTime time ranges
     * @param windowDisplay   the window data to display
     * @param stopWindow      the UI window TextView
     * @param stopWindowError the UI error window icon ImageView
     */
    public static void evaluateWindowTimeList(Context appContext, List<WindowTime> windowTimeList, String windowDisplay, TextView stopWindow, ImageView stopWindowError) {
        boolean windowWithError = true;
        Calendar c = Calendar.getInstance();
        long daySeconds = (c.get(Calendar.SECOND) +
                c.get(Calendar.MINUTE) * 60 +
                c.get(Calendar.HOUR_OF_DAY) * 3600);
        for (WindowTime wt : windowTimeList) {
            if (daySeconds >= wt.getStartSec() && daySeconds <= wt.getEndSec()) {
                windowWithError = false;
                break;
            }
        }
        stopWindow.setText(windowDisplay);
        stopWindow.setVisibility(View.VISIBLE);
        stopWindow.setTextColor(ContextCompat.getColor(appContext, windowWithError
                ? R.color.stop_windowDisplay_error_text_color
                : R.color.stop_windowDisplay_normal_text_color));
        stopWindowError.setVisibility(windowWithError ? View.VISIBLE : View.GONE);
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

    @Override
    public void onMessageOk(String tag) {
        if (tag.equals(CONFIRM_ACCEPT_ALL_TAG)) {
            goSignatureScreen();
        }
    }

    @Override
    public void onMessageCancel(String tag) {
    }
}
