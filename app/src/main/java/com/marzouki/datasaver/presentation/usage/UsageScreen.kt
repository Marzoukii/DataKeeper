package com.marzouki.datasaver.presentation.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.UsageAccessRequiredCard
import com.marzouki.datasaver.presentation.common.formatBytes
import com.marzouki.datasaver.presentation.common.openUsageAccessSettings
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun UsageScreen(modifier: Modifier = Modifier, viewModel: UsageViewModel = hiltViewModel()) {
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
    UsageContent(state, viewModel::onIntent, modifier)
}

@Composable
private fun UsageContent(state: UsageState, onIntent: (UsageIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            !state.hasUsageAccess -> UsageAccessRequiredCard(onGrantClick = { onIntent(UsageIntent.GrantAccessClicked) })
            state.usage != null -> UsageSummary(state.usage)
            state.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed -> Text(stringResource(R.string.usage_load_failed), color = MaterialTheme.colorScheme.error)
        }
        if (state.hasUsageAccess) {
            TextButton(onClick = { onIntent(UsageIntent.Refresh) }) { Text(stringResource(R.string.action_refresh)) }
        }
    }
}

@Composable
private fun UsageSummary(usage: DataUsage) {
    val wifiColor = MaterialTheme.colorScheme.primary
    val mobileColor = MaterialTheme.colorScheme.tertiary
    val wifiPercent = (usage.wifiShare * 100).roundToInt()
    val since = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(usage.period.startMillis))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.usage_period, since), style = MaterialTheme.typography.labelLarge)
            Text(formatBytes(usage.totalBytes), style = MaterialTheme.typography.displaySmall)
            SplitBar(usage.wifiBytes, usage.mobileBytes, wifiColor, mobileColor)
            UsageRow(
                iconRes = R.drawable.ic_wifi, color = wifiColor,
                label = stringResource(R.string.usage_wifi), bytes = usage.wifiBytes, percent = wifiPercent,
            )
            UsageRow(
                iconRes = R.drawable.ic_cell, color = mobileColor,
                label = stringResource(R.string.usage_mobile), bytes = usage.mobileBytes,
                percent = if (usage.totalBytes == 0L) 0 else 100 - wifiPercent,
            )
        }
    }
    if (usage.totalBytes > 0) {
        Text(stringResource(R.string.usage_insight, wifiPercent), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SplitBar(wifiBytes: Long, mobileBytes: Long, wifiColor: Color, mobileColor: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (wifiBytes > 0) Box(Modifier.weight(wifiBytes.toFloat()).fillMaxSize().background(wifiColor))
        if (mobileBytes > 0) Box(Modifier.weight(mobileBytes.toFloat()).fillMaxSize().background(mobileColor))
    }
}

@Composable
private fun UsageRow(iconRes: Int, color: Color, label: String, bytes: Long, percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("${formatBytes(bytes)} · $percent %", style = MaterialTheme.typography.bodyLarge)
    }
}
