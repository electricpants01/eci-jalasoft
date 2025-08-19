package com.jumptech.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.networking.JTRepository;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.Product;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.LineRepository;
import com.jumptech.tracklib.utils.CaptureAPI;
import com.jumptech.tracklib.utils.IntentHelper;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.tracklib.utils.scan.ScanLauncher;
import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.client.ConnectionState;

import org.apache.commons.lang.StringUtils;

public class ProductSearchActivity extends BaseActivity implements CaptureAPI {

    private static final String TAG = ProductSearchActivity.class.getSimpleName();

    private TrackPreferences preferences;

    public ProductSearchActivity() {
        super(TAG);
    }

    private View loadingLayout;
    private TextView loadingTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.produce_search_activity);

        loadingLayout = findViewById(R.id.loadingPanel);
        loadingTextView = findViewById(R.id.loadingTextView);

        preferences = new TrackPreferences(this);

        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView);
        }
        super.setNavigationFragmentEnabled(false);
        Delivery delivery = DeliveryRepository.fetchDeliveryByKey(this, getIntent().getLongExtra(BundleTrack.KEY_DELIVERY, 0));

        ((TextView) findViewById(R.id.companyName)).setText(delivery.name);

        final EditText searchText = findViewById(R.id.enterReturnSKU);
        searchText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchText.selectAll();
            }
        });
        searchText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.searchReturnButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                query(searchText.getText().toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (preferences.getScanner() == Scanner.SOCKET_MOBILE) {
            setCaptureAPIListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        setCaptureAPIListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setIcon(R.drawable.scan_icon);
        menu.getItem(0).setTitleCondensed(getString(R.string.scan));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.returns_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("MenuItem", "" + item.getItemId());
        if (item.getItemId() == R.id.scanReturn) {
            startScanning();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    query(result.getContents());
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "not scan");
                }
                break;
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
                    PermissionUtil.displayGrantPermissionMessage(ProductSearchActivity.this, msgResult, R.string.camera_access_required, R.string.open_settings, android.R.string.cancel);
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startScanning() {
        switch (new TrackPreferences(this).getScanner()) {
            case CAMERA:
                if (PermissionUtil.hasPermission(this, Manifest.permission.CAMERA)) {
                    ScanLauncher.LaunchScanner(ProductSearchActivity.this, IntentIntegrator.ONE_D_CODE_TYPES);
                } else {
                    PermissionUtil.requestPermission(this, Manifest.permission.CAMERA, PermissionUtil.REQUEST_CAMERA_FOR_SCANNER);
                }
                break;
            default:
        }
    }

    private void query(final String search) {
        if (StringUtils.trimToNull(search) == null)
            return;
        hideKeyBoard();
        searchItem(search);
    }

    private void searchItem(final String search) {
        loadingLayout.setVisibility(View.VISIBLE);
        loadingTextView.setVisibility(View.VISIBLE);
        loadingTextView.setText(R.string.searchActive);
        JTRepository.searchProduct(search, this, new JTRepository.OnProductResponse() {
            @Override
            public void success(Product product) {
                loadingLayout.setVisibility(View.GONE);
                loadingTextView.setVisibility(View.GONE);
                if (product != null) {
                    goLineScreen(product);
                } else {
                    ActivityUtil.checkLoginSession(ProductSearchActivity.this);
                }
            }

            @Override
            public void error(String message) {
                loadingLayout.setVisibility(View.GONE);
                loadingTextView.setVisibility(View.GONE);
                Product product = new Product();
                product.name = StringUtils.trimToEmpty(search);
                product.no = product.name;
                goLineScreen(product);
                ActivityUtil.checkLoginSession(ProductSearchActivity.this);
            }
        });
    }

    private void goLineScreen(Product product) {
        long lineKey = LineRepository.addLine(this, getIntent().getLongExtra(BundleTrack.KEY_DELIVERY, 0), product);
        startActivity(new Intent(ProductSearchActivity.this, LineActivity.class).putExtras(getIntent().getExtras()).putExtra(BundleTrack.KEY_LINE, lineKey));
        finish();
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        //No navigation menu is needed for this screen
    }

    public boolean handleScan(String scan) {
        Log.v(TAG, "got scan: " + scan);
        query(scan);
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
    }
}