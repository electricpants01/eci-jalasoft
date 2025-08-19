package eci.technician.custom

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.adapters.CustomSearchItemAdapter
import eci.technician.models.order.Part
import kotlinx.android.synthetic.main.dialog_search_list.*

class CustomSearchPartDialog(val customContext: Context, val partList: MutableList<Part>, var listener: CustomDialogListener) : AlertDialog(customContext), CustomSearchItemAdapter.CustomSearchItemAdapterListener {

    interface CustomDialogListener {
        fun onSelectItem(item: Part)
    }

    override fun onItemTap(item: Part) {
        listener.onSelectItem(item)
        dismiss()
    }

    lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_search_list)
        recyclerView = listRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(customContext)
        recyclerView.adapter = CustomSearchItemAdapter(partList, this)
        recyclerView.setHasFixedSize(true)

        cancelDialog.setOnClickListener {
            dismiss()
        }
    }
}