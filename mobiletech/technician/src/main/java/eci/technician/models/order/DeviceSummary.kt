package eci.technician.models.order

import com.google.gson.annotations.SerializedName

data class DeviceSummary(
        @SerializedName("customer_id")
        val customerId: String,
        @SerializedName("customer_name")
        val customerName: String,
        @SerializedName("device_id")
        val deviceId: String,
        @SerializedName("device_name")
        val deviceName: Any,
        @SerializedName("install_date")
        val installDate: String,
        @SerializedName("last_activity")
        val lastActivity: String,
        @SerializedName("last_report_date")
        val lastReportDate: String?,
        @SerializedName("owner_company_id")
        val ownerCompanyId: String,
        @SerializedName("product_name")
        val productName: String,
        @SerializedName("status")
        val status: String?
)