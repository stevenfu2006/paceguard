package com.paceguard.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetectionViewModel,
    flagIndex: Int,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val flagged = when (val s = state) {
        is DetectionState.Running -> s.flagged
        is DetectionState.Complete -> s.flagged
        else -> emptyList()
    }
    val item = flagged.getOrNull(flagIndex) ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flag Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActivityInfoCard(item)
            FlagDetailCard(item)
            GpsPointsCard(item)
        }
    }
}

@Composable
private fun ActivityInfoCard(item: FlaggedActivity) {
    val act = item.activity
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Activity", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            InfoRow("Athlete", act.athleteName)
            InfoRow("Sport", act.sport.name)
            InfoRow("Distance", "${"%.2f".format(act.distanceMeters / 1000)} km")
            InfoRow("Duration", formatDuration(act.durationSeconds))
            InfoRow("Avg Speed", "${"%.2f".format(act.distanceMeters / act.durationSeconds)} m/s")
            InfoRow("GPS Points", act.gpsPoints.size.toString())
        }
    }
}

@Composable
private fun FlagDetailCard(item: FlaggedActivity) {
    val containerColor = when (item.severityLevel) {
        SeverityLevel.WARN -> MaterialTheme.colorScheme.tertiaryContainer
        SeverityLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    }
    val onContainerColor = when (item.severityLevel) {
        SeverityLevel.WARN -> MaterialTheme.colorScheme.onTertiaryContainer
        SeverityLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Detection Rule",
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainerColor,
                    modifier = Modifier.weight(1f)
                )
                SeverityChip(item.severityLevel, onContainerColor)
            }
            Spacer(Modifier.height(2.dp))
            FlagReasonDetails(item.flagReason, onContainerColor)
        }
    }
}

@Composable
private fun FlagReasonDetails(reason: FlagReason, textColor: androidx.compose.ui.graphics.Color) {
    when (reason) {
        is FlagReason.AverageSpeedExceeded -> {
            val overPct = ((reason.actualSpeed / reason.maxAllowed - 1) * 100).roundToInt()
            InfoRow("Rule", "Average Speed Check", textColor)
            InfoRow("Recorded Speed", "${"%.2f".format(reason.actualSpeed)} m/s", textColor)
            InfoRow("Sport Maximum", "${"%.1f".format(reason.maxAllowed)} m/s", textColor)
            InfoRow("Overage", "+$overPct%", textColor)
        }
        is FlagReason.SegmentAccelerationExceeded -> {
            val multiple = reason.segmentSpeed / reason.threshold
            InfoRow("Rule", "Segment Acceleration (Teleportation)", textColor)
            InfoRow("Segment Speed", "${"%.1f".format(reason.segmentSpeed)} m/s", textColor)
            InfoRow("Threshold", "${"%.1f".format(reason.threshold)} m/s", textColor)
            InfoRow("Multiple", "${"%.1f".format(multiple)}×", textColor)
        }
        is FlagReason.DuplicateRoute -> {
            InfoRow("Rule", "Duplicate Route", textColor)
            InfoRow("Matches Activity", reason.duplicateOfId, textColor)
            InfoRow("Matching Athlete", reason.duplicateOfAthlete, textColor)
        }
    }
}

@Composable
private fun GpsPointsCard(item: FlaggedActivity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "GPS Points (${item.activity.gpsPoints.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            item.activity.gpsPoints.forEachIndexed { i, point ->
                if (i > 0) Divider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "${i + 1}.  ${"%.6f".format(point.lat)}, ${"%.6f".format(point.lng)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun SeverityChip(level: SeverityLevel, onContainerColor: androidx.compose.ui.graphics.Color) {
    val (bg, fg) = when (level) {
        SeverityLevel.WARN -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        SeverityLevel.CRITICAL -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            text = level.name,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
