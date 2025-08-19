package eci.technician.models.field_transfer

import com.google.gson.annotations.SerializedName

data class PartRequestTransfer(
         @SerializedName("CreateDate")
         val createDate: String? = "",

         @SerializedName("CreateDateString")
         val createDateString: String? = "",

         @SerializedName("Description")
         val description: String? ="",

         @SerializedName("DestinationTechnician")
         val destinationTechnician: String? = "",

         @SerializedName("DestinationTechnicianName")
         val destinationTechnicianName: String? = "",

         @SerializedName("Item")
         val item: String? = "",

         @SerializedName("ItemId")
         val itemID: Int? = -1,

         @SerializedName("MyRequest")
         val myRequest: Boolean = false,

         @SerializedName("Quantity")
         val quantity: Double? = 0.0,

         @SerializedName("SerialNumber")
         val serialNumber: Any? = null,

         @SerializedName("SourceTechnician")
         val sourceTechnician: String? = "",

         @SerializedName("SourceTechnicianName")
         val sourceTechnicianName: String? = "",

         @SerializedName("ToId")
         val toID: Int? = -1
 ) {
}