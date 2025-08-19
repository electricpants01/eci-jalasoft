package eci.technician.helpers.api.retroapi

data class GenericDataModel<T>( val isSuccess:Boolean?, val data:T?, val message:String?, val requestStatus: RequestStatus?)