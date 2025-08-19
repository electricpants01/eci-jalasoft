package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class HoldCode : RealmObject() {
    @PrimaryKey
    @SerializedName("OnHoldCodeId")
    var onHoldCodeId = 0

    @SerializedName("OnHoldCode")
    var onHoldCode: String? = null

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("Active")
    var active = false

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null

    @SerializedName("AllowTechAssign")
    var allowTechAssign = false

    @SerializedName("AllowTechRelease")
    var allowTechRelease = false

    @SerializedName("TypeId")
    var typeId = 0


    /*
        typeId:3  -> "Waiting for parts"
     */
    companion object {
        const val HOLD_CODE_NAME_QUERY = "onHoldCode"
        const val ALLOW_TECH_RELEASE = "allowTechRelease"
        const val ALLOW_TECH_ASSIGN = "allowTechAssign"

        /**
         * this typeId 3 -> is the flag for Waiting for parts,
         * because the back doesn't know where to look for this info (EA value hardcoded)
         */
        const val WAITING_FOR_PARTS_TYPE_ID = 3
        const val ON_HOLD_CODE_ID = "onHoldCodeId"
    }
}