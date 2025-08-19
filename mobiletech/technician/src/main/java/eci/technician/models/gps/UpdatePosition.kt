package eci.technician.models.gps

import com.google.gson.annotations.SerializedName
import java.util.*

class UpdatePosition {
    @SerializedName("CarId")
    var carId: UUID? = null

    @SerializedName("Latitude")
    var latitude = 0.0

    @SerializedName("Longitude")
    var longitude = 0.0

    @SerializedName("Speed")
    var speed = 0.0

    @SerializedName("Altitude")
    var altitude = 0.0

    @SerializedName("UpdateTime")
    var updateTime: Date? = null

    @SerializedName("TechnicianId")
    private var technicianId = 0

    constructor(
        carId: UUID?,
        latitude: Double,
        longitude: Double,
        speed: Double,
        altitude: Double,
        updateTime: Date?
    ) {
        this.carId = carId
        this.latitude = latitude
        this.longitude = longitude
        this.speed = speed
        this.altitude = altitude
        this.updateTime = updateTime
    }

    fun setTechnicianId(technicianId: Int) {
        this.technicianId = technicianId
    }
}