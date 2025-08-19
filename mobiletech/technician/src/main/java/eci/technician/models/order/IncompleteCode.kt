package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class IncompleteCode(
        @PrimaryKey
        @SerializedName("IncompleteCodeId") var incompleteCodeId: Int? = null,
        @SerializedName("Id")
        var id: String? = null,
        @SerializedName("Active") var active: Boolean? = false,
        @SerializedName("Category") var category: String? = null,
        @SerializedName("Code") var code: String? = null,
        @SerializedName("CompanyId") var companyId: String? = null,
        @SerializedName("Description") var description: String? = null,
        @SerializedName("LastUpdate") var lastUpdate: String? = null,
        @SerializedName("OnHoldCodeId") var onHoldCodeId: String? = null,
        var isChecked:Boolean = false
) : RealmObject() {
        object COLUMNS{
                const val ID = "id"
                const val IS_CHECKED = "isChecked"
                const val INCOMPLETE_CODE_ID = "incompleteCodeId"
        }
}