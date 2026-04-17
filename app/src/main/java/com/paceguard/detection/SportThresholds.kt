package com.paceguard.detection

import com.paceguard.data.model.Sport

object SportThresholds {
    /** Maximum legitimate average speed per sport in m/s. */
    val maxAvgSpeed: Map<Sport, Double> = mapOf(
        Sport.RUN   to 10.0,
        Sport.CYCLE to 25.0,
        Sport.SWIM  to 3.0
    )

    /**
     * A segment whose speed exceeds this multiple of the sport's max is treated as
     * a teleportation signal (GPS spoofing / device malfunction).
     */
    const val TELEPORT_MULTIPLIER = 3.0

    /**
     * Average speed above this multiple of the sport max is CRITICAL;
     * between 1x and this multiple it is WARN.
     */
    const val CRITICAL_SPEED_MULTIPLIER = 2.0
}
