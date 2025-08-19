package eci.technician.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import eci.technician.MainActivity
import eci.technician.databinding.ActivityLocationPermissionBinding
import eci.technician.helpers.AppAuth
import eci.technician.tools.PermissionHelper.verifyLocationBackgroundPermissions
import eci.technician.tools.PermissionHelper.verifyTrackPermissions


class LocationPermissionActivity : AppCompatActivity() {

    lateinit var binding: ActivityLocationPermissionBinding

    private var wentToSettings = false
    private var isPermissionRequested = false

    companion object {
        const val TAG = "LocationPermissionActivity"
        const val EXCEPTION = "Exception"
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestFineBackgroundPermissions()
                } else {
                    goToMainActivity()
                }
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                goToMainActivity()
            }

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val shouldRequest =
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    if (shouldRequest) {
                        goToSettings()
                    } else {
                        goToMainActivity()
                    }
                } else {
                    val shouldRequest =
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (shouldRequest) {
                        goToSettings()
                    } else {
                        goToMainActivity()
                    }
                }
            }
        }
    }

    private val locationFineAndBackgroundRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionRequested = true
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                goToMainActivity()
            }

            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                val shouldRequest =
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                if (shouldRequest) {
                    goToSettings()
                } else {
                    goToMainActivity()
                }
            }

            else -> {
                Log.d(TAG, "Will got to mainActivity")
                goToMainActivity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPermissionBinding.inflate(layoutInflater)
        val view = binding.root
        if (AppAuth.getInstance().isLoggedIn) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    if (verifyLocationBackgroundPermissions(this) ||
                        AppAuth.getInstance().locationDeniedSelected ||
                        isPermissionRequested
                    ) {
                        goToMainActivity()
                    }
                }
                else -> {
                    if (verifyTrackPermissions(this) ||
                        AppAuth.getInstance().locationDeniedSelected ||
                        isPermissionRequested
                    ) {
                        goToMainActivity()
                    }
                }
            }
        } else {
            openLoginActivity()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            binding.allowAllTheTimeTextView.visibility = View.GONE
        }
        if (Build.VERSION.SDK_INT >= 31) {
            binding.allowAllTheTimeTextView.visibility = View.GONE
            binding.allowAllTheTimeTextViewAndroid12.visibility = View.VISIBLE
        }
        binding.acceptLocationAccessButton.setOnClickListener {
            requestLocationPermissions()
        }

        binding.noThanksButton.setOnClickListener {
            AppAuth.getInstance().locationDeniedSelected = true
            goToMainActivity()
        }

        setContentView(view)

    }

    private fun requestLocationPermissions() {

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestFineBackgroundPermissions()
                }
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                requestFineCoarsePermissions()
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                requestFineBackgroundPermissions()
            }
            else -> {
                requestFineCoarsePermissions()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestFineBackgroundPermissions() {
        locationFineAndBackgroundRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
    }

    private fun requestFineCoarsePermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    private fun openLoginActivity() {
        startActivity(Intent(this, UserActivity::class.java))
        finish()
    }


    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun goToSettings() {
        wentToSettings = true
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            goToMainActivity()
        }
    }

    override fun onBackPressed() {
        return
    }
}