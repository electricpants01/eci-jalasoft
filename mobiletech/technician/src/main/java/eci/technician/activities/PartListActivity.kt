package eci.technician.activities

import android.os.CountDownTimer
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection

abstract class PartListActivity : BaseActivity() {

    protected abstract fun searchParts()
    protected abstract fun setUpAdapter()
    protected abstract fun filterParts()
    protected abstract fun checkEmptyMessage()
    protected abstract fun loadDataFromSever()
    
    protected fun setUpBanner(bannerView: TextView) {
        if (AppAuth.getInstance().isConnected) {
            bannerView.visibility = View.GONE
        } else {
            bannerView.setText(R.string.offline_no_internet_connection)
            bannerView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOfflineDark))
            bannerView.visibility = View.VISIBLE
        }
        val connection = NetworkConnection(baseContext)
        connection.observe(this, { t ->
            t?.let {
                if (it) {
                    bannerView.setText(R.string.back_online)
                    bannerView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnline))
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) { /* not used */}
                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                            if (AppAuth.getInstance().isConnected) {
                                bannerView.visibility = View.GONE
                            }
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                    bannerView.setText(R.string.offline_no_internet_connection)
                    bannerView.setBackgroundColor(resources.getColor(R.color.colorOfflineDark))
                    bannerView.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}