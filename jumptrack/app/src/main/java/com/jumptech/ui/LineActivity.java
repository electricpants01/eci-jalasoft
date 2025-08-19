package com.jumptech.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.jumptech.android.util.Message;
import com.jumptech.android.util.Preferences;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.Line;
import com.jumptech.tracklib.data.Plate;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.LineRepository;
import com.jumptech.tracklib.repository.PlateRepository;
import com.jumptech.tracklib.utils.CaptureAPI;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.ui.adapters.LicensePlateRecyclerViewAdapter;
import com.jumptech.ui.adapters.PartialReasonsAdapter;
import com.jumptech.tracklib.utils.scan.integrator.ZxingClientApp;
import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.android.Capture;
import com.socketmobile.capture.client.ConnectionState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class LineActivity extends BaseActivity implements CaptureAPI {

    private static final String TAG = LineActivity.class.getSimpleName();

    private static final String PARTIAL_REASON_KEY = "partial_reason";
    private static final String SCAN_TEMPLATE = "Scan: %s";

    private EditText partialQuantityEditText;

    private ArrayList<String> _partialChecked = new ArrayList<>();

    private RecyclerView platesRecyclerView;
    private RecyclerView partialReasonsRecyclerView;

    private View scanView;
    private TextView scanTextView;
    private TextView scanQuantityTextView;
    private TextView targetTextView;

    private Button removeLineButton;
    private Button saveLineButton;

    private TextView loadedTextView;
    private View loadedView;
    private TextView uomTextView;

    private TextView skuTextView;
    private TextView descriptionTextView;

    private TrackPreferences preferences;
    private Menu screenMenu;
    private boolean finished;
    private Long deliveryKey;
    private Long lineKey;

    private ZxingClientApp app;
    private Observer cameraObserver = new Observer() {
        @Override
        public void update(Observable observable, Object data) {
            if (data instanceof String) {
                boolean validScan = handleScan(data.toString(), Scanner.CAMERA);
                app.getObserver().getOutput().emit(!validScan);
                if (validScan) {
                    updateData();
                }
            }
        }
    };
    private TextWatcher partialQuantityTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Line line = LineRepository.fetchByID(LineActivity.this, lineKey);
            updatePartialReasonUIComponents(line);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public LineActivity() {
        super(TAG);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.line_activity);

        preferences = new TrackPreferences(this);

        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView);
        }

        finished = getIntent().getLongExtra(BundleTrack.KEY_SIGNATURE, Signature.NEW) != Signature.NEW;

        deliveryKey = getIntent().getLongExtra(BundleTrack.KEY_DELIVERY, 0);
        lineKey = getIntent().getLongExtra(BundleTrack.KEY_LINE, 0);

        initUI();

        Line line = LineRepository.fetchByID(this, lineKey);
        Delivery delivery = DeliveryRepository.fetchDeliveryByKey(this, deliveryKey);

        if (savedInstanceState != null) {
            _partialChecked = savedInstanceState.getStringArrayList(PARTIAL_REASON_KEY);
        } else {
            _partialChecked = new ArrayList<>(Arrays.asList(line._partial_reason));
        }

        ((TextView) findViewById(R.id.deliveryName)).setText(delivery.name);
        ((TextView) findViewById(R.id.deliveryDisplay)).setText(delivery.display);
        ((ImageView) findViewById(R.id.deliveryLabel)).setImageResource(delivery.type.getIcon());
        ((TextView) findViewById(R.id.itemDetailProductName)).setText(line._name);

        skuTextView.setText(line._product_no);
        skuTextView.setVisibility(TextUtils.isEmpty(line._product_no) ? View.GONE : View.VISIBLE);
        descriptionTextView.setText(line._desc);
        descriptionTextView.setVisibility(TextUtils.isEmpty(line._desc) ? View.GONE : View.VISIBLE);
        uomTextView.setText(line._uom);

        if (delivery.canShowQtyLoaded(line)) {
            loadedTextView.setText(String.valueOf(line._qty_loaded));
        } else {
            loadedView.setVisibility(View.GONE);
        }

        //if we have a target quantity
        if (line._qty_target != null) {
            findViewById(R.id.ofLabel).setVisibility(View.VISIBLE);
            targetTextView.setVisibility(View.VISIBLE);
            targetTextView.setText(String.valueOf(line._qty_target));
        }
        // else if we are an editable unschedule pickup
        else if (!finished) {
            removeLineButton.setVisibility(View.VISIBLE);
            saveLineButton.setVisibility(View.VISIBLE);
        }

        setPartialReasons();
        setLicensePlates();

        boolean hasScan = !TextUtils.isEmpty(line._scan);
        if (hasScan) {
            scanView.setVisibility(View.VISIBLE);
            scanTextView.setText(String.format(SCAN_TEMPLATE, line._scan));
            updateScanQuantity(line);
        }

        partialQuantityEditText.addTextChangedListener(partialQuantityTextWatcher);
        if (savedInstanceState == null) {
            partialQuantityEditText.setText(String.valueOf(line._qty_accept));
        }

        app = (ZxingClientApp) getApplicationContext();
        app.getObserver().getInput().addObserver(cameraObserver);
    }

    private void initUI() {
        loadedTextView = findViewById(R.id.loadedTextView);
        loadedView = findViewById(R.id.loadedView);
        uomTextView = findViewById(R.id.uomTextView);

        scanView = findViewById(R.id.scanView);
        scanTextView = findViewById(R.id.scanTextView);
        scanQuantityTextView = findViewById(R.id.scanQuantityTextView);
        targetTextView = findViewById(R.id.targetTextView);

        skuTextView = findViewById(R.id.skuTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);

        partialReasonsRecyclerView = findViewById(R.id.partialReasonsRecyclerView);
        partialReasonsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        partialReasonsRecyclerView.setHasFixedSize(true);
        platesRecyclerView = findViewById(R.id.platesRecyclerView);
        platesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        platesRecyclerView.setHasFixedSize(true);

        removeLineButton = findViewById(R.id.removeLineButton);
        saveLineButton = findViewById(R.id.saveLineButton);
        removeLineButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LineRepository.deleteByKey(LineActivity.this, lineKey);
                finish();
            }
        });
        saveLineButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LineActivity.this.onBackPressed();
            }
        });

        partialQuantityEditText = findViewById(R.id.partialQuantityEditText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String pendingMessage = Preferences.getPendingMessage(this);
        if (!TextUtils.isEmpty(pendingMessage)) {
            Message.show(pendingMessage, this);
            Preferences.clearPendingMessage(this);
        }
        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && !finished) {
            setCaptureAPIListener(this);
        }
        Line line = LineRepository.fetchByID(this, lineKey);
        if (line._scanning) {
            partialQuantityEditText.setText(String.valueOf(line._qty_accept));
        }
    }

    @Override
    protected void onPause() {
        setCaptureAPIListener(null);
        super.onPause();
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
        inflater.inflate(R.menu.scanmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.invoiceSelectScanMenuItem) {
            startScanning();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(PARTIAL_REASON_KEY, _partialChecked);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (partialQuantityEditText.getText().toString().length() != 0) {

            long lineKey = (int) getIntent().getLongExtra(BundleTrack.KEY_LINE, -1);

            int qty = Integer.parseInt(partialQuantityEditText.getText().toString());

            LineRepository.updateQtyAndPartial(this, lineKey, qty, _partialChecked);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Scan onDestroy line");
        app.getObserver().getInput().deleteObserver(cameraObserver);
        super.onDestroy();
    }

    private void updateData() {
        Line line = LineRepository.fetchByID(this, lineKey);
        setLicensePlates();
        partialQuantityEditText.setText(String.valueOf(line._qty_accept));
        updatePartialReasonUIComponents(line);
        updateScanQuantity(line);
    }

    /**
     * Defines the view properties for partial reason UI components
     */
    private void updatePartialReasonUIComponents(Line line) {
        partialQuantityEditText.setTextColor(getResources().getColor(partialQuantityEditText.getText().toString().equals("" + line._qty_target) ? R.color.qty_target_on : R.color.qty_target_off));
        if (!TextUtils.isEmpty(partialQuantityEditText.getText()) && preferences.getPartialReasons().length > 0) {
            boolean partial = line._qty_target != null && Integer.parseInt(partialQuantityEditText.getText().toString()) < line._qty_target;
            if (!partial) {
                _partialChecked = new ArrayList<>();
            }
            partialReasonsRecyclerView.setVisibility(partial ? View.VISIBLE : View.GONE);
            findViewById(R.id.itemDetailPartialReasonsTag).setVisibility(partialReasonsRecyclerView.getVisibility());
        }
        if (finished || line._scanning) {
            partialQuantityEditText.setBackgroundColor(0);
            partialQuantityEditText.setEnabled(false);
            partialQuantityEditText.setFocusable(false);
        }
        //((PartialReasonsAdapter) partialReasonsRecyclerView.getAdapter()).setEnabled(!finished);
    }

    private void updateScanQuantity(Line line) {
        if (line._scanning) {
            scanQuantityTextView.setText(String.valueOf(line._qty_accept));
        }
    }

    private void setLicensePlates() {
        final List<Plate> plates = PlateRepository.getPlatesWithoutScan(this, deliveryKey, lineKey);
        if (plates.isEmpty()) {
            platesRecyclerView.setVisibility(View.GONE);
        } else {
            platesRecyclerView.setVisibility(View.VISIBLE);
            platesRecyclerView.setAdapter(new LicensePlateRecyclerViewAdapter(plates));
            platesRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void setPartialReasons() {
        final List<String> items = Arrays.asList(preferences.getPartialReasons());
        List<Boolean> checks = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); ++i)
            checks.add(i, _partialChecked.contains(items.get(i)));
        if (!items.isEmpty()) {
            partialReasonsRecyclerView.setAdapter(new PartialReasonsAdapter(items, checks, new PartialReasonsAdapter.OnReasonChangedListener() {
                @Override
                public void onChanged(int which, boolean isChecked) {
                    if (isChecked) _partialChecked.add(items.get(which));
                    else _partialChecked.remove(items.get(which));
                }
            }));
            partialReasonsRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        Menu menu = navView.getMenu();
        menu.findItem(R.id.LogoutMenuItem).setVisible(false);
        menu.findItem(R.id.ExitRouteMenuItem).setVisible(false);
        menu.findItem(R.id.selectDefaultMap).setVisible(false);

    }

    @Override
    protected void scannerSelected() {
        enableOrDisableMenuOptions();
        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && !finished) {
            setCaptureAPIListener(this);
        }
    }

    private void enableOrDisableMenuOptions() {
        if (this.screenMenu == null) {
            return;
        }
        MenuItem menuItem = this.screenMenu.findItem(R.id.invoiceSelectScanMenuItem);
        menuItem.setVisible(isCameraVisible(preferences));

        MenuItem bluetoothMenuItem = this.screenMenu.findItem(R.id.bluetoothScannerMenuItem);
        bluetoothMenuItem.setVisible(isBluetoothScannerVisible(preferences));
        bluetoothMenuItem.setIcon(!Capture.get().getDevices().isEmpty() ? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_red);
    }

    public boolean isCameraVisible(TrackPreferences pref) {
        return !finished && pref.getScanner().isManualTrigger();
    }

    public boolean isBluetoothScannerVisible(TrackPreferences pref) {
        return !finished && pref.getScanner().isBluetooth();
    }

    private void startScanning() {
        if (PermissionUtil.hasPermission(this, Manifest.permission.CAMERA)) {
            switch (preferences.getScanner()) {
                case CAMERA:
                    Intent intent = new Intent(this, ContinuousCaptureActivity.class);
                    startActivity(intent);
                    break;
                default:
            }
        } else {
            PermissionUtil.requestPermission(this, Manifest.permission.CAMERA, PermissionUtil.REQUEST_CAMERA_FOR_SCANNER);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.REQUEST_CAMERA_FOR_SCANNER: {
                Integer msgResult = PermissionUtil.checkForCameraPermission(grantResults);
                if (msgResult == null) {
                    startScanning();
                } else {
                    PermissionUtil.displayGrantPermissionMessage(LineActivity.this, msgResult, R.string.camera_access_required, R.string.open_settings, android.R.string.cancel);
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onCaptureAPIData(String data) {
        if (handleScan(data, Scanner.SOCKET_MOBILE)) {
            updateData();
        }
    }

    @Override
    public void onCaptureAPIError(CaptureError captureError, ConnectionState connectionState) {
        handleSocketScannerError(captureError, connectionState);
    }

    @Override
    public void onDeviceReady() {
        enableOrDisableMenuOptions();
    }

    public boolean handleScan(String scan, Scanner scanner) {
        Plate plate = PlateRepository.fetchPlateWitScan(this, deliveryKey, scan);
        if (plate != null) {
            if (!PlateRepository.updatePlateScan(this, plate)) {
                Preferences.setPendingMessage(getString(R.string.itemAlreadyScanned), this);
            } else {
                Preferences.clearPendingMessage(this);
            }
        } else {
            Line line = LineRepository.fetchLine(this, deliveryKey, scan);
            if (line != null && lineKey == line._key) {
                LineRepository.updateQty(this, line._key);
                Preferences.clearPendingMessage(this);
            } else {
                Preferences.setPendingMessage(getString(R.string.itemNotInDelivery), this);
            }
        }
        String pendingMessage = Preferences.getPendingMessage(this);
        if (scanner == Scanner.SOCKET_MOBILE && !TextUtils.isEmpty(pendingMessage)) {
            Message.show(pendingMessage, this);
            Preferences.clearPendingMessage(this);
        }
        return TextUtils.isEmpty(pendingMessage);
    }

}
