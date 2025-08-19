package eci.technician.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eci.technician.R
import eci.technician.tools.Constants
import kotlinx.android.synthetic.main.container_map_item.view.*

class MapsSettingsAdapter(private val mapList: MutableList<Triple<String, String, Drawable>>, val listener: MapsSettingsAdapterListener) : RecyclerView.Adapter<MapsSettingsAdapter.MapsSettingsViewHolder>() {
    lateinit var context: Context
    private var navigationAppName: String? = null

    interface MapsSettingsAdapterListener {
        fun onMapItemTap(pair: Triple<String, String, Drawable>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapsSettingsViewHolder {
        context = parent.context
        val viewHolder = LayoutInflater.from(context).inflate(R.layout.container_map_item, parent, false)
        return MapsSettingsViewHolder(viewHolder)
    }

    override fun getItemCount(): Int {
        return mapList.size
    }

    override fun onBindViewHolder(holder: MapsSettingsViewHolder, position: Int) {
        val item = mapList[position]
        holder.mapAppTextView.text = item.first
        navigationAppName = context.getSharedPreferences(Constants.PREFERENCE_NAVIGATION, Context.MODE_PRIVATE)
            .getString(Constants.PREFERENCE_NAVIGATION_APP, "")
        holder.mapIcon.setImageDrawable(item.third)
        navigationAppName?.let {
            holder.checkContainer.visibility = if (item.second == it) View.VISIBLE else View.GONE
        }
        holder.containerRow.setOnClickListener {
            listener.onMapItemTap(item)
        }
    }

    class MapsSettingsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mapAppTextView: TextView = v.mapAppTextView
        val checkContainer: LinearLayout = v.containerCheckImage
        val containerRow: LinearLayout = v.containerMapItem
        val mapIcon:ImageView = v.mapIcon
    }
}