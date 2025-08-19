package eci.technician.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.ShiftBinding
import eci.technician.models.time_cards.ShiftUI

class ShiftsAdapter(val listener: IShiftListener) :
    ListAdapter<ShiftUI, ShiftsAdapter.ShiftViewHolder>(SHIFT_COMPARATOR) {

    interface IShiftListener {
        fun onTapShift(shiftUI: ShiftUI)
    }

    class ShiftViewHolder(val binding: ShiftBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun create(viewGroup: ViewGroup): ShiftViewHolder {
                val binding =
                    ShiftBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
                return ShiftViewHolder(binding)
            }
        }
    }


    companion object {
        private val SHIFT_COMPARATOR = object : DiffUtil.ItemCallback<ShiftUI>() {
            override fun areItemsTheSame(oldItem: ShiftUI, newItem: ShiftUI): Boolean {
                return oldItem.shiftId == newItem.shiftId
            }

            override fun areContentsTheSame(oldItem: ShiftUI, newItem: ShiftUI): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftViewHolder {
        return ShiftViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ShiftViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.shift = item
        holder.binding.root.setOnClickListener {
            listener.onTapShift(item)
        }
    }
}
