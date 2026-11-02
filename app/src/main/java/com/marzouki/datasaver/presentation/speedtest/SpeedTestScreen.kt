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
import kotlin.math.sin
import kotlin.math.cos
import com.marzouki.datasaver.ui.components.SPEED_GAUGE_MAX_MBPS
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
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

/**
 * Écran Débit du parcours : carte réseau, carte résultat (jauge, envoi et ping, échelle, verdict),
 * bouton de test, puis l'historique en tableau. Pendant un test, la jauge suit la mesure en direct.
 */
@Composable
private fun SpeedTestContent(
    state: SpeedTestState,
    onIntent: (SpeedTestIntent) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val progress = state.progress
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
    ) {
        if (progress != null) {
            RunningContent(progress, onCancel = { onIntent(SpeedTestIntent.CancelClicked) })
        } else {
            val latest = state.result ?: state.history.firstOrNull()
            ScreenHeader(
                overline = stringResource(R.string.speed_test_overline),
                title = stringResource(R.string.speed_test_title),
            )
            NetworkCard(state.isOnWifi)
            ResultCard(latest, fresh = state.result != null, nowMillis = nowMillis)
            StartButton(
                label = stringResource(if (latest == null) R.string.speed_test_start else R.string.speed_test_restart),
                icon = if (latest == null) Icons.Rounded.PlayArrow else Icons.Rounded.Refresh,
                onClick = { onIntent(SpeedTestIntent.StartClicked) },
            )
            val previous = state.history.filter { it != latest }
            if (previous.isNotEmpty()) HistoryCard(previous, nowMillis)
            Text(
                stringResource(R.string.speed_test_note, RunSpeedTestUseCase.MOBILE_MAX_BYTES.toReadableSize()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
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

/**
 * Carte résultat : pastille « Test terminé », jauge en arc, débit descendant en grand, envoi et
 * ping en tuiles, échelle Lent → Excellent et verdict. Sans test, la jauge reste vide avec une invite.
 */
@Composable
private fun ResultCard(result: SpeedTestResult?, fresh: Boolean, nowMillis: Long) {
    DataKeeperCard(shape = MaterialTheme.shapes.extraLarge, contentPadding = 20.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result != null) {
                val whenLabel = formatDayTime(result.atMillis, nowMillis)
                StatusPill(
                    if (fresh) {
                        stringResource(R.string.speed_test_done_overline, whenLabel)
                    } else {
                        stringResource(R.string.speed_test_last_at, whenLabel)
                    },
                )
            }
            ArcGauge(result?.downloadMbps)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    result?.let { formatMbps(it.downloadMbps) } ?: "—",
                    style = MaterialTheme.typography.displaySmall.numeric().copy(fontSize = 52.sp, lineHeight = 56.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.speed_test_unit_mbps),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
            Text(
                stringResource(R.string.speed_test_phase_download),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            if (result == null) {
                Text(
                    stringResource(R.string.speed_test_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniMetric(
                        Icons.Rounded.ArrowUpward,
                        stringResource(R.string.speed_test_phase_upload),
                        formatMbps(result.uploadMbps),
                        stringResource(R.string.speed_test_unit_mbps),
                    )
                    MiniMetric(
                        Icons.AutoMirrored.Rounded.ShowChart,
                        stringResource(R.string.speed_test_phase_ping),
                        result.pingMs.toString(),
                        stringResource(R.string.speed_test_unit_ms),
                    )
                }
                SpeedScale(result.downloadMbps)
                Text(
                    stringResource(SpeedTestRating.from(result.downloadMbps).labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.speed_test_your_network, result.networkLabel()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                result.server?.label?.let {
                    Text(
                        stringResource(R.string.speed_test_server, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Pastille verte « Test terminé · Aujourd'hui 14:12 ». */
@Composable
private fun StatusPill(text: String) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(DataColors.successContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = DataColors.success, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = DataColors.success)
    }
}

/** Jauge en demi-cercle : piste grise, arc violet jusqu'au débit (borné à 100 Mbps), pastille au bout. */
@Composable
private fun ArcGauge(mbps: Double?) {
    val target = ((mbps ?: 0.0) / SPEED_GAUGE_MAX_MBPS).coerceIn(0.0, 1.0).toFloat()
    val fraction by animateFloatAsState(target, label = "arcGauge")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val arc = MaterialTheme.colorScheme.primary
    val knobFill = MaterialTheme.colorScheme.surface
    Canvas(Modifier.size(width = 240.dp, height = 132.dp)) {
        val stroke = 18.dp.toPx()
        val radius = (size.width - stroke) / 2f
        val center = Offset(size.width / 2f, size.height - stroke / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        drawArc(track, 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (fraction > 0f) {
            drawArc(arc, 180f, 180f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val angle = Math.toRadians((180f + 180f * fraction).toDouble())
            val knob = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
            drawCircle(knobFill, radius = 11.dp.toPx(), center = knob)
            drawCircle(arc, radius = 11.dp.toPx(), center = knob, style = Stroke(5.dp.toPx()))
        }
    }
}

/** Tuile grise « Envoi 9,7 Mbps » ou « Ping 24 ms ». */
@Composable
private fun RowScope.MiniMetric(icon: ImageVector, label: String, value: String, unit: String) {
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium.numeric(), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
        }
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
                content = if (isOnWifi) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onTertiaryContainer,
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
private fun ColumnScope.RunningContent(progress: SpeedTestProgress, onCancel: () -> Unit) {
    val phaseColor = progress.phase.color()
    ScreenHeader(
        overline = stringResource(R.string.speed_test_running_overline),
        title = stringResource(progress.phase.labelRes),
        overlineColor = DataColors.wifi,
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

/** Historique : les tests précédents en tableau réglé (date, réseau, descendant, montant, ping). */
@Composable
private fun HistoryCard(previous: List<SpeedTestResult>, nowMillis: Long) {
    DataKeeperCard(contentPadding = 18.dp) {
        SectionTitle(stringResource(R.string.speed_test_history), Icons.Rounded.History) {
            Text(
                pluralStringResource(R.plurals.speed_test_history_count, previous.size, previous.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        HistoryHeaderRow()
        previous.forEach { item ->
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.padding(vertical = 12.dp)) { HistoryCells(item, nowMillis) }
        }
    }
}

private fun SpeedTestRating.labelRes(): Int = when (this) {
    SpeedTestRating.EXCELLENT -> R.string.speed_test_rating_excellent
    SpeedTestRating.GOOD -> R.string.speed_test_rating_good
    SpeedTestRating.FAIR -> R.string.speed_test_rating_fair
    SpeedTestRating.SLOW -> R.string.speed_test_rating_slow
}

private val HistoryColumns = listOf(1.7f, 1f, 0.9f, 0.8f, 0.7f)

@Composable
private fun HistoryHeaderRow() {
    Row(Modifier.padding(vertical = 6.dp)) {
        listOf(
            stringResource(R.string.speed_test_date),
            stringResource(R.string.speed_test_network),
            stringResource(R.string.speed_test_down),
            stringResource(R.string.speed_test_up),
            stringResource(R.string.speed_test_ping),
        ).forEachIndexed { index, label ->
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (index >= 1) TextAlign.End else TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.weight(HistoryColumns[index]),
            )
        }
    }
}

@Composable
private fun HistoryCells(result: SpeedTestResult, nowMillis: Long) {
    val valueStyle = MaterialTheme.typography.bodyMedium.numeric()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatDayTime(result.atMillis, nowMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(HistoryColumns[0]),
        )
        Box(Modifier.weight(HistoryColumns[1]), contentAlignment = Alignment.CenterEnd) {
            val (container, content) = if (result.onWifi) {
                MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            }
            Text(
                result.badgeLabel(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(container)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
        Text(formatMbps(result.downloadMbps), style = valueStyle, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(HistoryColumns[2]))
        Text(formatMbps(result.uploadMbps), style = valueStyle, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(HistoryColumns[3]))
        Text(result.pingMs.toString(), style = valueStyle, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(HistoryColumns[4]))
    }
}

/** Échelle en quatre segments égaux (Lent, Correct, Bon, Excellent) : le segment atteint en violet, les précédents en violet pâle. */
@Composable
private fun SpeedScale(mbps: Double) {
    val labels = listOf(
        R.string.speed_test_scale_slow,
        R.string.speed_test_scale_fair,
        R.string.speed_test_scale_good,
        R.string.speed_test_scale_excellent,
    )
    val active = when (SpeedTestRating.from(mbps)) {
        SpeedTestRating.SLOW -> 0
        SpeedTestRating.FAIR -> 1
        SpeedTestRating.GOOD -> 2
        SpeedTestRating.EXCELLENT -> 3
    }
    val primary = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index == active -> primary
                                index < active -> primary.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index == active) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
