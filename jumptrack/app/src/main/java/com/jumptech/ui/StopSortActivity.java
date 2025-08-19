package com.jumptech.ui;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Stop;
import com.jumptech.tracklib.db.Business;
import com.jumptech.tracklib.room.TrackDB;
import com.jumptech.tracklib.repository.StopRepository;
import com.jumptech.ui.adapters.DividerItemDecoration;
import com.jumptech.ui.adapters.StopSortRecyclerViewAdapter;
import com.jumptech.ui.adapters.ViewTouchHelper;
import com.jumptech.tracklib.utils.factories.AsyncProgressTaskFactory;

import java.util.ArrayList;
import java.util.List;

public class StopSortActivity extends SimpleBaseActivity {
    private List<Stop> _stops = new ArrayList<>();

    private StopSortRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private ItemTouchHelper helper;

    public StopSortActivity() {
        super("StopSortActivity");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_sort_activity);

        Fragment fragmentNavigation = getSupportFragmentManager().findFragmentById(R.id.navigationView);
        if (fragmentNavigation == null) {
            addNavigationFragment(R.id.toolbar, R.id.drawerLayout, R.id.navigationView, R.menu.no_menu_container_view);
        }
        super.setNavigationFragmentEnabled(false);

        //load stops
        {
            Cursor cursor = null;
            try {
                cursor = StopRepository.inTransitStops(this);
                while (cursor.moveToNext()) {
                    _stops.add(StopRepository.stop(cursor));
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        adapter = new StopSortRecyclerViewAdapter(StopSortActivity.this.getApplicationContext(), _stops);

        findViewById(R.id.edit_done_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                StopRepository.routeOrder(StopSortActivity.this, _stops);
                AsyncProgressTaskFactory.createSyncQuietTask(Business.from(StopSortActivity.this)).execute();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

        findViewById(R.id.edit_cancel_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        ItemTouchHelper.Callback callback = new ViewTouchHelper(adapter);
        helper = new ItemTouchHelper(callback);
        adapter.setItemTouchHelper(helper);

        recyclerView = findViewById(R.id.stopList);
        recyclerView.setLayoutManager(new LinearLayoutManager(StopSortActivity.this.getApplicationContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.list_divider));
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        helper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onConfigureNavigationMenu(Toolbar toolbar, NavigationView navView) {
        //No navigation menu is needed for this screen
    }
}
