package eci.technician.models.order

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class CallPriority(
    @PrimaryKey
    @SerializedName("PriorityID")
    var priorityId : Int = 0,
    @SerializedName("Id")
    var id: String = "",
    @SerializedName("Active")
    var active : Boolean = false,
    @SerializedName("Color")
    var color : Double = 0.0,
    @SerializedName("Company")
    var company: String? = null,
    @SerializedName("CompanyId")
    var companyId: String? = null,
    @SerializedName("Description")
    var description: String? = null,
    @SerializedName("Locks")
    var locks: Int = 0,
    @SerializedName("PriorityName")
    var priorityName: String = "",
    @SerializedName("Rank")
    var rank : Int = 0
) : RealmObject(){
    companion object {
        const val PRIORITY_ID = "priorityId"
        const val PRIORITY_NAME = "priorityName"
    }
}
