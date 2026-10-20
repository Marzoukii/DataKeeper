package com.marzouki.datasaver.presentation.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.model.DownloadSchedule
import com.marzouki.datasaver.domain.model.DownloadState
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.presentation.common.CollectEffects
import java.text.DateFormat
import java.util.Date

@Composable
fun DownloadsScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is DownloadsEffect.Scheduled ->
                snackbarHostState.showSnackbar(resources.getString(R.string.downloads_scheduled, effect.fileName))
        }
    }
    DownloadsContent(state, viewModel::onIntent, modifier)
}

@Composable
private fun DownloadsContent(state: DownloadsState, onIntent: (DownloadsIntent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScheduleForm(state, onIntent) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.downloads_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (state.downloads.any { it.state.isFinished }) {
                    TextButton(onClick = { onIntent(DownloadsIntent.ClearFinished) }) {
                        Text(stringResource(R.string.downloads_clear_finished))
                    }
                }
            }
        }
        if (state.downloads.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.downloads_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.downloads, key = { it.id }) { download ->
            DownloadItem(download, onCancel = { onIntent(DownloadsIntent.Cancel(download.id)) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleForm(state: DownloadsState, onIntent: (DownloadsIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.downloads_form_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.downloads_form_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.url,
                onValueChange = { onIntent(DownloadsIntent.UrlChanged(it)) },
                label = { Text(stringResource(R.string.downloads_url_label)) },
                isError = state.urlError,
                supportingText = if (state.urlError) {
                    { Text(stringResource(R.string.downloads_url_error)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fileName,
                onValueChange = { onIntent(DownloadsIntent.FileNameChanged(it)) },
                label = { Text(stringResource(R.string.downloads_file_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadSchedule.entries.forEach { schedule ->
                    FilterChip(
                        selected = state.schedule == schedule,
                        onClick = { onIntent(DownloadsIntent.ScheduleSelected(schedule)) },
                        label = { Text(stringResource(schedule.labelRes)) },
                    )
                }
            }
            Button(onClick = { onIntent(DownloadsIntent.Submit) }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_wifi), contentDescription = null)
                Text(stringResource(R.string.downloads_submit), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun DownloadItem(download: ScheduledDownload, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(download.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        download.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!download.state.isFinished) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            }
            Text(
                statusText(download),
                style = MaterialTheme.typography.labelMedium,
                color = if (download.state == DownloadState.FAILED) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
            if (download.state == DownloadState.RUNNING) {
                val progress = download.progress
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun statusText(download: ScheduledDownload): String = when (download.state) {
    DownloadState.WAITING_FOR_WIFI -> {
        val start = download.earliestStartMillis?.takeIf { it > System.currentTimeMillis() }
        if (start != null) {
            stringResource(
                R.string.download_state_scheduled,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(start)),
            )
        } else {
            stringResource(R.string.download_state_waiting)
        }
    }
    DownloadState.RUNNING -> download.progress?.let { stringResource(R.string.download_state_running_progress, it) }
        ?: stringResource(R.string.download_state_running)
    DownloadState.SUCCEEDED -> stringResource(R.string.download_state_done)
    DownloadState.FAILED -> stringResource(R.string.download_state_failed)
    DownloadState.CANCELLED -> stringResource(R.string.download_state_cancelled)
}

private val DownloadState.isFinished: Boolean
    get() = this == DownloadState.SUCCEEDED || this == DownloadState.FAILED || this == DownloadState.CANCELLED

private val DownloadSchedule.labelRes: Int
    get() = when (this) {
        DownloadSchedule.AS_SOON_AS_WIFI -> R.string.schedule_asap
        DownloadSchedule.IN_ONE_HOUR -> R.string.schedule_one_hour
        DownloadSchedule.TONIGHT -> R.string.schedule_tonight
    }
