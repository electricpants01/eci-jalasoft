package eci.technician.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.UsedProblemCodeBinding
import eci.technician.interfaces.IUsedProblemCodeListener
import eci.technician.models.data.UsedProblemCode

class ProblemCodesAdapter(
    list: MutableList<UsedProblemCode>,
    val listener: IUsedProblemCodeListener
) : RecyclerView.Adapter<ProblemCodesAdapter.ProblemCodesViewHolder>() {

    private var problemCodesList: MutableList<UsedProblemCode> = mutableListOf()

    init {
        problemCodesList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemCodesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = UsedProblemCodeBinding.inflate(inflater, parent, false)
        return ProblemCodesViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: ProblemCodesViewHolder, position: Int) {
        val item: UsedProblemCode = problemCodesList[position]
        holder.txtName.text = item.problemCodeName ?: ""
        holder.txtDescription.text = item.description ?: ""
        holder.btnRemove.setOnClickListener {
            listener.onUsedProblemCodePressed(item)
        }
    }

    override fun getItemCount(): Int {
        return problemCodesList.size
    }

    class ProblemCodesViewHolder(itemView: UsedProblemCodeBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val txtName: TextView = itemView.txtName
        val txtDescription: TextView = itemView.txtDescription
        val btnRemove: ImageView = itemView.btnRemove
    }

}

