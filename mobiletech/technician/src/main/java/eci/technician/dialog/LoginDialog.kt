package eci.technician.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import eci.technician.R
import eci.technician.databinding.DialogLoginBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.TextValidationHelper.isEmailValid
import eci.technician.workers.OfflineManager

class LoginDialog(val listener: ILoginListener) : BottomSheetDialogFragment() {
    lateinit var binding: DialogLoginBinding

    val viewModel: LoginViewModel by activityViewModels()

    interface ILoginListener {
        fun onDismissLoginDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtAccount.setText(AppAuth.getInstance().lastUser.account ?: "")
        binding.txtAccount.isEnabled = false
        binding.txtUsername.setText(AppAuth.getInstance().lastUser.username ?: "")
        binding.txtUsername.isEnabled = false
        binding.btnLogin.setOnClickListener {
            viewModel.performLogin(
                viewModel.createLoginPostModel(
                    binding.txtAccount.text.toString(),
                    binding.txtUsername.text.toString(),
                    binding.txtPassword.text.toString()
                )
            )
        }
        binding.txtPassword.doOnTextChanged { _, _, _, _ -> checkFields() }
        setUpObservers()
    }

    private fun checkFields() {
        val isAccountOk = binding.txtAccount.text.toString().isNotEmpty()
        val isUserNameOk = isEmailValid(binding.txtUsername.text.toString())
        val isPasswordOk = binding.txtPassword.text.toString().isNotEmpty()
        binding.btnLogin.isEnabled = isAccountOk && isUserNameOk && isPasswordOk
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener.onDismissLoginDialog()
    }

    private fun setUpObservers() {
        observeLoading()
        observeSuccessEvent()
        observeErrorEvent()
    }

    private fun observeErrorEvent() {
        viewModel.error.observe(viewLifecycleOwner) {
            it.getContent()?.let { pair ->
                val builderDialog = AlertDialog.Builder(requireContext())
                builderDialog.setTitle(getString(R.string.somethingWentWrong))
                builderDialog.setMessage(pair.second)
                builderDialog.setPositiveButton(getString(android.R.string.ok)) { _, _ -> }
                val errorDialog = builderDialog.create()
                errorDialog.setCancelable(false)
                errorDialog.show()
            }
        }
    }

    private fun observeSuccessEvent() {
        viewModel.isLoginSuccess.observe(viewLifecycleOwner) { value ->
            //Retry the action that was taken without a valid token
            OfflineManager.retryWorker(requireContext())
            value.getContentIfNotHandledOrReturnNull()?.let {
                if (it) {
                    (dialog as BottomSheetDialog).dismissWithAnimation = true
                    dialog?.dismiss()
                }
            }
        }
    }

    private fun observeLoading() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loaderIncluded.progressBarContainer.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener { dialogInterface ->

            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let {
                val behaviour = BottomSheetBehavior.from(parentLayout)
                setupFullHeight(parentLayout)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                behaviour.isDraggable = false
            }
        }

        return dialog
    }


    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    companion object {
        @JvmStatic
        fun newInstance(listener: ILoginListener): LoginDialog {
            val fragment = LoginDialog(listener)
            fragment.isCancelable = false
            return fragment
        }
    }
}