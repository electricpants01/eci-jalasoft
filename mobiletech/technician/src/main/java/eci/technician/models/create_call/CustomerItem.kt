package eci.technician.models.create_call

import com.google.gson.annotations.SerializedName

data class CustomerItem(
        @SerializedName("Id")
        val id: String? = "",

        @SerializedName("Active")
        val active: Boolean = false,

        @SerializedName("Address")
        val address: String? = "",

        @SerializedName("City")
        val city: String? = "",

        @SerializedName("CompanyId")
        val companyID: String? = "",

        @SerializedName("Country")
        val country: String? = "",

        @SerializedName("County")
        val county: String? = "",

        @SerializedName("CustomerName")
        val customerName: String? = "",

        @SerializedName("CustomerNumber_Code")
        val customerNumberCode: String? = "",

        @SerializedName("CustomerNumber_ID")
        val customerNumberID: Int = 0,

        @SerializedName("Email")
        val email: String? = "",

        @SerializedName("Equipments")
        val equipments: MutableList<EquipmentItem>? = null,

        @SerializedName("Fax")
        val fax: String? = "",

        @SerializedName("Latitude")
        val latitude: Any? = null,

        @SerializedName("Longitude")
        val longitude: Any? = null,

        @SerializedName("Phone1")
        val phone1: String? = "",

        @SerializedName("Phone2")
        val phone2: String? = "",

        @SerializedName("State")
        val state: String? = "",

        @SerializedName("WebSite")
        val webSite: String? = "",

        @SerializedName("Zip")
        val zip: String? = ""
) {
    fun getLocationJoined(): String {
        val locationAddressList = mutableListOf<String>()
        if (this.address?.isNotEmpty() == true) locationAddressList.add(this.address)
        if (this.city?.isNotEmpty() == true) locationAddressList.add(this.city)
        if (this.county?.isNotEmpty() == true) locationAddressList.add(this.county)
        return locationAddressList.joinToString(", ")
    }
}
