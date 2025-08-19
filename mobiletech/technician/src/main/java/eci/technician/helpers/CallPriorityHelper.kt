package eci.technician.helpers

import android.graphics.Color


object CallPriorityHelper {
    private const val BLACK_CODE = 0
    fun parseColor(color: Double): Int {
        if (color <= 0.0)
            return BLACK_CODE
        val red = color % 256
        val green = (color / 256) % 256
        val blue = (color / 65536) % 256
        return Color.rgb(red.toInt(), green.toInt(), blue.toInt())
    }

    fun shouldDisplayPriorityFlag(priorityColor: Int): Boolean {
        if (priorityColor == BLACK_CODE)
            return false
        return true
    }

}