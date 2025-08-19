package com.jumptech.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.navigation.NavigationView;
import com.jumptech.android.util.PermissionHelper;
import com.jumptech.android.util.Preferences;
import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.networking.JTRepository;
import com.jumptech.tracklib.data.Gdr;
import com.jumptech.tracklib.data.Gdr.Level;
import com.jumptech.tracklib.data.Gdr.Path;
import com.jumptech.tracklib.data.Route;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.utils.IntentHelper;
import com.jumptech.tracklib.utils.PermissionUtil;
import com.jumptech.ui.customDialogs.LocationDialog;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GdrActivity extends BaseActivity implements LocationDialog.LocationDialogListener {

    private static final String TAG = GdrActivity.class.getSimpleName();
    protected Gdr[] _gdrs = new Gdr[]{};

    /**
     * Stores the search view of the menu
     */
    protected SearchView searchView = null;

    private ListView gdrList;
    private ArrayAdapter<Gdr> _adapter;
    private volatile boolean ableToRequest = true;
    private Gdr gdrTemporal;
    private SwipeRefreshLayout swipeRefreshLayoutGdr;
    private TextView loadGdrTextView;
    private View loadingLayout;
    private TextView loadingTextView;
    private TextView noResultsTextView;
    private LocationDialog locationDialog;
    private boolean isPermissionDenied;
    private boolean wentToSettings = false;

    public GdrActivity() {
        super("GdrActivity");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gdr_activity);

        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView);
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ActivityUtil.showEnvironment(this);

        gdrList = (ListView) getViewStub().findViewById(R.id.GdrList);

        loadingLayout = findViewById(R.id.loadingPanel);
        loadingTextView = findViewById(R.id.loadingTextView);
        noResultsTextView = findViewById(R.id.noResultsTextView);
        swipeRefreshLayoutGdr = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

        loadGdrTextView = findViewById(R.id.loadGdrTextView);
        swipeRefreshLayoutGdr.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    swipeRefreshLayoutGdr.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    swipeRefreshLayoutGdr.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                Rect rect = new Rect();
                swipeRefreshLayoutGdr.getDrawingRect(rect);
                swipeRefreshLayoutGdr.setProgressViewOffset(false, 0, rect.centerY() - getResources().getDimensionPixelSize(R.dimen.swipe_padding) - (swipeRefreshLayoutGdr.getProgressCircleDiameter() / 2));
            }
        });

        swipeRefreshLayoutGdr.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                gdrList.setVisibility(View.GONE);
                searchView.setQuery("", true);
                if (_gdrs.length > 0) {
                    load(Path.SIBLING, _gdrs[0], true);
                }
            }
        });

        _adapter = new ArrayAdapter<Gdr>(this, R.layout.gdr_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup viewGroup) {
                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) GdrActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.gdr_item, viewGroup, false);
                }

                Gdr gdr = getItem(position);
                CardView colorView = view.findViewById(R.id.colorView);
                TextView nameTextView = view.findViewById(R.id.truckNameText);
                TextView timeTextView = view.findViewById(R.id.timeTextView);
                TextView ownerTextView = view.findViewById(R.id.ownerTextView);
                nameTextView.setText(gdr.getName());
                colorView.setVisibility(TextUtils.isEmpty(gdr.getColorHex()) ? View.GONE : View.VISIBLE);
                colorView.setCardBackgroundColor(gdr.isColorValid() ? Color.parseColor(gdr.getColorHex()) : ContextCompat.getColor(getContext(), android.R.color.black));
                nameTextView.setTypeface(null, gdr.getAssigned() != null && gdr.getAssigned() ? Typeface.BOLD : Typeface.NORMAL);
                String time = gdr.getDepartureDate() == null ? null : String.format("@%s", Util.formatTime(gdr.getDepartureDate()));
                timeTextView.setText(time);
                timeTextView.setVisibility(time == null ? View.GONE : View.VISIBLE);
                ownerTextView.setText(gdr.getOwnerLabel());
                ownerTextView.setVisibility(gdr.getOwnerLabel() == null ? View.GONE : View.VISIBLE);

                return view;
            }

            @Override
            public Filter getFilter() {
                return new Filter() {

                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        synchronized (_gdrs) {
                            if (constraint == null || constraint.length() == 0) {
                                results.values = Arrays.asList(_gdrs);
                                results.count = _gdrs.length;
                            } else {
                                List<Gdr> list = new ArrayList<Gdr>();
                                for (Gdr gdr : _gdrs) {
                                    if (StringUtils.contains(gdr.getName().toLowerCase(Locale.US), constraint.toString().toLowerCase(Locale.US)))
                                        list.add(gdr);
                                }
                                results.values = list;
                                results.count = list.size();
                            }
                        }

                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        _adapter.clear();
                        @SuppressWarnings("unchecked")
                        List<Gdr> gdrs = (List<Gdr>) results.values;
                        for (Gdr gdr : gdrs) _adapter.add(gdr);
                        _adapter.notifyDataSetChanged();
                    }

                };
            }
        };
        gdrList.setAdapter(_adapter);
        gdrList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (ableToRequest) {
                    ableToRequest = false;
                    Gdr gdr = _adapter.getItem(i);
                    if (gdr.getLevel()._hasDelivery) {
                        fetch(gdr);
                    } else {
                        load(Path.CHILD, gdr, false);
                    }
                    searchView.setQuery("", true);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean backgroundLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (backgroundLocationPermissionApproved && wentToSettings) {
            continueWithRoute();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Route route = RouteRepository.fetchRoute(this);
        if (route != null)
            load(Gdr.Path.SIBLING, new Gdr(Level.ROUTE, route._key, route._name), false);
        else loadRoot();
    }

    @Override
    public void onBackPressed() {
        if (_gdrs.length > 0) {
            load(Gdr.Path.PARENT, _gdrs[0], false);
        } else loadRoot();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtil.REQUEST_GPS: {
                dismissLocationDialog();
                continueWithRoute();
                return;
            }
            case PermissionUtil.REQUEST_TRACK_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        dismissLocationDialog();
                        requestBackgroundLocationPermission();
                    } else {
                        dismissLocationDialog();
                        continueWithRoute();
                    }
                } else {
                    isPermissionDenied = true;
                    dismissLocationDialog();
                    continueWithRoute();
                }
                return;
            }
            case PermissionUtil.REQUEST_TRACK_BACKGROUND_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dismissLocationDialog();
                } else {
                    isPermissionDenied = true;
                    dismissLocationDialog();
                    continueWithRoute();
                }
                return;
            }
            case PermissionUtil.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dismissLocationDialog();
                    continueWithRoute();
                } else {
                    isPermissionDenied = true;
                    dismissLocationDialog();
                    continueWithRoute();
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
                } else {
                    isPermissionDenied = true;
                }
                return;
            }

            default:
                continueWithRoute();
                return;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trucklistmenu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = configSearchMenu(searchItem);
        return true;
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        navView.getMenu().findItem(R.id.ExitRouteMenuItem).setVisible(false);
        navView.getMenu().findItem(R.id.selectDefaultMap).setVisible(false);
    }

    /**
     * Configures the state of the search menu in the tool bar
     *
     * @param searchItem search's menu item
     * @return {@link SearchView }
     */
    private SearchView configSearchMenu(MenuItem searchItem) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchViewAux =
                (SearchView) MenuItemCompat.getActionView(searchItem);
        searchViewAux.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        });
        searchViewAux.setQueryHint("Filter List");
        searchViewAux.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchViewAux.setMaxWidth(Integer.MAX_VALUE);
        return searchViewAux;
    }


    private void requestPermissions() {
        wentToSettings = true;

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


    private void requestForegroundLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PermissionUtil.REQUEST_TRACK_PERMISSION);
    }

    private void dismissLocationDialog() {
        if (locationDialog != null && locationDialog.getShowsDialog()) {
            locationDialog.dismiss();
        }
    }

    private void goToSettings() {
        wentToSettings = true;
        IntentHelper.goToSettings(GdrActivity.this);
    }


    private void requestBackgroundLocationPermission() {
        wentToSettings = true;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PermissionUtil.REQUEST_TRACK_BACKGROUND_PERMISSION);
    }


    public void filter(String query) {
        query = query.trim();
        Log.e("onQueryTextChange", "query " + query);
        if (query.isEmpty()) {
            _adapter.getFilter().filter("");
            noResultsTextView.setVisibility(View.GONE);
            gdrList.setVisibility(View.VISIBLE);
        } else {
            _adapter.getFilter().filter(query, new Filter.FilterListener() {
                @Override
                public void onFilterComplete(int i) {
                    if (_adapter.isEmpty()) {
                        gdrList.setVisibility(View.GONE);
                        noResultsTextView.setVisibility(View.VISIBLE);
                    } else {
                        gdrList.setVisibility(View.VISIBLE);
                        noResultsTextView.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.truckListRefreshMenuItem) {
            searchView.setQuery("", true);
            if (_gdrs.length > 0) {
                load(Path.SIBLING, _gdrs[0], false);
            } else {
                loadRoot();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void loadRoot() {
        load(Gdr.Path.CHILD, new Gdr(), false);
    }

    protected void load(final Gdr.Path path, final Gdr gdr, Boolean isRefresh) {
        if (!isRefresh) {
            loadingTextView.setText(R.string.loadTruckList);
            loadingLayout.setVisibility(View.VISIBLE);
        }
        loadGdrTextView.setText(R.string.loadTruckList);
        loadGdrTextView.setVisibility(View.VISIBLE);
        JTRepository.loadList(gdr, path, this, new JTRepository.OnGdrResponse() {
            @Override
            public void success(Gdr[] gdrs) {
                dismissProgressDialog();
                gdrList.setVisibility(View.VISIBLE);
                loadGdrTextView.setVisibility(View.GONE);
                swipeRefreshLayoutGdr.setRefreshing(false);
                updateList(gdrs);
            }

            @Override
            public void error(String message) {
                dismissProgressDialog();
                swipeRefreshLayoutGdr.setRefreshing(false);
                loadGdrTextView.setVisibility(View.GONE);
                gdrList.setVisibility(View.VISIBLE);
                checkLogin();
                updateList(null);
            }
        });
    }

    private void updateList(Gdr[] gdrs) {
        _gdrs = gdrs == null ? new Gdr[]{} : gdrs;
        _adapter.clear();
        _adapter.addAll(_gdrs);

        if (_gdrs.length > 0) {
            Gdr gdr = _gdrs[0];
            setTitle(gdr.getLevel()._title);
        }
        _adapter.notifyDataSetChanged();
        ableToRequest = true;
    }

    protected void fetch(final Gdr gdr) {
        loadingTextView.setText(R.string.fetchRouteNoWait);
        loadingLayout.setVisibility(View.VISIBLE);
        JTRepository.route(gdr, this, new JTRepository.OnServiceResponse() {


            @Override
            public void success() {
                dismissProgressDialog();
                loadingLayout.setVisibility(View.GONE);
                gdrTemporal = gdr;
                if (new TrackPreferences(GdrActivity.this).getGpsEnabled()) {
                    if (PermissionUtil.hasPermission(GdrActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        continueWithRoute();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            showLocationPermissionSettingsAlert(true);
                        } else {
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                showLocationPermissionSettingsAlert(true);
                            } else {
                                showLocationPermissionSettingsAlert(false);
                            }
                        }
                    }
                } else {
                    continueWithRoute();
                }
            }

            @Override
            public void error(String message) {
                dismissProgressDialog();
                loadingLayout.setVisibility(View.GONE);
                checkLogin();
                new AlertDialog.Builder(GdrActivity.this)
                        .setCancelable(false)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ableToRequest = true;
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private void showLocationPermissionSettingsAlert(boolean isAndroidQAndAbove) {
        if (locationDialog != null && locationDialog.getShowsDialog()) {
            locationDialog.dismiss();
        }
        locationDialog = new LocationDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean(LocationDialog.IS_ANDROID_Q_AND_ABOVE, isAndroidQAndAbove);
        locationDialog.setArguments(bundle);
        locationDialog.setCancelable(false);
        locationDialog.show(getSupportFragmentManager(), "LocationDialogOnGdrActivity");
    }

    private void continueWithRoute() {
        RouteRepository.clearAndInsertRoute(GdrActivity.this, gdrTemporal.getKey(), gdrTemporal.getName());
        startActivity(new Intent(GdrActivity.this, RouteActivity.class));
        finish();
    }

    private void checkLogin() {
        ActivityUtil.checkLoginSession(GdrActivity.this);
    }

    private void dismissProgressDialog() {
        loadingLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
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
        continueWithRoute();
    }
}
