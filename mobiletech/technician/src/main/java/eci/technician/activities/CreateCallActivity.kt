package eci.technician.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.fragments.createCall.CallTypesFragment
import eci.technician.fragments.createCall.CreateCallFragment
import eci.technician.fragments.createCall.CustomerLocationFragment
import eci.technician.fragments.createCall.SearchEquipmentFragment
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.CreateCallInterface
import eci.technician.workers.OfflineManager

class CreateCallActivity : BaseActivity(), CreateCallInterface {

    val connection by lazy {
        NetworkConnection(baseContext)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_call)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.CREATE_CALL_ACTIVITY)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            goToCreateCallFragment()
        }

        connection.observe(this, Observer { t ->
            t?.let {
                if (it) {
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {/*not used*/}
                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                }
            }
        })

    }

    override fun goToCreateCallFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val createCallFragment = CreateCallFragment()
            replace(R.id.fragmentToReplace, createCallFragment)
        }
    }

    override fun goToCallTypes() {
        supportFragmentManager.commit {
            val callTypesFragment = CallTypesFragment()
            setReorderingAllowed(true)
            replace(R.id.fragmentToReplace, callTypesFragment)
        }
    }

    override fun goToCustomerLocations() {
        supportFragmentManager.commit {
            val customerLocationFragment = CustomerLocationFragment()
            setReorderingAllowed(true)
            replace(R.id.fragmentToReplace, customerLocationFragment)
        }
    }

    override fun goToEquipmentSearch() {
        supportFragmentManager.commit {
            val searchEquipmentFragment = SearchEquipmentFragment()
            setReorderingAllowed(true)
            replace(R.id.fragmentToReplace, searchEquipmentFragment)
        }
    }

    override fun finishActivity() {
        finish()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }
}