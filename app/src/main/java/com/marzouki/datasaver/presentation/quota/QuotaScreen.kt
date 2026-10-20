package com.marzouki.datasaver.presentation.quota

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.presentation.common.UsageAccessRequiredCard
import com.marzouki.datasaver.presentation.common.formatBytes
import com.marzouki.datasaver.presentation.common.openUsageAccessSettings
import kotlin.math.roundToInt

private val PRESETS_GB = listOf(1, 5, 10, 20, 50)

@Composable
fun QuotaScreen(
    snackbarHostState: SnackbarHostState,
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
    QuotaContent(state, viewModel::onIntent, modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuotaContent(state: QuotaState, onIntent: (QuotaIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.hasUsageAccess) {
            UsageAccessRequiredCard(onGrantClick = { onIntent(QuotaIntent.GrantAccessClicked) })
        }
        state.status?.let { status ->
            if (status.isAlertReached) AlertBanner(status)
            QuotaProgressCard(status)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.quota_edit_title), style = MaterialTheme.typography.titleMedium)
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
                    keyboardActions = KeyboardActions(onDone = { onIntent(QuotaIntent.Save) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS_GB.forEach { gb ->
                        FilterChip(
                            selected = state.input == gb.toString(),
                            onClick = { onIntent(QuotaIntent.PresetSelected(gb)) },
                            label = { Text("$gb ${stringResource(R.string.unit_gb)}") },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { onIntent(QuotaIntent.Save) }) { Text(stringResource(R.string.action_save)) }
                    if (state.quota != null) {
                        TextButton(onClick = { onIntent(QuotaIntent.RemoveQuota) }) {
                            Text(stringResource(R.string.quota_remove))
                        }
                    }
                }
                Text(
                    stringResource(R.string.quota_alert_explanation, DataQuota.DEFAULT_ALERT_THRESHOLD_PERCENT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuotaProgressCard(status: QuotaStatus) {
    val percent = (status.usedRatio * 100).roundToInt()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.quota_month_title), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.quota_used_of, formatBytes(status.usedBytes), formatBytes(status.quota.limitBytes)),
                style = MaterialTheme.typography.headlineSmall,
            )
            LinearProgressIndicator(
                progress = { status.usedRatio.coerceIn(0f, 1f) },
                color = if (status.isAlertReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.quota_percent_remaining, percent, formatBytes(status.remainingBytes)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AlertBanner(status: QuotaStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Text(
                stringResource(
                    if (status.isExceeded) R.string.quota_banner_exceeded else R.string.quota_banner_alert,
                    (status.usedRatio * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
