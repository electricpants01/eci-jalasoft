package eci.technician.models.order

import com.google.gson.annotations.SerializedName

data class CanonDetailsResponse(
        @SerializedName("device_summary")
        var deviceSummary: DeviceSummary? = null,
        @SerializedName("ids_device")
        var idsDevice: Boolean? = null,
        @SerializedName("snapshot_logo")
        var snapshotLogo: String? = null,
        @SerializedName("token")
        var token: String? = null
)