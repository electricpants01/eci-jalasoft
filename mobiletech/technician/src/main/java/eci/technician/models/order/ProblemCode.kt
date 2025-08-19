package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class ProblemCode : RealmObject() {
    @PrimaryKey
    @SerializedName("ProblemCodeId")
    var problemCodeId:Int = 0

    @SerializedName("ProblemCodeName")
    var problemCodeName: String? = null

    @SerializedName("Description")
    var description: String? = null

    @SerializedName("Active")
    var isActive:Boolean = false

    @SerializedName("LastUpdateString")
    var lastUpdateString: Date? = null

    companion object {
        const val PROBLEM_CODE_NAME_QUERY = "problemCodeName"
    }
}