package eci.technician.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.adapters.MapsSettingsAdapter
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.activity_map_settings.*
import kotlinx.android.synthetic.main.activity_settings.toolbar


class MapSettingsActivity : AppCompatActivity(), MapsSettingsAdapter.MapsSettingsAdapterListener {
    private lateinit var mapList: MutableList<Triple<String, String, Drawable>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_settings)
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.MAP_SETTINGS_ACTIVITY)
        setSupportActionBar(toolbar)
        title = getString(R.string.navigation_app_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mapList = mutableListOf()

        mapsRecyclerView.layoutManager = LinearLayoutManager(this)
        mapsRecyclerView.setHasFixedSize(true)
        setUpRecycler()

    }

    private fun createMapList() {
        mapList = mutableListOf()
        val dummyIntentUri: Uri = Uri.parse("geo:")
        val mapIntent = Intent(Intent.ACTION_VIEW, dummyIntentUri)
        val list = packageManager.queryIntentActivities(
            mapIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        for (resolve in list) {
            val label = resolve.loadLabel(packageManager).toString()
            val packageName = resolve.activityInfo.packageName
            val icon = resolve.loadIcon(packageManager)
            mapList.add(Triple(label, packageName, icon))

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapItemTap(pair: Triple<String, String, Drawable>) {
        getSharedPreferences(Constants.PREFERENCE_NAVIGATION, Context.MODE_PRIVATE).edit()
            .putString(Constants.PREFERENCE_NAVIGATION_APP, pair.second).apply()
        setUpRecycler()
    }

    private fun setUpRecycler() {
        createMapList()
        mapsRecyclerView.adapter = MapsSettingsAdapter(mapList, this)
        (mapsRecyclerView.adapter as MapsSettingsAdapter).notifyDataSetChanged()
    }
}