package eci.technician.activities.serviceOrderFilter.filterModels

data class CallTypeFilter(
    val callTypeId: Int,
    val callTypeDescription: String,
    val callTypeCode: String,
    var isChecked: Boolean
)