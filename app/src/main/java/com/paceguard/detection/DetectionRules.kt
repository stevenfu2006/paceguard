package com.paceguard.detection

import com.paceguard.data.model.Activity
import com.paceguard.data.model.FlagReason
import com.paceguard.data.model.FlaggedActivity
import com.paceguard.data.model.LatLng
import com.paceguard.data.model.SeverityLevel
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Flags an activity whose average speed (distanceMeters / durationSeconds) exceeds
 * the sport-specific maximum defined in [SportThresholds].
 *
 * Severity: WARN if speed is between 1× and [SportThresholds.CRITICAL_SPEED_MULTIPLIER]×
 * the max; CRITICAL above that threshold.
 */
fun checkAverageSpeed(activity: Activity): FlaggedActivity? {
    val maxAllowed = SportThresholds.maxAvgSpeed[activity.sport] ?: return null
    val avgSpeed = activity.distanceMeters / activity.durationSeconds

    if (avgSpeed <= maxAllowed) return null

    val severity = if (avgSpeed >= maxAllowed * SportThresholds.CRITICAL_SPEED_MULTIPLIER) {
        SeverityLevel.CRITICAL
    } else {
        SeverityLevel.WARN
    }

    return FlaggedActivity(
        activity = activity,
        flagReason = FlagReason.AverageSpeedExceeded(avgSpeed, maxAllowed),
        severityLevel = severity
    )
}

/**
 * Flags activities containing a GPS segment whose implied speed exceeds
 * [SportThresholds.TELEPORT_MULTIPLIER] times the sport maximum — a signal of
 * GPS spoofing, device teleportation, or route fabrication.
 *
 * Time is distributed uniformly across segments: each segment duration =
 * durationSeconds / (pointCount - 1).
 *
 * Always CRITICAL — a single teleportation-grade segment is a hard fraud signal.
 */
fun checkSegmentAcceleration(activity: Activity): FlaggedActivity? {
    val points = activity.gpsPoints
    if (points.size < 2) return null

    val maxAllowed = SportThresholds.maxAvgSpeed[activity.sport] ?: return null
    val threshold = maxAllowed * SportThresholds.TELEPORT_MULTIPLIER
    val segmentDuration = activity.durationSeconds.toDouble() / (points.size - 1)

    for (i in 0 until points.size - 1) {
        val dist = haversineMeters(points[i], points[i + 1])
        val segmentSpeed = dist / segmentDuration
        if (segmentSpeed > threshold) {
            return FlaggedActivity(
                activity = activity,
                flagReason = FlagReason.SegmentAccelerationExceeded(segmentSpeed, threshold),
                severityLevel = SeverityLevel.CRITICAL
            )
        }
    }
    return null
}

/**
 * Flags an activity whose GPS point sequence is identical to an earlier activity
 * in [allActivities]. Only the later activity is flagged to avoid symmetric duplication.
 *
 * WARN severity: repeated routes can be legitimate (e.g., same course), but identical
 * GPS sequences from different athletes warrant manual review.
 */
fun checkDuplicateRoute(activity: Activity, allActivities: List<Activity>): FlaggedActivity? {
    val earlierActivities = allActivities.takeWhile { it.id != activity.id }
    val duplicate = earlierActivities.firstOrNull { it.gpsPoints == activity.gpsPoints }
        ?: return null

    return FlaggedActivity(
        activity = activity,
        flagReason = FlagReason.DuplicateRoute(duplicate.id, duplicate.athleteName),
        severityLevel = SeverityLevel.WARN
    )
}

/** Haversine great-circle distance between two coordinates, in metres. */
private fun haversineMeters(a: LatLng, b: LatLng): Double {
    val earthRadius = 6_371_000.0
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(h))
}
