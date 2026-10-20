package com.marzouki.datasaver.presentation.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.UsageAccessRequiredCard
import com.marzouki.datasaver.presentation.common.openUsageAccessSettings
import com.marzouki.datasaver.ui.components.AppUsageRow
import com.marzouki.datasaver.ui.components.DailyStackedBars
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.HeaderIconButton
import com.marzouki.datasaver.ui.components.PlaceholderCard
import com.marzouki.datasaver.ui.components.PreviewData
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.QuotaSummaryCard
import com.marzouki.datasaver.ui.components.ScreenHeader
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.UsageSplitCard
import com.marzouki.datasaver.ui.components.displayName
import com.marzouki.datasaver.ui.components.formatMonthYear
import com.marzouki.datasaver.ui.components.rememberAppIcon
import com.marzouki.datasaver.ui.theme.Spacing

@Composable
fun UsageScreen(
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(UsageIntent.Refresh)
        onPauseOrDispose { }
    }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            UsageEffect.OpenUsageAccessSettings -> openUsageAccessSettings(context)
        }
    }
    UsageContent(state, viewModel::onIntent, onOpenQuota, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageContent(
    state: UsageState,
    onIntent: (UsageIntent) -> Unit,
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val period = state.period ?: MonthPeriod.current()
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
                else -> dashboard(state, usage, period, onOpenQuota)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboard(
    state: UsageState,
    usage: DataUsage,
    period: MonthPeriod,
    onOpenQuota: () -> Unit,
) {
    item {
        if (usage.totalBytes == 0L) EmptyUsageCard() else UsageSplitCard(usage.wifiBytes, usage.mobileBytes)
    }
    item { QuotaSummaryCard(state.quotaStatus, period, onClick = onOpenQuota) }
    if (state.dailyUsage.isNotEmpty()) {
        item {
            DataKeeperCard {
                DailyStackedBars(state.dailyUsage, title = stringResource(R.string.usage_last_7_days))
            }
        }
    }
    if (state.topMobileApps.isNotEmpty()) {
        item {
            DataKeeperCard {
                Text(stringResource(R.string.usage_top_apps_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.usage_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                dailyUsage = PreviewData.days,
                topMobileApps = PreviewData.topApps,
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
