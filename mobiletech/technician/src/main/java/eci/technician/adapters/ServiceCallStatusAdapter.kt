package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.databinding.ContainerOneItemLayoutSelectableBinding

class ServiceCallStatusAdapter(private val statusList: List<Int>,
                               val listener: IServiceCallStatus):
    RecyclerView.Adapter<ServiceCallStatusAdapter.ServiceCallStatusHolder>() {

    var itemSelected = -1
    var showCheckMark: Boolean = false

    fun setItemSelectedStatus(rowSelected: Int, showCheckMark: Boolean){
        this.itemSelected = rowSelected
        this.showCheckMark = showCheckMark
        notifyDataSetChanged()
    }

    interface IServiceCallStatus{
        fun onTapServiceCallStatus(position: Int)
    }

    class ServiceCallStatusHolder(val view: View): RecyclerView.ViewHolder(view) {
        val binding = ContainerOneItemLayoutSelectableBinding.bind(view)

        fun render(item: Int){
            binding.mainText.text = view.context.getString(item)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ServiceCallStatusHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = inflater.inflate(R.layout.container_one_item_layout_selectable,parent,false)
        return ServiceCallStatusHolder(viewHolder)
    }

    override fun onBindViewHolder(
        holder: ServiceCallStatusHolder,
        position: Int
    ) {
        val item = statusList[position]
        holder.render(item)
        holder.binding.checkedImage.visibility = View.GONE
        if( position == itemSelected && showCheckMark ) holder.binding.checkedImage.visibility = View.VISIBLE
        holder.binding.rowContainer.setOnClickListener {
            listener.onTapServiceCallStatus(position)
        }
    }

    override fun getItemCount(): Int = statusList.size


}