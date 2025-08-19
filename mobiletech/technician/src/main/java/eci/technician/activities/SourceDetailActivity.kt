package eci.technician.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import eci.technician.R
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.activity_source.toolbar
import kotlinx.android.synthetic.main.activity_source_detail.*

class SourceDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source_detail)

        val libraryTitle = intent.getStringExtra(Constants.EXTRA_LIBRARY_TITLE).toString()
        val libraryId = intent.getIntExtra(Constants.EXTRA_LIBRARY_RAW_ID, R.raw.license_signalr)
        setSupportActionBar(toolbar)
        title = libraryTitle
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
        }
        val license = resources.openRawResource(libraryId).readBytes().toString(Charsets.UTF_8)

        licenseContent.text = license
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
