package eci.technician.models.create_call

import com.google.gson.annotations.SerializedName

data class EquipmentItem(
        @SerializedName("Id")
        val id: String? = "",

        @SerializedName("Active")
        val active: Boolean = false,

        @SerializedName("Caller")
        val caller: String? =  null,

        @SerializedName("Address")
        val address: String? = "",

        @SerializedName("City")
        val city: String? = "",

        @SerializedName("CompanyId")
        val companyID: String? = "",

        @SerializedName("Coordinates")
        val coordinates: Any? = null,

        @SerializedName("Country")
        val country: String? = "",

        @SerializedName("Customer")
        val customer: CustomerItem? = null,

        @SerializedName("CustomerNumber_Code")
        val customerNumberCode: String? = "",

        @SerializedName("CustomerNumber_ID")
        val customerNumberID: Int = 0,

        @SerializedName("EquipmentNumber_Code")
        val equipmentNumberCode: String = "",

        @SerializedName("EquipmentNumber_ID")
        val equipmentNumberID: Int = 0,

        @SerializedName("IPAddress")
        val ipAddress: String? = "",

        @SerializedName("ItemNumber_Code")
        val itemNumberCode: String? = "",

        @SerializedName("ItemNumber_ID")
        val itemNumberID: Int = 0,

        @SerializedName("LastUpdate")
        val lastUpdate: String? = "",

        @SerializedName("Location")
        val location: String? = "",

        @SerializedName("LocationNumber_Code")
        val locationNumberCode: String? = "",

        @SerializedName("LocationNumber_ID")
        val locationNumberID: Int = 0,

        @SerializedName("MACAddress")
        val macAddress: String? = "",

        @SerializedName("MakeDescription")
        val makeDescription: String? = "",

        @SerializedName("MakeNumber_Code")
        val makeNumberCode: String? = "",

        @SerializedName("MakeNumber_ID")
        val makeNumberID: Int = 0,

        @SerializedName("ModelDescription")
        val modelDescription: String = "",

        @SerializedName("ModelNumber_Code")
        val modelNumberCode: String = "",

        @SerializedName("ModelNumber_ID")
        val modelNumberID: Int = 0,

        @SerializedName("Remarks")
        val remarks: String? = "",

        @SerializedName("RequireMeteronServiceCalls")
        val requireMeteronServiceCalls: Boolean = false,

        @SerializedName("SerialNumber")
        val serialNumber: String = "",

        @SerializedName("State")
        val state: String? = "",

        @SerializedName("SupplyItems")
        val supplyItems: Any? = null,

        @SerializedName("TechnicianNumber_Code")
        val technicianNumberCode: String? = "",

        @SerializedName("TechnicianNumber_ID")
        val technicianNumberID: Int = 0,

        @SerializedName("Zip")
        val zip: String? = ""
)
