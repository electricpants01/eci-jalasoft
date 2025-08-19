package eci.technician.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eci.technician.BaseActivity
import eci.technician.MainActivity
import eci.technician.R
import eci.technician.databinding.ActivityUserBinding
import eci.technician.dialog.LoginViewModel
import eci.technician.helpers.AppAuth
import eci.technician.helpers.DialogHelperManager.displayOkMessage
import eci.technician.helpers.TextValidationHelper.isEmailValid
import eci.technician.helpers.api.retroapi.RetrofitApiHelper.setApiToNull
import eci.technician.helpers.versionManager.CompatibilityManager
import eci.technician.helpers.versionManager.MessageDisplayer.displayMessage
import eci.technician.models.LastUserModel
import eci.technician.service.ChatService
import eci.technician.service.TechnicianService
import eci.technician.tools.Constants
import eci.technician.tools.PermissionHelper.verifyLocationBackgroundPermissions


class UserActivity : BaseActivity() {
    private lateinit var binding: ActivityUserBinding
    private lateinit var alertDialog: AlertDialog
    private val viewModel: LoginViewModel by viewModels()

    companion object {
        private const val TAG = "UserActivity"
        private const val EXCEPTION = "Exception"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user)
        AppAuth.getInstance().locationDeniedSelected = false

        binding.btnLogin.isEnabled = false
        setSupportActionBar(binding.toolbar)
        if (AppAuth.getInstance().isLoggedIn) {
            finish()
        }
        binding.btnLogin.setOnClickListener { login2() }
        binding.txtUsername.doAfterTextChanged { binding.btnLogin.isEnabled = allFieldsOk() }
        binding.txtAccount.doAfterTextChanged { binding.btnLogin.isEnabled = allFieldsOk() }
        binding.txtPassword.doAfterTextChanged { binding.btnLogin.isEnabled = allFieldsOk() }
        val lastUserModel = AppAuth.getInstance().lastUser
        setInitialFields(lastUserModel)
        setLicenseDialog()
        stopChatService()
        stopTechnicianService()
        setApiToNull()
        setObservers()
    }

    private fun setObservers(){
        observeLicenseSuccess()
        observeLoginSuccess()
        observeLoading()
        observeError()
        observeLicenseHandledError()
        observeLoginSystemHandledError()
    }

    private fun observeLoginSystemHandledError() {
        viewModel.handledLoginSystemError.observe(this) {
            it?.let {
                showMessageBox(
                    getString(R.string.invalid_credentials),
                    getString(R.string.invalid_credentials_message)
                )
            }
        }
    }

    private fun observeLicenseHandledError() {
        viewModel.handledLicenseError.observe(this) {
            it?.let {
                val account = binding.txtAccount.text.toString()
                showMessageBox(
                    getString(R.string.invalid_account),
                    getString(R.string.invalidDealer, account)
                )
            }
        }
    }

    private fun observeError() {
        viewModel.error.observe(this) {
            it?.getContent()?.let { pair ->
                showNetworkErrorDialog(pair, this, supportFragmentManager)
            }
        }
    }

    private fun observeLoading() {
        viewModel.loading.observe(this) { showLoading ->
            binding.loaderIncluded.progressBarContainer.visibility =
                if (showLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !showLoading
        }
    }

    private fun observeLoginSuccess() {
        viewModel.isLoginSuccess.observe(this) {
            it?.getContent()?.let { success ->
                if (success) {
                    openNextView()
                }
            }
        }
    }

    private fun observeLicenseSuccess() {
        viewModel.successLicense.observe(this) {
            it?.getContent()?.let { success ->
                if (success) {
                    checkCompatibility()
                }
            }
        }
    }

    private fun setInitialFields(lastUserModel: LastUserModel?) {
        binding.txtAccount.setText(lastUserModel?.account ?: "")
        if (lastUserModel?.isSave == true) {
            binding.txtUsername.setText(lastUserModel.username)
            binding.chkSavePassword.isChecked = true
        }
    }

    private fun setLicenseDialog() {
        alertDialog = AlertDialog.Builder(this)
            .setPositiveButton(R.string.accept) { _, _ ->
                AppAuth.getInstance().acceptLicenseAgreement()
            }
            .setNegativeButton(R.string.reject) { _, _ -> finish() }
            .setNeutralButton(R.string.read) { _, _ -> openLicensePage() }
            .setTitle(R.string.license)
            .setMessage(R.string.accept_license)
            .setCancelable(false)
            .create()
    }

    private fun stopTechnicianService() {
        try {
            val technicianService = Intent(this, TechnicianService::class.java)
            stopService(technicianService)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun stopChatService() {
        try {
            val chatServiceIntent = Intent(this, ChatService::class.java)
            stopService(chatServiceIntent)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun allFieldsOk(): Boolean {
        val account: Boolean = binding.txtAccount.text.toString().trim().isNotEmpty()
        val email = isEmailValid(binding.txtUsername.text.toString().trim())
        val password: Boolean = binding.txtPassword.text.toString().trim().isNotEmpty()
        return account && email && password
    }

    private fun openLicensePage() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.OPEN_LICENCE_URI))
        startActivity(browserIntent)
    }

    private fun checkAgreement() {
        if (!AppAuth.getInstance().checkLicenseAgreement()) {
            showAgreementDialog()
        }
    }

    private fun showAgreementDialog() {
        alertDialog.show()
    }

    override fun onPause() {
        alertDialog.dismiss()
        super.onPause()
    }

    override fun onResume() {
        checkAgreement()
        super.onResume()
    }

    private fun login2() {
        if (AppAuth.getInstance().isLoggedIn) { // for the case we don't update the app in the playStore on UPDATE button
            checkCompatibility()
        } else {
            val lastUserModel = LastUserModel(
                binding.txtAccount.text.toString(),
                binding.txtUsername.text.toString(),
                binding.txtPassword.text.toString(),
                binding.chkSavePassword.isChecked
            )
            AppAuth.getInstance().lastUser = lastUserModel
            viewModel.loginLicense(
                account = binding.txtAccount.text.toString()
            )
        }
    }

    private fun openMainActivity() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun checkCompatibility() {
        val compatibilityManager = CompatibilityManager(applicationContext)
        compatibilityManager.checkCompatibility(
            lifecycle,
            { compatibilityMessage: String ->
                viewModel.compatibilityLoader(false)
                resolveCompatibility(compatibilityMessage)
            },
            { title: String, message: String ->
                viewModel.compatibilityLoader(false)
                onError(title, message)
            }, {
                viewModel.compatibilityLoader(true)
            })
    }

    private fun onError(title: String, message: String) {
        if (message == getString(R.string.unknown_host_version)) displayOkMessage(
            title,
            message,
            getString(R.string.ok),
            this
        ) {
            viewModel.performLoginToSystem(
                binding.txtAccount.text.toString(),
                binding.txtUsername.text.toString(),
                binding.txtPassword.text.toString()
            )
        } else displayOkMessage(
            title,
            message,
            getString(R.string.ok),
            this
        ) { }
    }

    private fun openNextView() {
        if (verifyLocationBackgroundPermissions(baseContext)) {
            openMainActivity()
        } else {
            startActivity(Intent(baseContext, LocationPermissionActivity::class.java))
            finish()
        }
    }

    private fun resolveCompatibility(compatibilityMessage: String) {
        if (compatibilityMessage == CompatibilityManager.COMPATIBILITY_OK) {
            viewModel.performLoginToSystem(
                binding.txtAccount.text.toString(),
                binding.txtUsername.text.toString(),
                binding.txtPassword.text.toString()
            )
            return
        }
        if (compatibilityMessage == CompatibilityManager.MESSAGE_OLD_MOBILE) {
            displayMessage(
                compatibilityMessage,
                this
            ) { }
            return
        }
        if (compatibilityMessage == CompatibilityManager.MESSAGE_OLD_HOST ||
            compatibilityMessage == CompatibilityManager.MESSAGE_UNKNOWN_HOST
        ) {
            displayOkMessage(
                this.getString(R.string.warning),
                compatibilityMessage,
                this.getString(R.string.ok),
                this
            ) {
                viewModel.performLoginToSystem(
                    binding.txtAccount.text.toString(),
                    binding.txtUsername.text.toString(),
                    binding.txtPassword.text.toString()
                )
            }
            return
        }
        return
    }
}