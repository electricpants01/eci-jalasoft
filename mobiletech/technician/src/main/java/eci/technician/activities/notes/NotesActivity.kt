package eci.technician.activities.notes

import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivityNotesBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.api.retroapi.RetrofitRepository
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.interfaces.INotesNavigation
import eci.technician.workers.OfflineManager
import kotlinx.coroutines.launch

class NotesActivity : BaseActivity(), INotesNavigation {

    companion object {
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALL_NUMBER_CODE = "extra_call_number_code"
    }

    lateinit var binding: ActivityNotesBinding
    val viewModel: ServiceCallNotesViewModel by viewModels()
    val connection by lazy {
        NetworkConnection(baseContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.NOTES_ACTIVITY)
        viewModel.callId = intent.getIntExtra(EXTRA_CALL_ID, 0)
        viewModel.callNumberCode = intent.getStringExtra(EXTRA_CALL_NUMBER_CODE) ?: ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            goToAllNotesFragment(true)
        }

        lifecycleScope.launch {
            RetrofitRepository.RetrofitRepositoryObject.getInstance()
                .getServiceCallNotesTypes(this@NotesActivity)
        }

        connection.observe(this, { t ->
            t?.let {
                if (it) {
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {/*not used*/
                        }

                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                            OfflineManager.retryNotesWorker(baseContext)
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                }
            }
        })
    }

    override fun goToAllNotesFragment(showLoadingError: Boolean) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle()
            bundle.putBoolean(AllNotesFragment.EXTRA_SHOW_LOADING_ERROR, showLoadingError)
            replace(R.id.fragmentToReplace, AllNotesFragment::class.java, bundle)
        }
    }

    override fun goToCreateNoteFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle()
            bundle.putBoolean(NoteDetailFragment.EXTRA_IS_CREATING_NOTE, true)
            replace(R.id.fragmentToReplace, NoteDetailFragment::class.java, bundle)
        }
    }

    override fun goToEditNoteFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val bundle = Bundle()
            bundle.putBoolean(NoteDetailFragment.EXTRA_IS_EDITING_NOTE, true)
            replace(R.id.fragmentToReplace, NoteDetailFragment::class.java, bundle)
        }
    }
}