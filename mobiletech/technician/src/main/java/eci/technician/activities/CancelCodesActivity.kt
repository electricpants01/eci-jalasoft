package eci.technician.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import eci.technician.R
import eci.technician.adapters.CancelCodesAdapter
import eci.technician.models.order.CancelCode
import eci.technician.repository.DatabaseRepository
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.activity_cancel_codes.*


class CancelCodesActivity : AppCompatActivity(), CancelCodesAdapter.CancelCodeListener {

    private var currentCancelCode: CancelCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_codes)
        setSupportActionBar(toolbar)
        title = getString(R.string.cancel_code_list_title)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        cancelCodeRecyclerView.layoutManager = LinearLayoutManager(this)
        cancelCodeRecyclerView.setHasFixedSize(true)
        cancelCodeRecyclerView.adapter = CancelCodesAdapter(DatabaseRepository.getInstance().cancelCodes, this)
    }

    override fun updateCancelCode(item: CancelCode?) {
        val bundle = Bundle()
        bundle.putString("CancelCode", item?.code)
        FirebaseAnalytics.getInstance(baseContext).logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)
        this.currentCancelCode = item
        if (currentCancelCode != null) {
            currentCancelCode?.let {
                val returnIntent = Intent()
                returnIntent.putExtra(Constants.EXTRA_CANCEL_CODE_ID, it.cancelCodeId)
                returnIntent.putExtra(Constants.EXTRA_CANCEL_CODE_TITLE, it.code)
                returnIntent.putExtra(Constants.EXTRA_CANCEL_CODE_DESCRIPTION, it.description)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
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
}