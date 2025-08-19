package eci.technician.custom

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import eci.technician.R
import eci.technician.databinding.DialogUsePartBinding
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants

class CustomPartUseDialog(
    private val availableQuantity: Double,
    private val description: String,
    private val canShowDescription: Boolean,
    private val onPositiveClick: (quantity: Double, description: String) -> Unit,
    private val onNegativeClick: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activityFragment ->
            val builder = AlertDialog.Builder(activityFragment)
            val inflater = requireActivity().layoutInflater
            val binding = DialogUsePartBinding.inflate(inflater)
            binding.txtAvailable.text = availableQuantity.toString()
            binding.containerAvailableQuantity.visibility = View.GONE
            binding.partDescriptionTxt.visibility =
                if (canShowDescription) View.VISIBLE else View.GONE
            binding.editTxtDescription.visibility =
                if (canShowDescription) View.VISIBLE else View.GONE
            binding.editTxtDescription.setText(description)
            builder.setView(binding.root)
                .setPositiveButton(
                    "save"
                ) { _, _ ->
                    FBAnalyticsConstants.logEvent(requireContext(),FBAnalyticsConstants.NeededPartsFragment.ADD_NEEDED_PART_ACTION)
                    val quantity =
                        binding.txtQuantity.text?.toString()?.toDouble() ?: return@setPositiveButton
                    val desc = binding.editTxtDescription.text.toString()
                    onPositiveClick.invoke(quantity, desc)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    onNegativeClick.invoke()
                    dialog.dismiss()
                }
            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.window?.let { window ->
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    binding.txtQuantity.isFocusable = true
                }
                binding.txtQuantity.isFocusable = true
                binding.txtQuantity.requestFocus()
                binding.txtQuantity.setSelection(binding.txtQuantity.text?.length ?: 1)

                binding.txtQuantity.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        //do nothing
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        dialog.getButton(Dialog.BUTTON_POSITIVE)?.let { positiveButton ->

                            positiveButton.isEnabled = shouldEnableButton(s, availableQuantity)
                            if (!positiveButton.isEnabled) {
                                positiveButton.setTextColor(
                                    ContextCompat.getColor(
                                        activityFragment.baseContext,
                                        R.color.disabled_button
                                    )
                                )
                            } else {
                                positiveButton.setTextColor(
                                    ContextCompat.getColor(
                                        activityFragment.baseContext,
                                        R.color.colorAccent
                                    )
                                )
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        //do nothing
                    }
                })
            }
            dialog

        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun shouldEnableButton(s: CharSequence?, availableQuantity: Double): Boolean {
        if (s?.toString().isNullOrEmpty()) return false
        val value = s?.toString()?.toDouble() ?: 0.0
        return value > 0
    }
}