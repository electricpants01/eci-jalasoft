package eci.technician.models.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Parts(@SerializedName("ItemName")
                 var itemName: String? = null,
                 @SerializedName("Description")
                 var description: String? = null,
                 @SerializedName("Quantity")
                 var quantity: Int = 0,
                 @SerializedName("SourceTechnicianName")
                 var sourceTechnicianName: String? = null,
                 @SerializedName("WarehouseId")
                 var warehouseId: Int = 0): Serializable {
    val quantityText: String
        get() = "Quantity: $quantity"
}