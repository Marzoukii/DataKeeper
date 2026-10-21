package com.marzouki.datasaver.presentation.quota

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.ForecastOutlook
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaForecast
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.UsageAccessRequiredCard
import com.marzouki.datasaver.presentation.common.openUsageAccessSettings
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.ForecastChart
import com.marzouki.datasaver.ui.components.InfoPill
import com.marzouki.datasaver.ui.components.PreviewData
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.QuotaRing
import com.marzouki.datasaver.ui.components.ScreenHeader
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.SettingDivider
import com.marzouki.datasaver.ui.components.SettingRow
import com.marzouki.datasaver.ui.components.formatDayMonth
import com.marzouki.datasaver.ui.components.formatDayOrdinal
import com.marzouki.datasaver.ui.components.formatDinars
import com.marzouki.datasaver.ui.components.formatShortDate
import com.marzouki.datasaver.ui.components.toReadableSize
import com.marzouki.datasaver.ui.components.toReadableSizeParts
import com.marzouki.datasaver.ui.theme.SpaceGrotesk
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric
import java.math.BigDecimal
import java.util.Calendar
import kotlin.math.roundToInt

private val PRESETS_GB = listOf(2, 5, 10, 20)
private const val SLIDER_MIN_GB = 1f
private const val SLIDER_MAX_GB = 50f
private const val DEFAULT_GB = 5f
private const val BYTES_PER_GB = 1_000_000_000f

@Composable
fun QuotaScreen(
    snackbarHostState: SnackbarHostState,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuotaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(QuotaIntent.Refresh)
        onPauseOrDispose { }
    }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            QuotaEffect.OpenUsageAccessSettings -> openUsageAccessSettings(context)
            QuotaEffect.RequestNotificationPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            is QuotaEffect.ShowMessage -> snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
        }
    }
    QuotaContent(state, viewModel::onIntent, onOpenDownloads, modifier)
}

@Composable
private fun QuotaContent(
    state: QuotaState,
    onIntent: (QuotaIntent) -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOtherDialog by rememberSaveable { mutableStateOf(false) }
    var showCycleDialog by rememberSaveable { mutableStateOf(false) }
    var showPriceDialog by rememberSaveable { mutableStateOf(false) }
    val period = state.period ?: MonthPeriod.current(cycleStartDay = state.cycleStartDay)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
    ) {
        item {
            ScreenHeader(
                overline = stringResource(R.string.quota_overline, formatDayMonth(period.nextStartMillis)),
                title = stringResource(R.string.quota_title),
            )
        }
        if (!state.hasUsageAccess) {
            item { UsageAccessRequiredCard(onGrantClick = { onIntent(QuotaIntent.GrantAccessClicked) }) }
        }
        item { QuotaGaugeCard(state.quota, state.status, period) }
        state.forecast?.let { forecast -> item { ForecastCard(forecast) } }
        item { PlanCard(state, onIntent, onOtherClick = { showOtherDialog = true }) }
        item {
            SettingsCard(
                state,
                onIntent,
                onCycleClick = { showCycleDialog = true },
                onPriceClick = { showPriceDialog = true },
            )
        }
        item { TipBox(onClick = onOpenDownloads) }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showOtherDialog) {
        OtherQuotaDialog(
            state = state,
            onIntent = onIntent,
            onDismiss = { showOtherDialog = false },
        )
    }
    if (showCycleDialog) {
        CycleStartDialog(
            selected = state.cycleStartDay,
            onSelect = {
                onIntent(QuotaIntent.CycleStartDaySelected(it))
                showCycleDialog = false
            },
            onDismiss = { showCycleDialog = false },
        )
    }
    if (showPriceDialog) {
        PricePerGbDialog(
            current = state.pricePerGb,
            onSave = {
                onIntent(QuotaIntent.PricePerGbSaved(it))
                showPriceDialog = false
            },
            onDismiss = { showPriceDialog = false },
        )
    }
}

@Composable
private fun QuotaGaugeCard(quota: DataQuota?, status: QuotaStatus?, period: MonthPeriod) {
    DataKeeperCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val percent = status?.let { (it.usedRatio * 100).roundToInt() } ?: 0
            val ringDescription = stringResource(R.string.quota_ring_description, percent)
            QuotaRing(
                used = status?.usedBytes ?: 0L,
                quota = quota?.limitBytes ?: 0L,
                size = 200.dp,
                stroke = 16.dp,
                markerFraction = (quota?.alertThresholdPercent ?: DataQuota.DEFAULT_ALERT_THRESHOLD_PERCENT) / 100f,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = ringDescription },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        status?.usedBytes?.toReadableSize() ?: stringResource(R.string.quota_no_value),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        when {
                            quota == null -> stringResource(R.string.quota_none)
                            status == null -> quota.limitBytes.toReadableSize()
                            else -> stringResource(R.string.quota_of_total, quota.limitBytes.toReadableSize(), percent)
                        },
                        style = MaterialTheme.typography.bodyMedium.numeric(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (status != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status.isExceeded) {
                        InfoPill(
                            stringResource(R.string.quota_exceeded_chip, status.exceededBytes.toReadableSize()),
                            MaterialTheme.colorScheme.errorContainer,
                        )
                    } else {
                        InfoPill(
                            stringResource(R.string.quota_remaining_chip, status.remainingBytes.toReadableSize()),
                            MaterialTheme.colorScheme.tertiaryContainer,
                        )
                        InfoPill(
                            stringResource(
                                R.string.quota_per_day_chip,
                                (status.remainingBytes / period.remainingDays).toReadableSize(),
                            ),
                            MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Prévision de fin de cycle : verdict, conseil de rythme quotidien et courbe. */
@Composable
private fun ForecastCard(forecast: QuotaForecast) {
    val lastDayMillis = Calendar.getInstance().apply {
        timeInMillis = forecast.period.nextStartMillis
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis
    val projected = forecast.projectedBytes.toReadableSize()
    val endDate = formatDayMonth(lastDayMillis)
    val safeDaily = forecast.safeDailyBytes.toReadableSize()
    val (badgeRes, badgeColor) = when (forecast.outlook) {
        ForecastOutlook.COMFORTABLE -> R.string.forecast_badge_comfortable to MaterialTheme.colorScheme.primaryContainer
        ForecastOutlook.TIGHT -> R.string.forecast_badge_tight to MaterialTheme.colorScheme.tertiaryContainer
        ForecastOutlook.OVER_QUOTA -> R.string.forecast_badge_over to MaterialTheme.colorScheme.errorContainer
        ForecastOutlook.EXCEEDED -> R.string.forecast_badge_exceeded to MaterialTheme.colorScheme.errorContainer
    }
    val body = when (forecast.outlook) {
        ForecastOutlook.COMFORTABLE -> stringResource(R.string.forecast_body_comfortable, projected, endDate)
        ForecastOutlook.TIGHT -> stringResource(R.string.forecast_body_tight, projected, endDate, safeDaily)
        ForecastOutlook.OVER_QUOTA -> stringResource(R.string.forecast_body_over, projected, endDate, safeDaily)
        ForecastOutlook.EXCEEDED -> stringResource(R.string.forecast_body_exceeded, projected, endDate)
    }

    DataKeeperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.forecast_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            InfoPill(stringResource(badgeRes), badgeColor)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                append(body)
                val start = body.indexOf(projected)
                if (start >= 0) {
                    addStyle(SpanStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold), start, start + projected.length)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        ForecastChart(
            cumulativeBytes = forecast.cumulativeBytes,
            totalDays = forecast.period.totalDays,
            projectedBytes = forecast.projectedBytes,
            limitBytes = forecast.quota.limitBytes,
            alertPercent = forecast.quota.alertThresholdPercent,
            startLabel = formatShortDate(forecast.period.startMillis),
            endLabel = formatShortDate(lastDayMillis),
            description = stringResource(
                R.string.forecast_chart_description,
                forecast.usedBytes.toReadableSize(),
                projected,
                endDate,
                forecast.quota.limitBytes.toReadableSize(),
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanCard(state: QuotaState, onIntent: (QuotaIntent) -> Unit, onOtherClick: () -> Unit) {
    val inputBytes = QuotaViewModel.parseGigabytes(state.input)
    val currentGb = (inputBytes ?: state.quota?.limitBytes)?.let { it / BYTES_PER_GB }
    val sliderDescription = stringResource(R.string.quota_slider_description)

    DataKeeperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.quota_plan_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            val value = currentGb?.let { (it * BYTES_PER_GB).toLong().toReadableSizeParts() }
            Text(
                buildAnnotatedString {
                    append(value?.value ?: stringResource(R.string.quota_no_value))
                    if (value != null) {
                        withStyle(SpanStyle(fontSize = 15.sp)) { append(" ${value.unit}") }
                    }
                },
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceGrotesk, fontSize = 22.sp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = (currentGb ?: DEFAULT_GB).coerceIn(SLIDER_MIN_GB, SLIDER_MAX_GB),
            onValueChange = { onIntent(QuotaIntent.InputChanged(it.roundToInt().toString())) },
            onValueChangeFinished = { onIntent(QuotaIntent.Save) },
            valueRange = SLIDER_MIN_GB..SLIDER_MAX_GB,
            steps = (SLIDER_MAX_GB - SLIDER_MIN_GB).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0f),
            ),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .semantics { contentDescription = sliderDescription },
        )
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS_GB.forEach { gb ->
                FilterChip(
                    selected = inputBytes == gb * BYTES_PER_GB.toLong(),
                    onClick = {
                        onIntent(QuotaIntent.PresetSelected(gb))
                        onIntent(QuotaIntent.Save)
                    },
                    label = { Text(stringResource(R.string.quota_preset, gb), style = MaterialTheme.typography.labelLarge.numeric()) },
                    shape = CircleShape,
                    colors = chipColors,
                )
            }
            FilterChip(
                selected = inputBytes != null && PRESETS_GB.none { inputBytes == it * BYTES_PER_GB.toLong() },
                onClick = onOtherClick,
                label = { Text(stringResource(R.string.quota_preset_other), style = MaterialTheme.typography.labelLarge) },
                shape = CircleShape,
                colors = chipColors,
            )
        }
        if (state.quota != null) {
            TextButton(
                onClick = { onIntent(QuotaIntent.RemoveQuota) },
                shape = CircleShape,
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(stringResource(R.string.quota_remove)) }
        }
    }
}

@Composable
private fun SettingsCard(
    state: QuotaState,
    onIntent: (QuotaIntent) -> Unit,
    onCycleClick: () -> Unit,
    onPriceClick: () -> Unit,
) {
    val quota = state.quota
    val threshold = quota?.alertThresholdPercent ?: DataQuota.DEFAULT_ALERT_THRESHOLD_PERCENT
    DataKeeperCard(contentPadding = 0.dp) {
        Spacer(Modifier.height(4.dp))
        SettingRow(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.quota_alert_title, threshold),
            subtitle = if (quota != null) {
                stringResource(R.string.quota_alert_subtitle, quota.alertThresholdBytes.toReadableSize())
            } else {
                stringResource(R.string.quota_alert_subtitle_no_quota)
            },
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Switch(
                checked = quota?.alertEnabled == true,
                onCheckedChange = { onIntent(QuotaIntent.AlertToggled(it)) },
                enabled = quota != null,
            )
        }
        SettingDivider()
        SettingRow(
            icon = Icons.Rounded.CalendarMonth,
            title = stringResource(R.string.quota_cycle_title),
            subtitle = stringResource(
                R.string.quota_cycle_subtitle,
                formatDayOrdinal(state.cycleStartDay),
                formatDayMonth((state.period ?: MonthPeriod.current(cycleStartDay = state.cycleStartDay)).startMillis),
            ),
            onClick = onCycleClick,
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.quota_cycle_choose),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingDivider()
        val price = state.pricePerGb
        SettingRow(
            icon = Icons.Rounded.Payments,
            title = stringResource(R.string.quota_price_title),
            subtitle = if (price != null) {
                stringResource(R.string.quota_price_subtitle, formatDinars(price.millimes.toLong()))
            } else {
                stringResource(R.string.quota_price_subtitle_none)
            },
            onClick = onPriceClick,
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.quota_price_choose),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PricePerGbDialog(current: PricePerGb?, onSave: (PricePerGb?) -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf(current?.let { millimesToInput(it.millimes) }.orEmpty()) }
    var error by rememberSaveable { mutableStateOf(false) }
    val submit = {
        val price = PricePerGb.parse(input)
        if (price == null) error = true else onSave(price)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quota_price_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.quota_price_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = false
                    },
                    label = { Text(stringResource(R.string.quota_price_input_label)) },
                    suffix = { Text(stringResource(R.string.unit_dinar)) },
                    isError = error,
                    supportingText = if (error) {
                        { Text(stringResource(R.string.quota_price_input_error)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = submit) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.quota_price_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

/** 2500 → "2,5", 2000 → "2". */
private fun millimesToInput(millimes: Int): String =
    BigDecimal(millimes).movePointLeft(3).stripTrailingZeros().toPlainString().replace('.', ',')

@Composable
private fun TipBox(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(stringResource(R.string.quota_tip_label)) }
                append(' ')
                append(stringResource(R.string.quota_tip_body))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun OtherQuotaDialog(state: QuotaState, onIntent: (QuotaIntent) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quota_plan_title)) },
        text = {
            OutlinedTextField(
                value = state.input,
                onValueChange = { onIntent(QuotaIntent.InputChanged(it)) },
                label = { Text(stringResource(R.string.quota_input_label)) },
                suffix = { Text(stringResource(R.string.unit_gb)) },
                isError = state.inputError,
                supportingText = if (state.inputError) {
                    { Text(stringResource(R.string.quota_input_error)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onIntent(QuotaIntent.Save)
                    if (QuotaViewModel.parseGigabytes(state.input) != null) onDismiss()
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onIntent(QuotaIntent.Save)
                if (QuotaViewModel.parseGigabytes(state.input) != null) onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@ScreenPreviews
@Composable
private fun QuotaContentPreview() {
    PreviewSurface(padded = false) {
        QuotaContent(
            state = QuotaState(
                quota = PreviewData.quota,
                usedMobileBytes = PreviewData.quotaStatus.usedBytes,
                input = "5",
                period = PreviewData.period,
                pricePerGb = PreviewData.pricePerGb,
                dailyMobileBytes = PreviewData.cycleDailyMobile,
            ),
            onIntent = {},
            onOpenDownloads = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun QuotaContentExceededPreview() {
    PreviewSurface(padded = false) {
        QuotaContent(
            state = QuotaState(
                quota = PreviewData.quota,
                usedMobileBytes = PreviewData.quotaExceededStatus.usedBytes,
                input = "5",
                period = PreviewData.period,
                dailyMobileBytes = PreviewData.cycleDailyMobile.map { it * 54 / 32 },
            ),
            onIntent = {},
            onOpenDownloads = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun QuotaContentNoQuotaPreview() {
    PreviewSurface(padded = false) {
        QuotaContent(state = QuotaState(period = PreviewData.period), onIntent = {}, onOpenDownloads = {})
    }
}
