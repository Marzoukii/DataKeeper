package com.marzouki.datasaver.presentation.usage

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.NetworkGeneration
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.UsageAccessRequiredCard
import com.marzouki.datasaver.presentation.common.openAppDetailsSettings
import com.marzouki.datasaver.presentation.common.openUsageAccessSettings
import com.marzouki.datasaver.ui.components.AppUsageRow
import com.marzouki.datasaver.ui.components.DailyStackedBars
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.HeaderIconButton
import com.marzouki.datasaver.ui.components.PlaceholderCard
import com.marzouki.datasaver.ui.components.PreviewData
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.QuotaSummaryCard
import com.marzouki.datasaver.ui.components.SavingsAndPlanRow
import com.marzouki.datasaver.ui.components.ScreenHeader
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.ThinBar
import com.marzouki.datasaver.ui.components.UsageSplitCard
import com.marzouki.datasaver.ui.components.dayName
import com.marzouki.datasaver.ui.components.displayName
import com.marzouki.datasaver.ui.components.formatDayMonth
import com.marzouki.datasaver.ui.components.formatMonthYear
import com.marzouki.datasaver.ui.components.rememberAppIcon
import com.marzouki.datasaver.ui.components.toReadableSize
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric
import kotlin.math.roundToInt

@Composable
fun UsageScreen(
    snackbarHostState: SnackbarHostState,
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    // Après un second refus, Android n'affiche plus la demande : on ouvre alors les infos de l'appli.
    var phonePermissionBlocked by rememberSaveable { mutableStateOf(false) }
    val phonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        phonePermissionBlocked = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)
        viewModel.onIntent(UsageIntent.PhonePermissionResult)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(UsageIntent.Refresh)
        onPauseOrDispose { }
    }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            UsageEffect.OpenUsageAccessSettings -> openUsageAccessSettings(context)
        }
    }
    UsageContent(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenQuota = onOpenQuota,
        onRequestPhonePermission = {
            if (phonePermissionBlocked) {
                openAppDetailsSettings(context)
            } else {
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageContent(
    state: UsageState,
    onIntent: (UsageIntent) -> Unit,
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestPhonePermission: () -> Unit = {},
) {
    val period = state.period ?: MonthPeriod.current()
    // Jour du graphique dont le détail est affiché en feuille de bas de page.
    var selectedDayMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    PullToRefreshBox(
        isRefreshing = state.isLoading && state.usage != null,
        onRefresh = { onIntent(UsageIntent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
        ) {
            item {
                ScreenHeader(
                    overline = stringResource(
                        R.string.usage_overline,
                        formatMonthYear(period.endMillis),
                        period.currentDay,
                        period.totalDays,
                    ),
                    title = stringResource(R.string.usage_title),
                ) {
                    HeaderIconButton(Icons.Rounded.Settings, stringResource(R.string.usage_settings), onOpenQuota)
                }
            }
            val usage = state.usage
            when {
                !state.hasUsageAccess -> item {
                    UsageAccessRequiredCard(onGrantClick = { onIntent(UsageIntent.GrantAccessClicked) })
                }
                usage == null && state.isLoading -> {
                    item { PlaceholderCard(height = 120.dp) }
                    item { PlaceholderCard(height = 56.dp) }
                    item { PlaceholderCard(height = 140.dp) }
                }
                usage == null -> item { LoadFailedCard(onRetry = { onIntent(UsageIntent.Refresh) }) }
                else -> dashboard(
                    state, usage, period, onOpenQuota, onRequestPhonePermission,
                    selectedDayMillis = selectedDayMillis,
                    onDayClick = { selectedDayMillis = it.dayStartMillis },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
    // Le jour peut sortir de la fenêtre des 7 jours après minuit : la feuille se ferme alors seule.
    state.dailyUsage.firstOrNull { it.dayStartMillis == selectedDayMillis }?.let { day ->
        DayDetailSheet(day, onDismiss = { selectedDayMillis = null })
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboard(
    state: UsageState,
    usage: DataUsage,
    period: MonthPeriod,
    onOpenQuota: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    selectedDayMillis: Long?,
    onDayClick: (DayUsage) -> Unit,
) {
    item {
        if (usage.totalBytes == 0L) {
            EmptyUsageCard()
        } else {
            val operatorMobileBytes = state.operatorMobileBytes
            UsageSplitCard(
                wifiBytes = usage.wifiBytes,
                mobileBytes = operatorMobileBytes ?: usage.mobileBytes,
                mobileNote = operatorMobileBytes?.let {
                    state.operatorName?.let { name -> stringResource(R.string.usage_mobile_from_balance_operator, name) }
                        ?: stringResource(R.string.usage_mobile_from_balance)
                },
                operatorName = state.operatorName,
                networkLabel = state.networkGeneration?.label,
                onShowNetwork = if (state.hasPhonePermission) null else onRequestPhonePermission,
            )
        }
    }
    val heavyApp = state.heavyApp
    val quota = state.quota
    if (heavyApp != null && quota != null) {
        item { HeavyAppBanner(heavyApp, quota.limitBytes) }
    }
    item { QuotaSummaryCard(state.quotaStatus, period, onClick = onOpenQuota) }
    item {
        SavingsAndPlanRow(
            wifiBytes = usage.wifiBytes,
            pricePerGb = state.pricePerGb,
            status = state.quotaStatus,
            quota = state.quota,
            balance = state.quota?.balanceIn(period),
            onOpenQuota = onOpenQuota,
            operatorName = state.operatorName,
            resetMillis = period.nextStartMillis,
        )
    }
    item {
        DataKeeperCard {
            if (state.dailyUsage.any { it.totalBytes > 0 }) {
                DailyStackedBars(
                    state.dailyUsage,
                    title = stringResource(R.string.usage_last_7_days),
                    selectedDayMillis = selectedDayMillis,
                    onDayClick = onDayClick,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.usage_day_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(stringResource(R.string.usage_last_7_days), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                AnalyzingBox(stringResource(R.string.usage_analyzing_days))
            }
        }
    }
    item {
        DataKeeperCard {
            Text(stringResource(R.string.usage_top_apps_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.topMobileApps.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                AnalyzingBox(stringResource(R.string.usage_analyzing_apps))
            } else {
                val mobileTotal = usage.mobileBytes.coerceAtLeast(1L).toFloat()
                Column {
                    state.topMobileApps.forEach { app ->
                        AppUsageRow(
                            icon = rememberAppIcon(app.packageName),
                            name = app.displayName(),
                            bytes = app.mobileBytes,
                            fraction = app.mobileBytes / mobileTotal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyUsageCard() {
    DataKeeperCard(shape = MaterialTheme.shapes.extraLarge) {
        Text(stringResource(R.string.usage_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        AnalyzingBox(stringResource(R.string.usage_empty_body))
    }
}

/** Section encore vide : sablier et message d'attente plutôt qu'un trou dans le tableau de bord. */
@Composable
private fun AnalyzingBox(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.HourglassTop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.usage_analyzing_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Bandeau discret : une seule appli a mangé au moins la moitié du forfait mobile. */
@Composable
private fun HeavyAppBanner(app: AppDataUsage, limitBytes: Long) {
    val name = app.displayName()
    DataKeeperCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DataUsage, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.usage_heavy_app_title, name), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(
                        R.string.usage_heavy_app_body,
                        name,
                        app.mobileBytes.toReadableSize(),
                        (app.mobileBytes * 100 / limitBytes).toInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall.numeric(),
                )
            }
        }
    }
}

/** Détail d'un jour touché dans le graphique : total, puis Wi-Fi et mobile avec leur part. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(day: DayUsage, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                "${dayName(day.dayStartMillis).replaceFirstChar { it.uppercase() }} ${formatDayMonth(day.dayStartMillis)}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.usage_day_total, day.totalBytes.toReadableSize()),
                style = MaterialTheme.typography.bodyMedium.numeric(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            DayShareRow(stringResource(R.string.usage_wifi), day.wifiBytes, day.totalBytes, DataColors.wifi)
            Spacer(Modifier.height(16.dp))
            DayShareRow(stringResource(R.string.usage_mobile), day.mobileBytes, day.totalBytes, DataColors.mobile)
        }
    }
}

@Composable
private fun DayShareRow(label: String, bytes: Long, totalBytes: Long, color: Color) {
    val share = if (totalBytes == 0L) 0f else bytes.toFloat() / totalBytes
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.usage_day_share, bytes.toReadableSize(), (share * 100).roundToInt()),
                style = MaterialTheme.typography.labelLarge.numeric(),
            )
        }
        ThinBar(share, color, height = 8.dp)
    }
}

@Composable
private fun LoadFailedCard(onRetry: () -> Unit) {
    DataKeeperCard(
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(stringResource(R.string.usage_load_failed), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onRetry, shape = CircleShape) { Text(stringResource(R.string.action_retry)) }
    }
}

@ScreenPreviews
@Composable
private fun UsageContentPreview() {
    PreviewSurface(padded = false) {
        UsageContent(
            state = UsageState(
                isLoading = false,
                usage = PreviewData.usage,
                period = PreviewData.period,
                quota = PreviewData.quota,
                quotaUsedBytes = PreviewData.quotaStatus.usedBytes,
                dailyUsage = PreviewData.days,
                topMobileApps = PreviewData.topApps,
                pricePerGb = PreviewData.pricePerGb,
                operatorName = "Ooredoo",
                networkGeneration = NetworkGeneration.G4,
            ),
            onIntent = {},
            onOpenQuota = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun UsageContentLoadingPreview() {
    PreviewSurface(padded = false) {
        UsageContent(UsageState(isLoading = true, period = PreviewData.period), onIntent = {}, onOpenQuota = {})
    }
}

@ScreenPreviews
@Composable
private fun UsageContentEmptyPreview() {
    PreviewSurface(padded = false) {
        UsageContent(
            UsageState(isLoading = false, usage = PreviewData.usage.copy(wifiBytes = 0, mobileBytes = 0), period = PreviewData.period),
            onIntent = {},
            onOpenQuota = {},
        )
    }
}
