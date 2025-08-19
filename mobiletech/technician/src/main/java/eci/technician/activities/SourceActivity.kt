package eci.technician.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import eci.technician.R
import eci.technician.databinding.ActivitySourceBinding
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.activity_source.*
import java.util.ArrayList

class SourceActivity : AppCompatActivity() {
    lateinit var binding:ActivitySourceBinding

    private enum class License(internal var title: String, internal var id: Int) {
        //License titles are not subject to translation
        SIGNATURE_PAD("SignaturePad 1.2.1", R.raw.license_signaturepad),
        SIGNALR("SignalR 1.0.0", R.raw.license_signalr),
        MATERIAL_COMPONENTS("Material Components 1.0.0", R.raw.license_material_components),
        FIREBASE_CRASHLYTICS("Firebase Crashlytics 18.0.0", R.raw.license_firebase_crashlytics),
        SQUAREUP_OKHTTP("Squareup okhttp 2.4.0 ", R.raw.license_okhttp),
        GSON("Google Gson 2.8.6", R.raw.license_gson),
        REALM("Realm Adapter 2.1.1", R.raw.license_realm),
        PICASSO("Picasso 2.5.2", R.raw.license_picasso),
        LIBPHONENUMBER("Libphonenumber 8.2.0", R.raw.license_libphonenumber),
        RETROFIT("Retrofit 2.7.0", R.raw.license_retrofit);

        companion object {
            val labels: List<String>
                get() {
                    val labels = ArrayList<String>()
                    for (license in values()) labels.add(license.title)
                    return labels
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySourceBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setSupportActionBar(toolbar)
        title = getString(R.string.source_licenses)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
        }

        binding.list.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, License.labels)
        binding.list.setOnItemClickListener { _, _, position, _ ->
            var licen = License.values()[position]
            var intent = Intent(this, SourceDetailActivity::class.java)
            intent.putExtra(Constants.EXTRA_LIBRARY_TITLE, licen.title)
            intent.putExtra(Constants.EXTRA_LIBRARY_RAW_ID, licen.id)
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
