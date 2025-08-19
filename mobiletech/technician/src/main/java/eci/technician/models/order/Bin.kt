package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import eci.technician.repository.PartsRepository
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
open class Bin(
    @SerializedName("BinId")
    var binId: Int = 0,
    @SerializedName("Bin")
    var bin: String? = null,
    @SerializedName("BinAvailableQty")
    var binAvailableQuantity: Double? = 0.0,
    @SerializedName("BinOnHandQty")
    var binOnHandQuantity: Double? = 0.0,
    @SerializedName("Allocated")
    var allocated: Double? = 0.0,
    @SerializedName("BinDiscription")
    var binDescription: String? = null,
    @SerializedName("SerialNumber")
    var serialNumber: String? = null,
    @Ignore
    var binAvailableQuantityUI: Double = 0.0

) : RealmObject() {

    fun updateAvailableQuantityUI(partItemId: Int, warehouseId: Int): Double {
        val valueToSubtract = PartsRepository.getUsedQuantityBins(
            binId,
            partItemId,
            warehouseId,
            serialNumber ?: ""
        )
        this.binAvailableQuantityUI = (binAvailableQuantity ?: 0.0) - valueToSubtract
        return binAvailableQuantityUI
    }

    fun usedPartsInDB(partItemId: Int, warehouseId: Int): Double {
        return PartsRepository.getUsedQuantityBins(
            binId,
            partItemId,
            warehouseId,
            serialNumber ?: ""
        )
    }
}