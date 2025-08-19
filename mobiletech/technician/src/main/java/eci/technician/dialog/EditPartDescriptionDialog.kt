package eci.technician.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import eci.technician.R

object EditPartDescriptionDialog {

    private const val TAG: String = "EditPartDescription"
    private const val EXCEPTION: String = "Exception"

    fun showDialog(
        context: Context,
        partDescription: String,
        onConfirm: (newDescription: String) -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.edit_part_description_dialog, null)
        val newPartDescription =
            dialogView.findViewById<AppCompatEditText>(R.id.editTxtDescriptionDialog)
        newPartDescription.setText(partDescription)
        val saveButton = dialogView.findViewById<Button>(R.id.btnSaveDialog)
        val backButton = dialogView.findViewById<Button>(R.id.btnBackDialog)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        backButton.setOnClickListener {
            alertDialog.dismiss()
        }

        saveButton.setOnClickListener {
            onConfirm.invoke(newPartDescription.text.toString())
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
}