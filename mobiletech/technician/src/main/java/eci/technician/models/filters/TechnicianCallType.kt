package eci.technician.models.filters

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class TechnicianCallType (
        @SerializedName("Id")
        var id: String? = "",
        @SerializedName("Active")
        var active: Boolean = false,
        @SerializedName("CallType_Code")
        var callTypeCode: String? = "",
        @SerializedName("CallType_Description")
        var callTypeDescription: String? = "",
        @PrimaryKey
        @SerializedName("CallType_ID")
        var callTypeId: Int = 0,
        @SerializedName("CompanyId")
        var companyId: String? = "",
        var isChecked: Boolean = false
) : RealmObject(){
    object COLUMNS{
        const val ACTIVE = "active"
        const val IS_CHECKED = "isChecked"
        const val CALL_TYPE_ID = "callTypeId"
        const val CALL_TYPE_DESCRIPTION = "callTypeDescription"
        const val CALL_TYPE_CODE = "callTypeCode"
    }
}