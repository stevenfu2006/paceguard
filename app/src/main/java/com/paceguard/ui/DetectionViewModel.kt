package com.paceguard.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.paceguard.data.ActivityLoader
import com.paceguard.data.DetectionRepository
import com.paceguard.data.model.FlaggedActivity
import com.paceguard.data.model.ScanEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetectionViewModel(private val repository: DetectionRepository) : ViewModel() {

    private val _state = MutableStateFlow<DetectionState>(DetectionState.Idle)
    val state: StateFlow<DetectionState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        _state.value = DetectionState.Running(scanned = 0, total = 0, flagged = emptyList())

        scanJob = viewModelScope.launch {
            val flagged = mutableListOf<FlaggedActivity>()

            repository.scan().collect { event ->
                when (event) {
                    is ScanEvent.FlagFound ->
                        flagged.add(event.flaggedActivity)

                    is ScanEvent.ActivityScanned ->
                        _state.value = DetectionState.Running(
                            scanned = event.scanned,
                            total = event.total,
                            flagged = flagged.toList()
                        )
                }
            }

            val finalTotal = (_state.value as? DetectionState.Running)?.total ?: flagged.size
            _state.value = DetectionState.Complete(
                totalScanned = finalTotal,
                flagged = flagged.toList()
            )
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetectionViewModel(
                        DetectionRepository(ActivityLoader(context.applicationContext))
                    ) as T
            }
    }
}
