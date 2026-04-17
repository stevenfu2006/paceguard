package com.paceguard.data

import com.paceguard.data.model.ScanEvent
import com.paceguard.detection.checkAverageSpeed
import com.paceguard.detection.checkDuplicateRoute
import com.paceguard.detection.checkSegmentAcceleration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DetectionRepository(private val loader: ActivityLoader) {

    /**
     * Cold flow that processes each activity sequentially, emitting [ScanEvent.FlagFound]
     * for every rule violation and [ScanEvent.ActivityScanned] after each activity.
     * A 300ms delay between activities simulates a streaming pipeline.
     */
    fun scan(): Flow<ScanEvent> = flow {
        val activities = loader.loadActivities()
        val total = activities.size

        activities.forEachIndexed { index, activity ->
            buildList {
                checkAverageSpeed(activity)?.let { add(it) }
                checkSegmentAcceleration(activity)?.let { add(it) }
                checkDuplicateRoute(activity, activities)?.let { add(it) }
            }.forEach { emit(ScanEvent.FlagFound(it)) }

            emit(ScanEvent.ActivityScanned(scanned = index + 1, total = total))
            delay(300)
        }
    }
}
