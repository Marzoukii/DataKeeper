package com.marzouki.datasaver.presentation.speedtest

import com.marzouki.datasaver.ui.components.SectionTitle
import androidx.compose.material.icons.rounded.History
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.enums.SpeedTestPhase
import com.marzouki.datasaver.domain.enums.SpeedTestRating
import com.marzouki.datasaver.domain.model.SpeedTestProgress
import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.model.SpeedTestServer
import com.marzouki.datasaver.domain.usecase.RunSpeedTestUseCase
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.InfoPill
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenHeader
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.SpeedGauge
import com.marzouki.datasaver.ui.components.ThinBar
import com.marzouki.datasaver.ui.components.formatDayTime
import com.marzouki.datasaver.ui.components.formatElapsed
import com.marzouki.datasaver.ui.components.formatMbps
import com.marzouki.datasaver.ui.components.toReadableSize
import com.marzouki.datasaver.ui.theme.Brand
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric

@Composable
fun SpeedTestScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SpeedTestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SpeedTestEffect.Failed -> snackbarHostState.showSnackbar(resources.getString(R.string.speed_test_failed))
        }
    }
    SpeedTestContent(state, viewModel::onIntent, modifier)
}

/** Accueil du test, test en cours, puis résultat : un seul écran qui change de contenu. */
@Composable
private fun SpeedTestContent(
    state: SpeedTestState,
    onIntent: (SpeedTestIntent) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val progress = state.progress
    val result = state.result
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
        ) {
            when {
                progress != null -> RunningContent(progress, onCancel = { onIntent(SpeedTestIntent.CancelClicked) })
                result != null -> ResultContent(result, state.history.filter { it != result }, nowMillis)
                else -> IdleContent(state, onStart = { onIntent(SpeedTestIntent.StartClicked) }, nowMillis)
            }
        }
        if (progress == null && result != null) {
            StartButton(
                label = stringResource(R.string.speed_test_restart),
                icon = Icons.Rounded.Refresh,
                onClick = { onIntent(SpeedTestIntent.StartClicked) },
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = 12.dp),
            )
        }
    }

    if (state.showMobileWarning) {
        MobileWarningDialog(
            onConfirm = { onIntent(SpeedTestIntent.MobileTestConfirmed) },
            onDismiss = { onIntent(SpeedTestIntent.MobileWarningDismissed) },
        )
    }
}

/** Avant le test : réseau utilisé, bouton de lancement, puis l'historique en tableau réglé. */
@Composable
private fun ColumnScope.IdleContent(state: SpeedTestState, onStart: () -> Unit, nowMillis: Long) {
    ScreenHeader(
        overline = stringResource(R.string.speed_test_overline),
        title = stringResource(R.string.speed_test_title),
    )
    NetworkCard(state.isOnWifi)
    StartButton(stringResource(R.string.speed_test_start), Icons.Rounded.PlayArrow, onStart, Modifier.fillMaxWidth())
    val history = state.history
    if (history.isEmpty()) {
        DataKeeperCard(contentPadding = 16.dp) {
            SectionTitle(stringResource(R.string.speed_test_history), Icons.Rounded.History)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.speed_test_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        HistoryTable(history.first(), history.drop(1), nowMillis)
    }
}

/** Réseau du test : gratuit en Wi-Fi, plafonné en données mobiles. */
@Composable
private fun NetworkCard(isOnWifi: Boolean) {
    DataKeeperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = if (isOnWifi) Icons.Rounded.Wifi else Icons.Rounded.SignalCellularAlt,
                container = if (isOnWifi) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                content = if (isOnWifi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                size = 40,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (isOnWifi) R.string.speed_test_wifi_title else R.string.speed_test_mobile_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (isOnWifi) {
                        stringResource(R.string.speed_test_wifi_body)
                    } else {
                        stringResource(R.string.speed_test_mobile_body, RunSpeedTestUseCase.MOBILE_MAX_BYTES.toReadableSize())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LastTestCard(last: SpeedTestResult?, nowMillis: Long) {
    DataKeeperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.speed_test_last), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (last != null) {
                Text(
                    "${formatDayTime(last.atMillis, nowMillis)} · ${last.badgeLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (last == null) {
            Text(
                stringResource(R.string.speed_test_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val mbps = stringResource(R.string.speed_test_unit_mbps)
                LastTestTile(stringResource(R.string.speed_test_download), formatMbps(last.downloadMbps), mbps, DataColors.wifi)
                LastTestTile(stringResource(R.string.speed_test_upload), formatMbps(last.uploadMbps), mbps, DataColors.mobile)
                LastTestTile(
                    stringResource(R.string.speed_test_ping),
                    last.pingMs.toString(),
                    stringResource(R.string.speed_test_unit_ms),
                    MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RowScope.LastTestTile(label: String, value: String, unit: String, color: Color) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge.numeric(), color = color)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColumnScope.RunningContent(progress: SpeedTestProgress, onCancel: () -> Unit) {
    val phaseColor = progress.phase.color()
    ScreenHeader(
        overline = stringResource(R.string.speed_test_running_overline),
        title = stringResource(progress.phase.labelRes),
        overlineColor = Brand.Wifi,
    )
    DataKeeperCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SpeedGauge(mbps = progress.currentMbps ?: 0.0, color = phaseColor, modifier = Modifier.padding(top = 8.dp))
            SpeedValue(progress.currentMbps, phaseColor)
            Text(
                stringResource(R.string.speed_test_server, progress.server?.label.orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    DataKeeperCard(contentPadding = 16.dp) {
        val steps = SpeedTestPhase.entries
        steps.forEachIndexed { index, phase ->
            val status = when {
                phase.ordinal < progress.phase.ordinal -> StepStatus.DONE
                phase == progress.phase -> StepStatus.ACTIVE
                else -> StepStatus.PENDING
            }
            StepRow(phase, status, progress)
            if (index < steps.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    val fraction by animateFloatAsState(progress.fraction, label = "speedTestProgress")
    ThinBar(fraction, MaterialTheme.colorScheme.primary, Modifier.padding(vertical = 4.dp), height = 6.dp)
    OutlinedButton(
        onClick = onCancel,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge) }
}

private enum class StepStatus { DONE, ACTIVE, PENDING }

@Composable
private fun StepRow(phase: SpeedTestPhase, status: StepStatus, progress: SpeedTestProgress) {
    val color = phase.color()
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (status) {
                StepStatus.DONE -> Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(DataColors.successContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = DataColors.success, modifier = Modifier.size(16.dp)) }
                StepStatus.ACTIVE -> CircularProgressIndicator(color = color, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                StepStatus.PENDING -> {
                    val dashColor = MaterialTheme.colorScheme.outline
                    Canvas(Modifier.size(22.dp)) {
                        drawCircle(
                            dashColor,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(phase.labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = when (status) {
                StepStatus.DONE -> MaterialTheme.colorScheme.onSurface
                StepStatus.ACTIVE -> color
                StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        val value = when (phase) {
            SpeedTestPhase.PING -> progress.pingMs?.let { "$it ${stringResource(R.string.speed_test_unit_ms)}" }
            SpeedTestPhase.DOWNLOAD -> (progress.downloadMbps ?: progress.currentMbps.takeIf { status == StepStatus.ACTIVE })
                ?.let { "${formatMbps(it)} ${stringResource(R.string.speed_test_unit_mbps)}" }
            SpeedTestPhase.UPLOAD -> (progress.uploadMbps ?: progress.currentMbps.takeIf { status == StepStatus.ACTIVE })
                ?.let { "${formatMbps(it)} ${stringResource(R.string.speed_test_unit_mbps)}" }
        }
        if (status == StepStatus.PENDING) {
            Text(
                stringResource(R.string.speed_test_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.numeric(),
                fontWeight = FontWeight.Bold,
                color = if (status == StepStatus.ACTIVE) color else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Résultat : le dernier test en première ligne du tableau d'historique, en bande safran, avec
 * l'échelle de débit et le verdict ; les tests précédents dessous, en lignes réglées.
 */
@Composable
private fun ColumnScope.ResultContent(result: SpeedTestResult, previous: List<SpeedTestResult>, nowMillis: Long) {
    ScreenHeader(
        overline = stringResource(R.string.speed_test_done_overline, formatElapsed(result.atMillis, nowMillis)),
        title = stringResource(R.string.speed_test_title),
    )
    HistoryTable(result, previous, nowMillis)
}

/** Historique en tableau réglé : le test le plus récent en tête, en bande rose avec l'échelle et le verdict. */
@Composable
private fun HistoryTable(result: SpeedTestResult, previous: List<SpeedTestResult>, nowMillis: Long) {
    DataKeeperCard(contentPadding = 16.dp) {
        SectionTitle(stringResource(R.string.speed_test_history), Icons.Rounded.History)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        HistoryHeaderRow()
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brand.AccentTint.copy(alpha = 0.7f))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HistoryCells(result, nowMillis, highlighted = true)
            SpeedScale(result.downloadMbps)
            Text(
                stringResource(SpeedTestRating.from(result.downloadMbps).labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Brand.Accent,
            )
            result.server?.label?.let {
                Text(
                    stringResource(R.string.speed_test_server, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.OnAccentTint,
                )
            }
        }
        previous.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) { HistoryCells(item, nowMillis, highlighted = false) }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }

}

private fun SpeedTestRating.labelRes(): Int = when (this) {
    SpeedTestRating.EXCELLENT -> R.string.speed_test_rating_excellent
    SpeedTestRating.GOOD -> R.string.speed_test_rating_good
    SpeedTestRating.FAIR -> R.string.speed_test_rating_fair
    SpeedTestRating.SLOW -> R.string.speed_test_rating_slow
}

private val HistoryColumns = listOf(1.6f, 1f, 1f, 0.9f, 0.8f)

@Composable
private fun HistoryHeaderRow() {
    Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        listOf(
            stringResource(R.string.speed_test_last),
            stringResource(R.string.speed_test_network),
            stringResource(R.string.speed_test_download),
            stringResource(R.string.speed_test_upload),
            stringResource(R.string.speed_test_ping),
        ).forEachIndexed { index, label ->
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (index >= 2) TextAlign.End else TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.weight(HistoryColumns[index]),
            )
        }
    }
}

@Composable
private fun HistoryCells(result: SpeedTestResult, nowMillis: Long, highlighted: Boolean) {
    val ink = if (highlighted) Brand.Accent else MaterialTheme.colorScheme.onSurface
    val valueStyle = if (highlighted) {
        MaterialTheme.typography.titleMedium.numeric()
    } else {
        MaterialTheme.typography.bodyMedium.numeric()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatDayTime(result.atMillis, nowMillis),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = ink,
            modifier = Modifier.weight(HistoryColumns[0]),
        )
        Box(Modifier.weight(HistoryColumns[1])) {
            val (container, content) = if (result.onWifi) {
                MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            }
            Text(
                result.badgeLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = content,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(container)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text(formatMbps(result.downloadMbps), style = valueStyle, fontWeight = FontWeight.SemiBold, color = ink, textAlign = TextAlign.End, modifier = Modifier.weight(HistoryColumns[2]))
        Text(formatMbps(result.uploadMbps), style = valueStyle, color = ink, textAlign = TextAlign.End, modifier = Modifier.weight(HistoryColumns[3]))
        Text(result.pingMs.toString(), style = valueStyle, color = ink, textAlign = TextAlign.End, modifier = Modifier.weight(HistoryColumns[4]))
    }
}

/** Échelle Lent / Correct / Bon / Excellent avec un repère au débit mesuré (échelle bornée à 100 Mbps). */
@Composable
private fun SpeedScale(mbps: Double) {
    val bands = listOf(5.0 to R.string.speed_test_rating_slow, 15.0 to R.string.speed_test_rating_fair, 50.0 to R.string.speed_test_rating_good, 100.0 to R.string.speed_test_rating_excellent)
    val widths = listOf(0.08f, 0.12f, 0.40f, 0.40f)
    val active = bands.indexOfFirst { mbps < it.first }.let { if (it < 0) bands.lastIndex else it }
    val position = when {
        mbps < 5 -> mbps / 5 * 0.08
        mbps < 15 -> 0.08 + (mbps - 5) / 10 * 0.12
        mbps < 50 -> 0.20 + (mbps - 15) / 35 * 0.40
        else -> (0.60 + (mbps - 50) / 50 * 0.40).coerceAtMost(1.0)
    }.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
            Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                widths.forEachIndexed { index, w ->
                    Box(
                        Modifier
                            .weight(w)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == active) Brand.Accent else Brand.AccentTint),
                    )
                }
            }
            Box(Modifier.fillMaxWidth(position.coerceIn(0.01f, 1f)), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DataColors.mobile),
                )
            }
        }
        Text(
            formatMbps(mbps) + " " + stringResource(R.string.speed_test_unit_mbps),
            style = MaterialTheme.typography.labelSmall.numeric(),
            color = Brand.OnAccentTint,
        )
    }
}

@Composable
private fun RatingPill(rating: SpeedTestRating) {
    val (container, content) = when (rating) {
        SpeedTestRating.EXCELLENT, SpeedTestRating.GOOD -> DataColors.successContainer to DataColors.success
        SpeedTestRating.FAIR -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SpeedTestRating.SLOW -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    val label = when (rating) {
        SpeedTestRating.EXCELLENT -> R.string.speed_test_rating_excellent
        SpeedTestRating.GOOD -> R.string.speed_test_rating_good
        SpeedTestRating.FAIR -> R.string.speed_test_rating_fair
        SpeedTestRating.SLOW -> R.string.speed_test_rating_slow
    }
    InfoPill(stringResource(label), containerColor = container, contentColor = content)
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconContainer: Color,
    iconContent: Color,
    value: String,
    valueColor: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    DataKeeperCard(modifier = modifier, contentPadding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconTile(icon, iconContainer, iconContent, size = 28, shape = CircleShape)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.numeric(), color = valueColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Les noms de serveur sont longs (« Agence Tunisienne d'Internet · Belvédère ») : ils passent à la ligne, calés à droite.
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
    }
}

@Composable
private fun HistoryRow(result: SpeedTestResult, nowMillis: Long) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val (container, content) = if (result.onWifi) {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        }
        Box(
            Modifier
                .width(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(container)
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) { Text(result.badgeLabel(), style = MaterialTheme.typography.labelSmall, color = content) }
        Spacer(Modifier.width(10.dp))
        Text(
            formatDayTime(result.atMillis, nowMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(formatMbps(result.downloadMbps), style = MaterialTheme.typography.titleSmall.numeric(), fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.speed_test_unit_mbps),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** "48,6 Mbps" en grand sous le compteur ; un tiret tant que rien n'est mesuré. */
@Composable
private fun SpeedValue(mbps: Double?, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(mbps?.let(::formatMbps) ?: "—", style = MaterialTheme.typography.displaySmall, color = color)
        Text(
            stringResource(R.string.speed_test_unit_mbps),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun StartButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun IconTile(
    icon: ImageVector,
    container: Color,
    content: Color,
    size: Int,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size((size * 0.55f).dp)) }
}

@Composable
private fun MobileWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.speed_test_mobile_dialog_title)) },
        text = {
            Text(
                stringResource(R.string.speed_test_mobile_dialog_body, RunSpeedTestUseCase.MOBILE_MAX_BYTES.toReadableSize()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.speed_test_mobile_dialog_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val SpeedTestPhase.labelRes: Int
    get() = when (this) {
        SpeedTestPhase.PING -> R.string.speed_test_phase_ping
        SpeedTestPhase.DOWNLOAD -> R.string.speed_test_phase_download
        SpeedTestPhase.UPLOAD -> R.string.speed_test_phase_upload
    }

/** Bleu pour le téléchargement, orange pour l'envoi, comme les tuiles du résultat. */
@Composable
private fun SpeedTestPhase.color(): Color = when (this) {
    SpeedTestPhase.PING, SpeedTestPhase.DOWNLOAD -> DataColors.wifi
    SpeedTestPhase.UPLOAD -> DataColors.mobile
}

/** "Wi‑Fi", "Ooredoo · 5G", à défaut "Données mobiles". */
@Composable
private fun SpeedTestResult.networkLabel(): String = when {
    onWifi -> stringResource(R.string.speed_test_network_wifi)
    operatorName == null && generation == null -> stringResource(R.string.speed_test_network_mobile)
    else -> listOfNotNull(operatorName, generation?.label).joinToString(" · ")
}

@Composable
private fun SpeedTestResult.badgeLabel(): String = when {
    onWifi -> stringResource(R.string.speed_test_badge_wifi)
    else -> generation?.label ?: stringResource(R.string.speed_test_badge_mobile)
}

private val PreviewServer = SpeedTestServer("Agence Tunisienne d’Internet", "Belvédère")

private object SpeedTestPreviewData {
    private const val NOW = 1_792_500_000_000L
    val history = listOf(
        SpeedTestResult(48.6, 12.3, 18, NOW - 60_000, onWifi = true, server = PreviewServer),
        SpeedTestResult(46.1, 11.8, 21, NOW - 5 * 3_600_000, onWifi = true, server = PreviewServer),
        SpeedTestResult(82.7, 24.0, 32, NOW - 20 * 3_600_000, onWifi = false, operatorName = "Ooredoo", generation = NetworkGeneration.G5),
    )
    val now = NOW
}

@ScreenPreviews
@Composable
private fun SpeedTestIdlePreview() {
    PreviewSurface(padded = false) {
        SpeedTestContent(SpeedTestState(isOnWifi = true, history = SpeedTestPreviewData.history.drop(1)), {}, nowMillis = SpeedTestPreviewData.now)
    }
}

@ScreenPreviews
@Composable
private fun SpeedTestRunningPreview() {
    PreviewSurface(padded = false) {
        SpeedTestContent(
            SpeedTestState(
                isOnWifi = true,
                progress = SpeedTestProgress(SpeedTestPhase.DOWNLOAD, 0.55f, currentMbps = 48.6, pingMs = 18, server = PreviewServer),
            ),
            {},
            nowMillis = SpeedTestPreviewData.now,
        )
    }
}

@ScreenPreviews
@Composable
private fun SpeedTestResultPreview() {
    PreviewSurface(padded = false) {
        SpeedTestContent(
            SpeedTestState(isOnWifi = true, result = SpeedTestPreviewData.history.first(), history = SpeedTestPreviewData.history),
            {},
            nowMillis = SpeedTestPreviewData.now,
        )
    }
}
