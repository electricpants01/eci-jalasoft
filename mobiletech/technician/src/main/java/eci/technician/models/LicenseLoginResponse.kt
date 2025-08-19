package eci.technician.models

import com.google.gson.annotations.SerializedName

data class LicenseLoginResponse(
    val service: String,
    val provider: String,
    val account: String,
    @SerializedName("accountalias")
    val accountAlias: String,
    val license: License,
    val _links: LinkContent
) {
}

data class LinkContent(
    val _self: SelfContent,
    val host: HostContent,
    val gps: GpsContent,
    val chat: ChatContent,
    val esn: EsnContent
)

data class License(
    val count: Int,
    val expires: String
)

data class SelfContent(
    val href: String
)

data class HostContent(
    val href: String
)

data class GpsContent(
    val href: String,
    val enabled: String,
    val location: String,
    @SerializedName("gpsprefix")
    val gpsPrefix: String
)

data class ChatContent(
    val href: String,
    val enabled: String
)

data class EsnContent(
    val href: String
)