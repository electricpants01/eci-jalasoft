package eci.technician.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.EquipmentHistoryItemBinding
import eci.technician.models.EquipmentHistoryModel

class EquipmentHistoryAdapter() : RecyclerView.Adapter<EquipmentHistoryAdapter.EquipmentHistoryViewHolder>() {
    var elements: MutableList<EquipmentHistoryModel> = mutableListOf()
    var filteredData: MutableList<EquipmentHistoryModel> = mutableListOf()
    var filterType = 0

    companion object {
        const val FULL_INFO = 0
        const val USED_PARTS = 1
        const val CALL_REMARKS = 2
    }

    lateinit var contex: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentHistoryViewHolder {
        contex = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val equipmentHistoryItemBinding = EquipmentHistoryItemBinding.inflate(inflater, parent, false)
        return EquipmentHistoryViewHolder(equipmentHistoryItemBinding)
    }

    override fun onBindViewHolder(holder: EquipmentHistoryViewHolder, position: Int) {
        val item = filteredData[position]
        holder.binding.item = item
        holder.binding.filterType = filterType
        holder.binding.equipmentProblemCodes.text = item.getEquipmentProblemCodes(contex)
        holder.binding.equipmentRepairCodes.text = item.getEquipmentRepairCodes(contex)
        holder.binding.equipmentMeters.text = item.getEquipmentMeters(contex)
        holder.binding.equipmentParts.text = item.getEquipmentParts(contex)
    }

    override fun getItemCount(): Int {
        return filteredData.size
    }

    fun setElementsList(elements: MutableList<EquipmentHistoryModel>) {
        this.elements = elements
        filterData()
    }

    fun setFilterTypeAdapter(filterType: Int) {
        this.filterType = filterType
        filterData()
    }

    private fun filterData() {
        filteredData.clear()
        for (element in elements) {
            when (filterType) {
                FULL_INFO -> {
                    filteredData.add(element)
                }
                USED_PARTS -> {
                    if (!element.parts.isNullOrEmpty()) filteredData.add(element)
                }
                CALL_REMARKS -> {
                    if (!element.callNotes.isNullOrEmpty()) filteredData.add(element)
                }
            }
        }
        notifyDataSetChanged()
    }

    class EquipmentHistoryViewHolder(v: EquipmentHistoryItemBinding) : RecyclerView.ViewHolder(v.root) {
        val binding: EquipmentHistoryItemBinding = v
    }
}