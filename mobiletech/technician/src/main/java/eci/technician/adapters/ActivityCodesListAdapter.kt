package eci.technician.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import eci.technician.R
import eci.technician.databinding.TwoItemLayoutBinding
import eci.technician.models.order.ActivityCode

class ActivityCodesListAdapter(activityCodesList: MutableList<ActivityCode>)
    : BaseAdapter() {

    private var activityCodeList: MutableList<ActivityCode> = mutableListOf()

    init {
        activityCodeList = activityCodesList
    }

    override fun getCount(): Int {
        return activityCodeList.size;
    }

    override fun getItem(position: Int): ActivityCode {
        return activityCodeList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TwoItemLayoutBinding.inflate(inflater, parent, false)
        val item = activityCodeList[position]
        binding.text1.text = item.activityCodeName.toString()
        binding.text2.text = item.description.toString()
        return binding.root
    }
}