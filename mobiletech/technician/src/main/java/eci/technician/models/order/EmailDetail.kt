package eci.technician.models.order

import io.realm.RealmObject

open class EmailDetail(
        var bccAddress: String = "",
        var ccAddress: String = "",
        var emailContent: String = "",
        var subjectEmail: String = "",
        var toAddress: String = "",
        var emailCallNumberId: Int = 0
) : RealmObject() {
    object COLUMNS {
        const val EMAIL_CALL_NUMBER_ID = "emailCallNumberId"
    }
}