package eci.technician.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivitySettingsBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.tools.Constants

class SettingsActivity : BaseActivity(), View.OnClickListener {

    companion object{
        const val TAG = "SettingsActivity"
        const val EXCEPTION = "Exception"
    }

    lateinit var binding:ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.SETTINGS_ACTIVITY)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        title = getString(R.string.settings_help_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.containerSettingsMapRow.setOnClickListener(this)
        binding.containerSettingsChangePasswordRow.setOnClickListener(this)
        binding.containerSettingsHelpRow.setOnClickListener(this)
        updateSelectedMap()
    }

    private fun updateSelectedMap() {
        val navigationAppName = getSharedPreferences(Constants.PREFERENCE_NAVIGATION, Context.MODE_PRIVATE)
            .getString(Constants.PREFERENCE_NAVIGATION_APP, "")
        navigationAppName?.let {
            try {
                val packageManager = applicationContext.packageManager
                val appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, PackageManager.GET_META_DATA))
                binding.mapSelectedTextView.text = appName
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelectedMap()
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

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.containerSettingsMapRow -> {
                openMapsList()
            }
            R.id.containerSettingsChangePasswordRow -> {
                openChangePassword()
            }
            R.id.containerSettingsHelpRow -> {
                openHelpWebPage()
            }
        }
    }

    private fun openChangePassword() {
        showMessageBox("", getString(R.string.change_password_dialog))
    }

    private fun openHelpWebPage() {
        val aboutIntent = Intent(this, AboutActivity::class.java)
        startActivity(aboutIntent)
    }

    private fun openMapsList() {
        startActivity(Intent(this, MapSettingsActivity::class.java))
    }
}