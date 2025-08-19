package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView

import eci.technician.databinding.AssistantItemBinding
import eci.technician.models.TechnicianItem

class TechniciansAdapter(private val items: List<TechnicianItem>, private val listener: TechnicianClickListener) : RecyclerView.Adapter<TechniciansAdapter.ViewHolder>() {
    private var query: String = ""
    private var filteredTechnicians: List<TechnicianItem> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AssistantItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredTechnicians[position]
        holder.binding?.item = item
    }

    override fun getItemCount(): Int {
        return filteredTechnicians.size
    }

    fun setQuery(query: String) {
        this.query = query.trim()
        if (query.isBlank()) {
            this.filteredTechnicians = items
        } else {
            this.filteredTechnicians = this.items.filter {
                it.fullName.contains(query, true) ||
                        it.code.contains(query, true)
            }
        }
        notifyDataSetChanged()
    }

    interface TechnicianClickListener {
        fun onItemClick(technician: TechnicianItem)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: AssistantItemBinding? = DataBindingUtil.bind(itemView)

        init {
            itemView.setOnClickListener { listener.onItemClick(filteredTechnicians[adapterPosition]) }
        }
    }
}
