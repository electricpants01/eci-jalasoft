package com.jumptech.tracklib.repository

import android.content.Context
import com.jumptech.tracklib.room.TrackDB
import com.jumptech.tracklib.room.entity.Crumb

object CrumbRepository {

    const val CRUMB_SIZE = 20000

    /**
     * Add a new Crumb(with location info) to the DB
     */
    fun addCrumb(crumb: Crumb, context: Context) {
        TrackDB.getInstance(context.applicationContext).crumbDao.insert(crumb)
    }

    fun deleteCrumbs(list: List<Crumb>, context: Context) {
        TrackDB.getInstance(context.applicationContext).crumbDao.deleteCrumbs(list)
    }

    fun getCrumbs(crumbSize: Long, context: Context): List<Crumb> {
        return TrackDB.getInstance(context.applicationContext).crumbDao.getCrumbs(crumbSize)
    }

    fun getTotalCrumbs(context: Context): Long {
        return TrackDB.getInstance(context.applicationContext).crumbDao.getTotalCrumbs()
    }

    private fun getAllCrumbs(context: Context): List<Crumb> {
        return TrackDB.getInstance(context.applicationContext).crumbDao.getAllCrumbs()
    }

    fun getChunkedCrumbs(context: Context, size: Int): List<List<Crumb>> {
        val fullList = getAllCrumbs(context)
        return fullList.chunked(size)
    }
}