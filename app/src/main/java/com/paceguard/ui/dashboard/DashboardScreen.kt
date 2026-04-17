package com.paceguard.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paceguard.data.model.FlagReason
import com.paceguard.data.model.FlaggedActivity
import com.paceguard.data.model.SeverityLevel
import com.paceguard.ui.DetectionState
import com.paceguard.ui.DetectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DetectionViewModel,
    onFlagClick: (index: Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("PaceGuard") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScanControls(
                state = state,
                onScanClick = viewModel::startScan,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            val flagged = when (val s = state) {
                is DetectionState.Running -> s.flagged
                is DetectionState.Complete -> s.flagged
                DetectionState.Idle -> emptyList()
            }

            if (flagged.isEmpty() && state is DetectionState.Idle) {
                Text(
                    text = "Tap Scan to begin fraud detection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(flagged) { index, item ->
                    FlaggedActivityCard(
                        item = item,
                        onClick = { onFlagClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanControls(
    state: DetectionState,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onScanClick,
            enabled = state !is DetectionState.Running,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedContent(targetState = state is DetectionState.Running, label = "scan_btn") { scanning ->
                if (scanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Scanning\u2026")
                    }
                } else {
                    Text(if (state is DetectionState.Complete) "Scan Again" else "Scan Activities")
                }
            }
        }

        when (state) {
            is DetectionState.Running -> {
                val progress = if (state.total > 0) state.scanned.toFloat() / state.total else 0f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Scanned ${state.scanned} / ${state.total}  ·  ${state.flagged.size} flag(s) found",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            is DetectionState.Complete -> {
                Text(
                    "Scan complete — ${state.totalScanned} activities, ${state.flagged.size} flag(s) found",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun FlaggedActivityCard(item: FlaggedActivity, onClick: () -> Unit) {
    val containerColor = when (item.severityLevel) {
        SeverityLevel.WARN -> MaterialTheme.colorScheme.tertiaryContainer
        SeverityLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    }
    val onContainerColor = when (item.severityLevel) {
        SeverityLevel.WARN -> MaterialTheme.colorScheme.onTertiaryContainer
        SeverityLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.activity.athleteName,
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainerColor,
                    modifier = Modifier.weight(1f)
                )
                SeverityBadge(item.severityLevel)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${item.activity.sport}  ·  ${item.flagReason.shortLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = onContainerColor
            )
        }
    }
}

@Composable
private fun SeverityBadge(level: SeverityLevel) {
    val (bg, fg, label) = when (level) {
        SeverityLevel.WARN -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            "WARN"
        )
        SeverityLevel.CRITICAL -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "CRITICAL"
        )
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private val FlagReason.shortLabel: String
    get() = when (this) {
        is FlagReason.AverageSpeedExceeded -> "Avg Speed Exceeded"
        is FlagReason.SegmentAccelerationExceeded -> "Teleportation Detected"
        is FlagReason.DuplicateRoute -> "Duplicate Route"
    }
