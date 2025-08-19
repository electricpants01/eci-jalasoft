package com.jumptech.tracklib.room.dao

import androidx.room.*
import com.jumptech.tracklib.room.entity.Crumb

@Dao
interface CrumbDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(crumb: Crumb)

    @Query("SELECT * FROM crumb  ORDER BY _date LIMIT :quantity")
    fun getCrumbs(quantity: Long): List<Crumb>

    @Query("SELECT * FROM crumb")
    fun getAllCrumbs():List<Crumb>

    @Query("SELECT COUNT(*) FROM  crumb")
    fun getTotalCrumbs():Long

    @Delete
    fun deleteCrumbs(crumbs: List<Crumb>)
}