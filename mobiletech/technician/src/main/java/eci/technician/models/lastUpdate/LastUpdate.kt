package eci.technician.models.lastUpdate

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class LastUpdate(

) : RealmObject() {

    @PrimaryKey
    var id: String = ""
    var lastUpdateDate: Long? = null

    companion object {
        const val ID = "id"
    }
}