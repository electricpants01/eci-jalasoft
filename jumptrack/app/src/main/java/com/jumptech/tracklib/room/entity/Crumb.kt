package com.jumptech.tracklib.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jumptech.tracklib.room.TrackDB
import com.jumptech.tracklib.room.TrackDB.TBL_CRUMB

/**
 * Table to store location in csv format, when the data is sync  delete the synced crumbs
 */
@Entity(tableName = TBL_CRUMB)
data class Crumb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    @ColumnInfo(name = "_encodedCSV")
    val encodedCSV: String,
    @ColumnInfo(name = "_routeKey")
    val routeKey: Long,
    /**
     * Used to count the crumb file csv 1 location = 1 line = 1
     * Send csv with max 10 000 lines
     */
    @ColumnInfo(name = "_date")
    val date: Long
)
