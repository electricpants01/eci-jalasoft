package eci.technician.custom

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.adapters.CustomRepeatedListAdapter
import eci.technician.models.ClusterObject
import kotlinx.android.synthetic.main.dialog_search_list.*

class CustomRepeatedListDialog(val customContext: Context, val repeatedCalls: MutableList<ClusterObject>, var listener: CustomDialogListener) : AlertDialog(customContext), CustomRepeatedListAdapter.CustomRepeatedCallsListener {
    interface CustomDialogListener {
        fun onSelectItem(item: ClusterObject)
    }

    override fun onItemTap(item: ClusterObject) {
        listener.onSelectItem(item)
        dismiss()
    }

    lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_same_place_calls_list)
        recyclerView = listRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(customContext)
        recyclerView.adapter = CustomRepeatedListAdapter(repeatedCalls, this)
        recyclerView.setHasFixedSize(true)

        cancelDialog.setOnClickListener {
            dismiss()
        }
    }
}