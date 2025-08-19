package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import eci.technician.databinding.ContainerEmailItemBinding
import eci.technician.helpers.AppAuth

class EmailSummaryAdapter(private val emailList: MutableList<String>, private val listener: EmailSummaryListener) : RecyclerView.Adapter<EmailSummaryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContainerEmailItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = emailList[position]
        holder.binding?.item = item
        holder.binding?.emailAddress?.text = item
    }

    override fun getItemCount(): Int {
        return emailList.size
    }

    interface EmailSummaryListener {
        fun onEmailDeleted(position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ContainerEmailItemBinding? = DataBindingUtil.bind(itemView)

        init {
            binding?.let { binding ->

                binding.emailAddress.setOnClickListener {
                    listener.onEmailDeleted(adapterPosition)
                }
            }
        }
    }

}