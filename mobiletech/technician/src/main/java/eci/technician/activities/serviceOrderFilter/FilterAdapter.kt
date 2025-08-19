package eci.technician.activities.serviceOrderFilter

interface FilterAdapter {
    fun unCheckOtherItems(position:Int)
    fun unCheckAllItems()
    fun <T> checkItem(position: Int, item:T)
}