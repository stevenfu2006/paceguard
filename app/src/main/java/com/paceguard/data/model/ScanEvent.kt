package com.paceguard.data.model

sealed class ScanEvent {
    /** Emitted after every activity is processed, whether flagged or not. Drives the progress counter. */
    data class ActivityScanned(val scanned: Int, val total: Int) : ScanEvent()

    /** Emitted once per rule violation found. */
    data class FlagFound(val flaggedActivity: FlaggedActivity) : ScanEvent()
}
