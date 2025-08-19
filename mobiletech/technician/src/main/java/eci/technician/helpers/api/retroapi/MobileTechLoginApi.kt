package eci.technician.helpers.api.retroapi

import eci.technician.models.LicenseLoginResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface MobileTechLoginApi {

    @GET("{account}")
    suspend fun licenseLogin(@Path("account") account: String): Response<LicenseLoginResponse>
}