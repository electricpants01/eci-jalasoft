package com.jumptech.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jumptech.android.util.Message;
import com.jumptech.android.util.Preferences;
import com.jumptech.jumppod.R;
import com.jumptech.networking.JTRepository;
import com.jumptech.networking.responses.AuthResponse;
import com.jumptech.tracklib.data.Login;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.utils.CommonUtil;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.tracklib.utils.scan.ScanLauncher;

public class LoginActivity extends SimpleBaseActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private ProgressBar loginProgressBar;
    private Button loginButton;
    private EditText passwordTextView;
    private EditText usernameTextView;

    public LoginActivity() {
        super("LoginActivity");
        setOnNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                drawerLayout.closeDrawers();
                if (menuItem.getItemId() == R.id.loginAboutMenuItem) {
                    startActivity(new Intent(LoginActivity.this, SupportActivity.class));
                }
                return false;
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login_activity);
        Preferences.setIsDialogDenied(this, false);
        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView, R.menu.login_menu_container_view);
        }

        loginProgressBar = findViewById(R.id.loginProgressBar);
        loginButton = findViewById(R.id.loginButton);
        passwordTextView = findViewById(R.id.password);
        usernameTextView = findViewById(R.id.username);
        usernameTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        //if we have a known user don't allow it to be changed
        {
            String username = new TrackPreferences(this).getLogin();
            if (username != null) {
                usernameTextView.setText(username);
                usernameTextView.setEnabled(false);
                usernameTextView.setFocusable(false);
                ((TextView) findViewById(R.id.server)).setText(Preferences.baseUrl(this));
                updateServer(Preferences.isDefaultBaseUrl(this) ? View.INVISIBLE : View.VISIBLE);
            }
        }

        findViewById(R.id.loginButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CharSequence username = usernameTextView.getText();
                if (0 == username.length()) {
                    new AlertDialog.Builder(LoginActivity.this)
                            .setCancelable(false)
                            .setMessage(R.string.emptyUsername)
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show();
                    return;
                }
                CharSequence password = passwordTextView.getText();
                if (0 == password.length()) {
                    new AlertDialog.Builder(LoginActivity.this)
                            .setCancelable(false)
                            .setMessage(R.string.emptyPassword)
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .show();
                    return;
                }

                String baseUrl = ((TextView) findViewById(R.id.server)).getText().toString();
                Preferences.setBaseUrl(baseUrl, LoginActivity.this);
                authenticate(username.toString(), password.toString());
            }
        });

        findViewById(R.id.notACustomerLabel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, NewCustomerActivity.class));
            }
        });
        bindShowPasswordChanges((CheckBox) findViewById(R.id.chkShowPassword), passwordTextView);
        setTitle(R.string.app_empty_name);
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.loginmenu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String username = new TrackPreferences(this).getLogin();
        menu.findItem(R.id.reset).setVisible(((TextView) findViewById(R.id.server)).getText().length() > 1 && username == null);
        menu.findItem(R.id.invoiceSelectScanMenuItem).setEnabled(username == null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reset) {
            //clear names
            for (int id : new int[]{R.id.username, R.id.password, R.id.server}) {
                ((TextView) findViewById(id)).setText("");
            }
            updateServer(View.GONE);
            return true;
        } else if (item.getItemId() == R.id.invoiceSelectScanMenuItem) {
            String username = new TrackPreferences(this).getLogin();
            if (username == null) {
                if (PermissionUtil.hasPermission(this, Manifest.permission.CAMERA)) {
                    startCaptureActivity();
                } else {
                    PermissionUtil.requestPermission(this, Manifest.permission.CAMERA, PermissionUtil.REQUEST_CAMERA);
                }
            }
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    private void startCaptureActivity() {
        ScanLauncher.LaunchScanner(this, IntentIntegrator.QR_CODE_TYPES);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCaptureActivity();
                } else {
                    PermissionUtil.displayGrantPermissionMessage(LoginActivity.this, R.string.support_should_allow_camera_msg
                            , R.string.camera_access_required, R.string.open_settings, android.R.string.cancel);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("ndb", "onresult " + requestCode);

        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    Login loginData = Login.parse(result.getContents());
                    if (loginData.getUsername() != null) {
                        ((EditText) findViewById(R.id.username)).setText(loginData.getUsername());
                    }
                    if (loginData.getServer() != null && CommonUtil.isValidServerAddress(loginData.getServer())) {
                        ((TextView) findViewById(R.id.server)).setText(loginData.getServer());
                        updateServer(View.VISIBLE);
                    } else {
                        Message.showSimpleError(getString(R.string.server_address_invalid_url_msg), LoginActivity.this);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i("ndb", "not ok");
                }
                break;
            default:
                Log.i("ndb", "on default");
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void authenticate(final String username, final String password) {
        final TrackPreferences preferences = new TrackPreferences(this);
        loginButton.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.VISIBLE);
        passwordTextView.setEnabled(false);
        usernameTextView.setEnabled(false);
        hideKeyBoard();
        JTRepository.authenticate(username, password, this, new JTRepository.OnAuthResponse() {
            @Override
            public void success(AuthResponse authResponse) {
                loginProgressBar.setVisibility(View.GONE);
                loginButton.setVisibility(View.VISIBLE);
                preferences.setAuth(username, authResponse.getAuthorization());
                preferences.setIsEulaPrompt(authResponse.isEulaPrompt());
                preferences.set(authResponse.getConfig());
                continueAfterLogIn();
            }

            @Override
            public void error(String message) {
                loginProgressBar.setVisibility(View.GONE);
                loginButton.setVisibility(View.VISIBLE);
                passwordTextView.setEnabled(true);
                usernameTextView.setEnabled(true);
                preferences.clearAuthToken();
                Message.showSimpleError(message, LoginActivity.this);
            }
        });
    }

    private void continueAfterLogIn() {
        startActivity(new Intent(LoginActivity.this, LauncherActivity.class));
        finish();
    }

    private void updateServer(int visibility) {
        findViewById(R.id.serverPrompt).setVisibility(visibility);
    }

    /**
     * Handles show password events
     *
     * @param chkShowPassword  the CheckBox UI view element
     * @param passwordTextView the password EditText UI view element
     */
    private void bindShowPasswordChanges(final CheckBox chkShowPassword, final EditText passwordTextView) {
        chkShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    passwordTextView.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    passwordTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
                passwordTextView.setSelection(passwordTextView.getText().length());
            }
        });
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        toolbar.getBackground().setAlpha(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loginProgressBar.setVisibility(View.GONE);
    }
}