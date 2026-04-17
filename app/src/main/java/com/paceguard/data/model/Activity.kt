package com.paceguard.data.model

data class LatLng(val lat: Double, val lng: Double)

data class Activity(
    val id: String,
    val athleteName: String,
    val sport: Sport,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val gpsPoints: List<LatLng>
)
