package eci.technician.data

import io.realm.*
import java.util.*

class MainMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var version: Long = oldVersion
        val schema = realm.schema

        if (version == 3L) {
            migrationFrom3To4(schema)

        }

    }

    /**
     * this is the first migration, currently 12/16/2021 the production env is working with
     * the version 3 of the DB
     * The dev env  is working with version 3 , use this migration for production only
     */
    private fun migrationFrom3To4(schema: RealmSchema){
        val hasAttachmentItem = schema.contains("AttachmentItem")
        if (hasAttachmentItem){
            schema.remove("AttachmentItem")
        }

        schema.get("FilterCriteria")
            ?.addField("callStatusSelected", Int::class.java, FieldAttribute.REQUIRED)

        schema.get("AttachmentIncompleteRequest")
            ?.addField(
                "id",
                String::class.java,
                FieldAttribute.REQUIRED,
                FieldAttribute.PRIMARY_KEY,
                FieldAttribute.INDEXED
            )
            ?.removeField("fileContentBase64")
            ?.addField("attachmentItemEntityId", String::class.java, FieldAttribute.REQUIRED)
            ?.addField("localPath", String::class.java, FieldAttribute.REQUIRED)

        schema.remove("PayPeriod")
        schema.create("PayPeriod")
            .addField(
                "payPeriodId",
                String::class.java,
                FieldAttribute.PRIMARY_KEY,
                FieldAttribute.REQUIRED,
                FieldAttribute.INDEXED
            )
            .addField("dateFrom", String::class.java)
            .addField("dateTo", String::class.java)
            .addField("dateValue", Date::class.java)

        schema.get("Shift")
            ?.removeField("shiftClosed")
            ?.removeField("payPeriodId")
            ?.addField("payPeriodId", String::class.java)
            ?.addField("isShiftClosed", Boolean::class.java, FieldAttribute.REQUIRED)


        schema.get("ShiftDetails")
            ?.setRequired("closeAction", true)


        schema.create("AttachmentItemEntity")
            .addField(
                "id",
                String::class.java,
                FieldAttribute.REQUIRED,
                FieldAttribute.PRIMARY_KEY,
                FieldAttribute.INDEXED
            )
            .addField("dBFileLinkID", Int::class.java, FieldAttribute.REQUIRED)
            .addField("createDate", String::class.java)
            .addField("description", String::class.java)
            .addField("filename", String::class.java)
            .addField("link", String::class.java)
            .addField("mimeHeader", String::class.java)
            .addField("number", Int::class.java, FieldAttribute.REQUIRED)
            .addField("callNumberId", Int::class.java, FieldAttribute.REQUIRED)
            .addField("localPath", String::class.java)
            .addField("isCreatedLocally", Boolean::class.java, FieldAttribute.REQUIRED)
            .addField("downloadTime", Date::class.java)


    }
}