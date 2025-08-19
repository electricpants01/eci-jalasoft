package com.jumptech.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.jumptech.jumppod.R;

/**
 * This class extends of AppCompatActivity standard class and allows setup UI with basic components
 *
 * @link{setOnNavigationItemSelectedListener} method should be called and provided with a proper
 * navigation listener, then call to @link{addNavigationFragment} method and setup a custom
 * menu content according the screen
 */
public abstract class SimpleBaseActivity extends AppCompatActivity {

    protected String mTag;
    protected Toolbar toolbar;
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerToggle;
    protected View viewStub;
    /**
     * Navigation view
     */
    protected NavigationView navView;
    private NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener;

    public SimpleBaseActivity(String tag) {
        this.mTag = tag;
    }

    public void setOnNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener) {
        this.onNavigationItemSelectedListener = onNavigationItemSelectedListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(R.layout.base_layout);
        ViewStub stub = (ViewStub) findViewById(R.id.body_container);
        stub.setLayoutResource(layoutResID);
        viewStub = stub.inflate();
    }

    protected View getViewStub() {
        return viewStub;
    }

    /**
     * Adds a Navigation Fragment to the activity
     *
     * @param toolbarId toolbar's resource ID
     * @param drawerLayoutId drawerLayout's resource ID
     * @param containerId container's resource ID
     * @param menu_container_view custom menu's resource ID
     */
    protected void addNavigationFragment(int toolbarId, int drawerLayoutId, int containerId, int menu_container_view) {
        toolbar = (Toolbar) findViewById(toolbarId);
        setSupportActionBar(toolbar);
        drawerLayout = (DrawerLayout) findViewById(drawerLayoutId);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name,
                R.string.app_name) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                hideKeyBoard();
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navView = (NavigationView) findViewById(R.id.navigationView);
        navView.inflateMenu(menu_container_view);
        navView.setNavigationItemSelectedListener(onNavigationItemSelectedListener);
        onConfigureNavigationMenu(toolbar, navView);
    }

    protected abstract void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView);

    /**
     * Sets the Navigation fragment visibility
     *
     * @param enabled if is true show the navigation else hide the navigation
     */
    protected void setNavigationFragmentEnabled(boolean enabled) {
        drawerToggle.setDrawerIndicatorEnabled(enabled);
        if (enabled) {
            getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void setToolbarTitle(String title) {
        if (toolbar != null) {
            if (!TextUtils.isEmpty(title)) {
                toolbar.setTitle(title);
                toolbar.setVisibility(View.VISIBLE);
            } else {
                toolbar.setVisibility(View.GONE);
            }
        }
    }

    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    public void closeDrawer() {
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    public boolean isDrawerOpened() {
        return drawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
