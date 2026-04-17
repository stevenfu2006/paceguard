package com.paceguard.data.model

enum class SeverityLevel { WARN, CRITICAL }

sealed class FlagReason {
    data class AverageSpeedExceeded(
        val actualSpeed: Double,
        val maxAllowed: Double
    ) : FlagReason()

    data class SegmentAccelerationExceeded(
        val segmentSpeed: Double,
        val threshold: Double
    ) : FlagReason()

    data class DuplicateRoute(
        val duplicateOfId: String,
        val duplicateOfAthlete: String
    ) : FlagReason()
}

data class FlaggedActivity(
    val activity: Activity,
    val flagReason: FlagReason,
    val severityLevel: SeverityLevel
)
