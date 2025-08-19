package eci.technician.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.TwoItemLayoutBinding
import eci.technician.interfaces.IHoldCodesListener
import eci.technician.models.order.HoldCode

class HoldCodesListAdapter(private val holdCodesList:List<HoldCode>, val listener: IHoldCodesListener): RecyclerView.Adapter<HoldCodesListAdapter.HoldCodesViewHolder>() {

    class HoldCodesViewHolder(view: TwoItemLayoutBinding) : RecyclerView.ViewHolder(view.root) {
        val text1: TextView = view.text1
        val text2: TextView = view.text2
        val container: LinearLayout = view.twoItemsLayoutContainer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoldCodesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = TwoItemLayoutBinding.inflate(inflater, parent, false)
        return HoldCodesViewHolder(viewHolder)
    }

    override fun getItemCount(): Int {
        return holdCodesList.size
    }

    override fun onBindViewHolder(holder: HoldCodesViewHolder, position: Int) {
        val item: HoldCode = holdCodesList[position]
        holder.text1.text = item.onHoldCode
        holder.text2.text = item.description

        holder.container.setOnClickListener{
            listener.onHoldCodePressed(item)
        }
    }
}