package com.paceguard.ui

import com.paceguard.data.model.FlaggedActivity

sealed class DetectionState {
    object Idle : DetectionState()

    data class Running(
        val scanned: Int,
        val total: Int,
        val flagged: List<FlaggedActivity>
    ) : DetectionState()

    data class Complete(
        val totalScanned: Int,
        val flagged: List<FlaggedActivity>
    ) : DetectionState()
}
