package com.jumptech.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.jumptech.android.util.Message;
import com.jumptech.android.util.Preferences;
import com.jumptech.android.util.Util;
import com.jumptech.android.util.UtilImage;
import com.jumptech.jumppod.R;
import com.jumptech.jumppod.UtilTrack;
import com.jumptech.jumppod.model.Image;
import com.jumptech.jumppod.model.Scanner;
import com.jumptech.tracklib.data.Delivery;
import com.jumptech.tracklib.data.DeliveryType;
import com.jumptech.tracklib.data.Line;
import com.jumptech.tracklib.data.Plate;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.db.Business;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.DeliveryRepository;
import com.jumptech.tracklib.repository.LineRepository;
import com.jumptech.tracklib.repository.PhotoRepository;
import com.jumptech.tracklib.repository.PlateRepository;
import com.jumptech.tracklib.repository.SignatureRepository;
import com.jumptech.tracklib.repository.StopRepository;
import com.jumptech.tracklib.utils.CaptureAPI;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.ui.adapters.DividerItemDecoration;
import com.jumptech.ui.adapters.LineRecyclerViewAdapter;
import com.jumptech.tracklib.utils.scan.integrator.ZxingClientApp;
import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.android.Capture;
import com.socketmobile.capture.client.ConnectionState;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class DeliveryActivity extends BaseActivity implements CaptureAPI, Message.MessageListener {

    private static final String TAG = DeliveryActivity.class.getSimpleName();

    private static final String CONFIRM_ACCEPT_TAG = "confirmAcceptTag";

    private ImageButton _photoButton = null;
    private Button _acceptButton = null;
    private ImageView deliveryTypeIcon = null;
    private TextView tvDriverNote;
    private TextView txtPhotoInfo;
    private Integer maxPhotosAllowed;
    private Boolean _isScanning = false;
    private Boolean _isBackPressed = false;

    /**
     * Stores the information of the delivery
     */
    private Delivery delivery;

    /**
     * Line cursor adapter that manages all list views
     */
    private LineRecyclerViewAdapter lineCursorAdapter;

    private RecyclerView linesRecyclerView;
    private int _imageResource = DeliveryType.DROPOFF.getIcon();

    private boolean _finished = true;

    /**
     * Stores the application context
     */
    private ZxingClientApp mApplication;

    /**
     * Indicates whether it is a NEW unscheduled delivery or pickup
     */
    private boolean isNewUnscheduled = false;

    /**
     * Indicate whether it is a editable delivery or pickup
     */
    private boolean isEditable = false;
    private Menu screenMenu;
    private TrackPreferences preferences;

    private Observer cameraObserver = new Observer() {
        @Override
        public void update(Observable observable, Object data) {
            if (Preferences.ignoreCameraScan(DeliveryActivity.this)) return;
            if (data instanceof String) {
                mApplication.getObserver().getOutput().emit(!handleScan(data.toString(), Scanner.CAMERA));
            }
        }
    };

    public DeliveryActivity() {
        super(TAG);
    }
    private long deliveryKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.delivery_activity);
        deliveryKey = getIntent().getLongExtra(BundleTrack.KEY_DELIVERY, 0);
        Preferences.setCurrentDeliveryKey(deliveryKey, this);
        preferences = new TrackPreferences(this);
        isNewUnscheduled = getIntent().getBooleanExtra(BundleTrack.KEY_NEW_UNSCHEDULED, false);
        if(!isNewUnscheduled) {
            preferences.setDeliveryInProgress(deliveryKey);
        }
        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView);
        }

        Long deliveryId = getIntent().getLongExtra(BundleTrack.KEY_DELIVERY,0);
        delivery = DeliveryRepository.fetchDeliveryByKey(this, deliveryId);
        delivery.loadLines(this);
        _imageResource = delivery.type.getIcon();

        ((TextView) findViewById(R.id.deliveryDisplay)).setText(delivery.display);
        ((TextView) findViewById(R.id.deliverySiteName)).setText(delivery.name);
        TextView companyAddressTextView = findViewById(R.id.deliverySiteAddress);

        linesRecyclerView = findViewById(R.id.rvProducts);
        linesRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        linesRecyclerView.setHasFixedSize(true);
        linesRecyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.list_divider));

        tvDriverNote = findViewById(R.id.driverNote);
        findViewById(R.id.noteButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteDialogFragment noteFragment = NoteDialogFragment.init(delivery.name, delivery.display);
                noteFragment.setOnNoteSaveListener(new NoteDialogFragment.OnNoteSaveListener() {
                    @Override
                    public void onSaveNote(String note) {
                        setDriverNote(note);
                    }
                });
                noteFragment.show(getSupportFragmentManager(), NoteDialogFragment.FRAGMENT_NAME);
            }
        });

        txtPhotoInfo = findViewById(R.id.txtPhotoInfo);
        maxPhotosAllowed = preferences.getImageMax();

        _acceptButton = findViewById(R.id.acceptButton);
        _acceptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                accept();
            }
        });
        deliveryTypeIcon = findViewById(R.id.deliveryTypeIcon);
        deliveryTypeIcon.setImageResource(_imageResource);
        setUIForDeliveryType(delivery.type, 0);

        if (delivery.address != null) {
            companyAddressTextView.setText(delivery.address);
        } else {
            companyAddressTextView.setVisibility(View.GONE);
        }

        if (delivery.note != null) {
            TextView dispNoteTxtTextView = findViewById(R.id.deliveryNotesTxt);
            dispNoteTxtTextView.setText(delivery.note);
            dispNoteTxtTextView.setVisibility(View.VISIBLE);
        }

        _photoButton = findViewById(R.id.camButton);
        SignatureTool.createCameraButton(this, _photoButton);

        long signatureKey = getIntent().getLongExtra(BundleTrack.KEY_SIGNATURE, Signature.NEW);
        _finished = signatureKey != Signature.NEW;
        if (_finished) {
            preferences.restartNavigationProgress();
            findViewById(R.id.driverNote).setVisibility(View.GONE);
            findViewById(R.id.signingLayout).setVisibility(View.GONE);
            findViewById(R.id.signatureLayout).setVisibility(View.VISIBLE);

            Signature signature = SignatureRepository.fetchSignature(this, signatureKey);

            ((TextView) findViewById(R.id.signatureNoteValue)).setText(signature._note);
            ((TextView) findViewById(R.id.signatureSigneeValue)).setText(signature._signee);
            ((TextView) findViewById(R.id.signatureTimeValue)).setText(UtilTrack.format(signature._signed));

            Bitmap sigBitmap = UtilImage.decodeFileToTarget(signature._path, 200, 100);
            ImageView imagePort = findViewById(R.id.signatureImage);
            if (sigBitmap != null) {
                imagePort.setImageBitmap(sigBitmap);
            } else {
                imagePort.setImageResource(R.drawable.nosignature);
            }

            String[] photos = PhotoRepository.photos(this, signatureKey);
            if (photos.length > 0) {
                LinearLayout photoLayout = findViewById(R.id.photoLayout);
                photoLayout.setVisibility(View.VISIBLE);

                LayoutInflater layoutInflater = LayoutInflater.from(this);
                int width = getWindowManager().getDefaultDisplay().getWidth();

                for (String photo : photos) {
                    View view = layoutInflater.inflate(R.layout.signature_image, photoLayout, false);
                    ImageView imageView = view.findViewById(R.id.deliveryImage1);
                    Bitmap b = Image.getScaledBitmapAR(BitmapFactory.decodeFile(photo), width / 4, new BitmapFactory.Options());
                    if (b != null) {
                        imageView.setImageBitmap(b);
                        photoLayout.addView(view);
                    }
                }
            }

        }
        isEditable = !_finished && delivery.isEditable();
        if (isEditable) {
            findViewById(R.id.editLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.btnDeliveryCancel).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    displayCancelConfirmationDialog();
                }
            });
            findViewById(R.id.btnAddline).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(DeliveryActivity.this, ProductSearchActivity.class).putExtras(getIntent().getExtras()));
                }
            });
        }
        mApplication = (ZxingClientApp) getApplicationContext();
        mApplication.getObserver().getInput().addObserver(cameraObserver);
    }//onCreate

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        Menu menu = navView.getMenu();
        menu.findItem(R.id.LogoutMenuItem).setVisible(false);
        menu.findItem(R.id.ExitRouteMenuItem).setVisible(false);
        menu.findItem(R.id.selectDefaultMap).setVisible(Util.isPackageInstalled(Util.wazePackageName,getPackageManager()));

    }

    /**
     * Displays delivery cancel confirmation dialog
     */
    private void displayCancelConfirmationDialog() {
        new AlertDialog.Builder(DeliveryActivity.this)
                .setCancelable(false)
                .setMessage(R.string.discardWarning)
                .setTitle(delivery.type == DeliveryType.PICKUP ? R.string.discardPickups : R.string.discardDeliveries)
                .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeliveryRepository.deleteMediaDelivery(DeliveryActivity.this.getApplicationContext());
                        DeliveryRepository.removeDelivery(DeliveryActivity.this, delivery.id);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create()
                .show();
    }

    /**
     * Displays delete edits confirmation dialog
     *
     * @return Boolean value which indicates whether the dialog is being shown
     */
    private boolean isDisplayDeleteEditsConfirmationDialog() {
        final long stopKey = getIntent().getLongExtra(BundleTrack.KEY_STOP, 0);
        //if (Fetch.stopHasChange(db(), stopKey)) {
        if (StopRepository.stopHasChange(this, stopKey)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(R.string.discardEdits)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DeliveryRepository.deleteMediaDelivery(DeliveryActivity.this.getApplicationContext());
                            DeliveryActivity.this.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.REQUEST_CAMERA_FOR_SCANNER:
            case PermissionUtil.REQUEST_CAMERA: {
                Integer msgResult = PermissionUtil.checkForCameraPermission(grantResults);
                if (msgResult == null) {
                    if (requestCode == PermissionUtil.REQUEST_CAMERA) {
                        SignatureTool.startSignaturePhoto(DeliveryActivity.this);
                    } else {
                        startScanning();
                    }
                } else {
                    PermissionUtil.displayGrantPermissionMessage(DeliveryActivity.this, msgResult, R.string.camera_access_required, R.string.open_settings, android.R.string.cancel);
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String pendingMessage = Preferences.getPendingMessage(this);
        if (!TextUtils.isEmpty(pendingMessage)) {
            Message.show(pendingMessage, this);
            Preferences.clearPendingMessage(this);
        }

        Preferences.setIgnoreCameraScan(false, this);
        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && !_finished) {
            setCaptureAPIListener(this);
        }

        if (getIntent().getLongExtra(BundleTrack.KEY_SIGNATURE, Signature.NEW) == Signature.NEW) {
            Signature signature = SignatureRepository.fetchSignature(this, Signature.NEW);
            setDriverNote(signature._note);
        }
        setPhotoCounterView();
        setListAdapter();
        enableOrDisableMenuOptions();
    }

    @Override
    protected void onPause() {
        setCaptureAPIListener(null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        _isBackPressed = true;
        if (isEditable) {
            if (!delivery.hasLines(this)) {
                displayCancelConfirmationDialog();
            } else if (!isNewUnscheduled || !isDisplayDeleteEditsConfirmationDialog()) {
                startActivity(new Intent(this,StopActivity.class).putExtra(BundleTrack.KEY_STOP, preferences.getStopKey()));
                finish();
            }
        } else {
            if(preferences.isDeliveryInProgress()){
                startActivity(new Intent(this,StopActivity.class).putExtra(BundleTrack.KEY_STOP, preferences.getStopKey()));
                finish();
            }else{
                super.onBackPressed();
            }
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
    protected void onDestroy() {
        Preferences.clearCurrentDeliveryKey(this);
        mApplication.getObserver().getInput().deleteObserver(cameraObserver);
        if (!_isBackPressed && isEditable && !delivery.hasLines(this)) {
            DeliveryRepository.deleteMediaDelivery(DeliveryActivity.this.getApplicationContext());
            DeliveryRepository.removeDelivery(this, delivery.id);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onactivityResult - requestCode:" + requestCode + " resultCode: " + resultCode);
        switch (requestCode) {
            case SignatureTool.SIGNATURE_PHOTO_REQUEST:
                if (Activity.RESULT_OK == resultCode) {
                    setPhotoCounterView();
                }
                break;
        }
    }

    private void setPhotoCounterView() {
        int pending = PhotoRepository.photoNewCount(this);
        txtPhotoInfo.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
        txtPhotoInfo.setText(String.format(StopActivity.PHOTO_QUANTITY, String.valueOf(pending), maxPhotosAllowed.toString()));
    }

    private void setDriverNote(String driverNote) {
        if (tvDriverNote != null) {
            tvDriverNote.setVisibility(driverNote != null ? View.VISIBLE : View.GONE);
            tvDriverNote.setText(StringUtils.trimToEmpty(driverNote));
        }
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

    private void setListAdapter() {
        Long deliveryId = getIntent().getLongExtra(BundleTrack.KEY_DELIVERY,-1);
        List<Line> lines = LineRepository.fetchLines(this, deliveryId, null, null);
        setUIForDeliveryType(delivery.type, lines.size());
        lineCursorAdapter = new LineRecyclerViewAdapter(this, lines, delivery);
        lineCursorAdapter.setOnLineClickListener(new LineRecyclerViewAdapter.OnLineListener() {
            @Override
            public void onLineClick(Line line) {
                Preferences.setIgnoreCameraScan(true, DeliveryActivity.this);
                startActivity(new Intent(DeliveryActivity.this, LineActivity.class).putExtras(getIntent().getExtras()).putExtra(BundleTrack.KEY_LINE, line._key));
            }
        });
        linesRecyclerView.setAdapter(lineCursorAdapter);
    }

    public void accept() {
        if (_isScanning) {
			/* TODO
			int missingItems = 0;
			for(int i = 0; i < _deliveryItem.getDetailList().size(); i++) {
				int scansByKey = JumpPODApplication.getDB().getScanTable().getNumberOfPlateScansByKey(_deliveryItem.getDetailList().get(i).getLineNumber());
				int scansByCode = JumpPODApplication.getDB().getScanTable().getNumberOfScansByKey(_deliveryItem.getDetailList().get(i).getLineNumber());
				int qty = scansByKey > scansByCode ? scansByKey : scansByCode;
				missingItems += ((_deliveryItem.getDetailList().get(i).getQty()) - (qty));
				_deliveryItem.getDetailList().get(i).setAcceptedQty(qty);
			}
			if(missingItems > 0) {
				PopupDialog.showPrompt(DeliveryItemDetailActivity.this
				                       , getString(R.string.deliveryPartialPlate, missingItems)
				                       , getString(android.R.string.ok)
				                       , new DialogInterface.OnClickListener() {
				                       	@Override
				                       	public void onClick(DialogInterface dialog, int which) {
				                       		completeDelivery(Status.PARTIALLY_COMPLETED);
				                       	}
				                       }
				                       , getString(android.R.string.cancel), null
				);
			}
			else {
				completeDelivery(Status.COMPLETED);
			}
			*/
        }
        delivery.loadLines(this);
        if (delivery.hasPlates && !delivery.isLinesPlatesScanCompleted()) {
            Message.showQuestion(R.string.unscanned_items_message, this, CONFIRM_ACCEPT_TAG);
        } else {
            goSignatureScreen();
        }
    }

    private void goSignatureScreen() {
        Business.from(this).signatureDelivery(getIntent().getLongExtra(BundleTrack.KEY_DELIVERY, -1));
        preferences = new TrackPreferences(this);
        preferences.setLastScreenBeforeSignatureImage(TrackPreferences.enumLastScreenBeforeSignature.DELIVERY.name());
        startActivity(new Intent(this, SignatureImageActivity.class));
    }

    /**
     * Sets the labels depending of the type delivery
     *
     * @param deliveryType type of delivery
     * @param quantity     quantity of items
     */
    private void setUIForDeliveryType(DeliveryType deliveryType, int quantity) {
        switch (deliveryType) {
            case DROPOFF:
                _acceptButton.setText(getText(R.string.acceptDelivery));
                setTitle(getString(R.string.invoicedetail));
                break;
            case PICKUP:
                _acceptButton.setText(getText(R.string.returnLabel));
                setTitle(getString(R.string.pickupDetail));
                break;
        }
        _acceptButton.setEnabled(quantity > 0);
    }


    /**
     * Displays unscheduled delivery/pickup options in screen menu and enable/disable Reorder Stops
     */

    private void enableOrDisableMenuOptions() {
        Log.i(TAG, "enableOrDisableMenuOptions");
        if (this.screenMenu == null) {
            return;
        }
        MenuItem menuItem = this.screenMenu.findItem(R.id.invoiceSelectScanMenuItem);
        menuItem.setVisible(isCameraVisible(preferences));

        MenuItem bluetoothMenuItem = this.screenMenu.findItem(R.id.bluetoothScannerMenuItem);
        bluetoothMenuItem.setVisible(isBluetoothScannerVisible(preferences));
        bluetoothMenuItem.setIcon(!Capture.get().getDevices().isEmpty() ? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_red);
    }

    /**
     * Indicates if the screen can show menu scan icon
     *
     * @param pref application preference
     * @return a Boolean
     */
    public boolean isCameraVisible(TrackPreferences pref) {
        return !_finished && pref.getScanner().isManualTrigger();
    }

    public boolean isBluetoothScannerVisible(TrackPreferences pref) {
        return !_finished && pref.getScanner().isBluetooth();
    }

    @Override
    public void onCaptureAPIData(String data) {
        if (handleScan(data, Scanner.SOCKET_MOBILE)) setListAdapter();
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
    protected void scannerSelected() {
        enableOrDisableMenuOptions();
        if (preferences.getScanner() == Scanner.SOCKET_MOBILE && !_finished) {
            setCaptureAPIListener(this);
        }
    }

    public boolean handleScan(String scan, Scanner scanner) {
        Plate plate = PlateRepository.fetchPlateWitScan(this, Preferences.getCurrentDeliveryKey(this), scan);
        if (plate != null) {
            if (!PlateRepository.updatePlateScan(this, plate)) {
                Preferences.setPendingMessage(getString(R.string.itemAlreadyScanned), this);
            } else {
                Preferences.clearPendingMessage(this);
            }
        } else {
            Line line = LineRepository.fetchLine(this, Preferences.getCurrentDeliveryKey(this), scan);
            if (line != null) {
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

    @Override
    public void onMessageOk(String tag) {
        if (tag.equals(CONFIRM_ACCEPT_TAG)) {
            goSignatureScreen();
        }
    }

    @Override
    public void onMessageCancel(String tag) {
    }
}