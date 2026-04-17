package com.paceguard.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paceguard.data.model.Activity
import com.paceguard.data.model.LatLng
import com.paceguard.data.model.Sport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityLoader(private val context: Context) {

    private data class LatLngJson(val lat: Double, val lng: Double)

    private data class ActivityJson(
        val id: String,
        val athleteName: String,
        val sport: String,
        val distanceMeters: Double,
        val durationSeconds: Int,
        val gpsPoints: List<LatLngJson>
    )

    suspend fun loadActivities(): List<Activity> = withContext(Dispatchers.IO) {
        val json = context.assets.open("activities.json").bufferedReader().readText()
        val type = object : TypeToken<List<ActivityJson>>() {}.type
        val raw: List<ActivityJson> = Gson().fromJson(json, type)
        raw.map { it.toDomain() }
    }

    private fun ActivityJson.toDomain() = Activity(
        id = id,
        athleteName = athleteName,
        sport = Sport.valueOf(sport),
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        gpsPoints = gpsPoints.map { LatLng(it.lat, it.lng) }
    )
}
