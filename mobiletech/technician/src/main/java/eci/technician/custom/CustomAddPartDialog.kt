package eci.technician.custom

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import eci.technician.R
import eci.technician.databinding.DialogAddPartWithBinBinding
import eci.technician.helpers.DecimalsHelper


class CustomAddPartDialog(
    private val binName: String,
    private val binAvailableQuantity: Double,
    private val serialNumber: String,
    private val onPositiveClick: (quantity: Double) -> Unit,
    private val onNegativeClick: () -> Unit
) :
    DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activityFragment ->
            val builder = AlertDialog.Builder(activityFragment)
            val inflater = requireActivity().layoutInflater
            val binding = DialogAddPartWithBinBinding.inflate(inflater)
            binding.binText.text = binName
            binding.availableValue.text = DecimalsHelper.getValueFromDecimal(binAvailableQuantity)
            builder.setView(binding.root)
                .setPositiveButton(
                    R.string.save
                ) { _, _ ->
                    val quantity =
                        binding.txtQuantity.text?.toString()?.toDouble() ?: return@setPositiveButton
                    onPositiveClick.invoke(quantity)
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
                binding.txtQuantity.setSelection(binding.txtQuantity.text.length)

                if (serialNumber.isNotEmpty()){
                    binding.serialNumberField.visibility = View.VISIBLE
                    binding.serialNumberText.visibility = View.VISIBLE
                    binding.serialNumberText.text = serialNumber
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        binding.txtQuantity.focusable = View.NOT_FOCUSABLE
                    } else {
                        binding.txtQuantity.isEnabled = false
                    }
                }

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
                            positiveButton.isEnabled = shouldEnableButton(s, serialNumber)
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

    private fun shouldEnableButton(s: CharSequence?, serialNumber: String): Boolean {
        if (s?.toString().isNullOrEmpty()) return false
        val value = s?.toString()?.toDouble() ?: 0.0
        if (serialNumber.isNotEmpty()) return value == 1.0
        return value > 0
    }
}