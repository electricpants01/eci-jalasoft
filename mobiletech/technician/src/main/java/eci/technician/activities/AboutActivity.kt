package eci.technician.activities

import android.content.Intent
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.BuildConfig
import eci.technician.R
import eci.technician.databinding.ActivityAboutBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants


class AboutActivity : AppCompatActivity() {
    lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.ABOUT_ACTIVITY)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        title = getString(R.string.help_title)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        binding.txtVersion.text = String.format("v.%s", BuildConfig.VERSION_NAME)
        if (BuildConfig.DEBUG || BuildConfig.FLAVOR == "beta") {
            val txtVersionName = String.format(
                "v.%s (%s) %s",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.BUILD_TYPE
            )
            binding.txtVersion.text = txtVersionName
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.aboutTextView.text = getString(R.string.about_message)
            binding.aboutTextView.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD;
        }

        binding.helpTextView.setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("http://webhelp.e-automate.com/MT/index.htm"))
            startActivity(browserIntent)
        }
        binding.openSourcesTextView.setOnClickListener {
            val intent = Intent(this, SourceActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
