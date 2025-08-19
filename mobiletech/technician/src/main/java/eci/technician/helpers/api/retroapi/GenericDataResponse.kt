package eci.technician.helpers.api.retroapi

import eci.technician.helpers.ErrorHelper.RequestError

data class GenericDataResponse <T> (val responseType: RequestStatus, val data: T? , val onError: RequestError?) {
}