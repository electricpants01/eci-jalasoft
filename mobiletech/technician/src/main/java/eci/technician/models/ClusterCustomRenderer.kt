package eci.technician.models

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import eci.technician.R
import eci.technician.helpers.CallPriorityHelper
import kotlinx.android.synthetic.main.custom_marker.view.*

class ClusterCustomRenderer(
        private val context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<ClusterObject>
) : DefaultClusterRenderer<ClusterObject>(context, map, clusterManager) {
    override fun onBeforeClusterItemRendered(item: ClusterObject, markerOptions: MarkerOptions) {
        var iconFactory = IconGenerator(context)
        var title = item.callNumber_Code
        if (item.status_StatusCode == "H" || item.status_StatusCode == "S") {
            title = "${item.callNumber_Code} (${item.status_StatusCode})"
        }

        val customMarker = LayoutInflater.from(context).inflate(R.layout.custom_marker, null)
        customMarker.serviceCall.text = title
        iconFactory.setContentView(customMarker)
        var priorityColor = CallPriorityHelper.parseColor(item.color)
        if(CallPriorityHelper.shouldDisplayPriorityFlag((priorityColor))) {
            customMarker.iconPriority.setColorFilter(priorityColor)
            customMarker.iconPriority.visibility = View.VISIBLE
        }

        markerOptions.position(item.position).icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon()))
    }

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterObject>): Boolean {
        return cluster.size > 1
    }

    override fun onClusterItemRendered(clusterItem: ClusterObject, marker: Marker) {
        marker.tag = clusterItem
    }

    override fun getBucket(cluster: Cluster<ClusterObject>): Int {
        return cluster.size
    }

    override fun getClusterText(bucket: Int): String {
        return super.getClusterText(bucket).replace("+", "")
    }
}