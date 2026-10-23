package com.marzouki.datasaver.presentation.downloads

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.DownloadSchedule
import com.marzouki.datasaver.domain.enums.DownloadState
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.presentation.common.CollectEffects
import com.marzouki.datasaver.ui.components.DataKeeperCard
import com.marzouki.datasaver.ui.components.DownloadItemCard
import com.marzouki.datasaver.ui.components.HeaderIconButton
import com.marzouki.datasaver.ui.components.InfoPill
import com.marzouki.datasaver.ui.components.PreviewData
import com.marzouki.datasaver.ui.components.PreviewSurface
import com.marzouki.datasaver.ui.components.ScreenHeader
import com.marzouki.datasaver.ui.components.ScreenPreviews
import com.marzouki.datasaver.ui.components.isFinished
import com.marzouki.datasaver.ui.components.toReadableSize
import com.marzouki.datasaver.ui.theme.Spacing
import com.marzouki.datasaver.ui.theme.numeric

private const val TAB_PENDING = 0
private const val TAB_AVAILABLE = 1

@Composable
fun DownloadsScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is DownloadsEffect.Scheduled -> {
                showAddSheet = false
                snackbarHostState.showSnackbar(resources.getString(R.string.downloads_scheduled, effect.fileName))
            }
        }
    }
    DownloadsContent(
        state = state,
        onIntent = viewModel::onIntent,
        showAddSheet = showAddSheet,
        onShowAddSheet = { showAddSheet = it },
        modifier = modifier,
    )
}

@Composable
private fun DownloadsContent(
    state: DownloadsState,
    onIntent: (DownloadsIntent) -> Unit,
    showAddSheet: Boolean,
    onShowAddSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_PENDING) }
    val pending = state.downloads.filter { it.state != DownloadState.SUCCEEDED }
    val available = state.downloads.filter { it.state == DownloadState.SUCCEEDED }
    val shown = if (selectedTab == TAB_PENDING) pending else available
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Le bas laisse la place au bouton flottant.
            contentPadding = PaddingValues(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ScreenHeader(
                    overline = stringResource(R.string.downloads_overline),
                    title = stringResource(R.string.downloads_title),
                ) {
                    if (state.downloads.any { it.state.isFinished }) {
                        HeaderIconButton(
                            Icons.Rounded.DeleteSweep,
                            stringResource(R.string.downloads_clear_finished),
                        ) { onIntent(DownloadsIntent.ClearFinished) }
                    }
                }
            }
            item { WifiStatusCard(state.isOnWifi, pending, available, Modifier.padding(top = 4.dp)) }
            item {
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    val colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    )
                    SegmentedButton(
                        selected = selectedTab == TAB_PENDING,
                        onClick = { selectedTab = TAB_PENDING },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = colors,
                    ) { Text(stringResource(R.string.downloads_tab_pending, pending.size)) }
                    SegmentedButton(
                        selected = selectedTab == TAB_AVAILABLE,
                        onClick = { selectedTab = TAB_AVAILABLE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = colors,
                    ) { Text(stringResource(R.string.downloads_tab_available, available.size)) }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        stringResource(
                            if (selectedTab == TAB_PENDING) R.string.downloads_empty_pending
                            else R.string.downloads_empty_available,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { download ->
                DownloadItemCard(
                    item = download,
                    onCancel = { onIntent(DownloadsIntent.Cancel(download.id)) },
                    modifier = Modifier.animateItem(),
                )
            }
            item { SavingTipsCard(Modifier.padding(top = 8.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = { onShowAddSheet(true) },
            expanded = fabExpanded,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.downloads_add_link), style = MaterialTheme.typography.labelLarge) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (showAddSheet) {
        AddLinkSheet(state, onIntent, onDismiss = { onShowAddSheet(false) })
    }
}

/** Carte d'état : pleine en bleu sur Wi-Fi, neutre en attente du Wi-Fi. */
@Composable
private fun WifiStatusCard(
    isOnWifi: Boolean,
    pending: List<ScheduledDownload>,
    available: List<ScheduledDownload>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (isOnWifi) colors.primary else colors.surfaceVariant
    val content = if (isOnWifi) colors.onPrimary else colors.onSurface
    val running = pending.count { it.state == DownloadState.RUNNING }
    val waiting = pending.count { it.state == DownloadState.WAITING_FOR_WIFI }
    val queuedBytes = pending.sumOf { it.totalBytes ?: 0L }
    val savedBytes = available.sumOf { it.totalBytes ?: 0L }

    DataKeeperCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = container,
        contentColor = content,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isOnWifi) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, contentDescription = null)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (isOnWifi) R.string.downloads_wifi_connected else R.string.downloads_wifi_waiting),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (running + waiting == 0) {
                        stringResource(R.string.downloads_queue_empty)
                    } else {
                        stringResource(R.string.downloads_queue_summary, running, waiting, queuedBytes.toReadableSize())
                    },
                    style = MaterialTheme.typography.bodyMedium.numeric(),
                    color = content.copy(alpha = 0.85f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (isOnWifi) R.string.downloads_mobile_used else R.string.downloads_wifi_waiting_body),
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            if (savedBytes > 0) {
                Spacer(Modifier.width(12.dp))
                InfoPill(
                    stringResource(R.string.downloads_savings, savedBytes.toReadableSize()),
                    containerColor = content.copy(alpha = 0.16f),
                    contentColor = content,
                )
            }
        }
    }
}

/** Réglage à changer dans une appli ou dans Android pour moins consommer en mobile. */
private data class SavingTip(val icon: ImageVector, val title: Int, val body: Int)

private val SavingTips = listOf(
    SavingTip(Icons.Rounded.PlayCircle, R.string.tip_autoplay_title, R.string.tip_autoplay_body),
    SavingTip(Icons.Rounded.HighQuality, R.string.tip_video_quality_title, R.string.tip_video_quality_body),
    SavingTip(Icons.Rounded.Photo, R.string.tip_media_download_title, R.string.tip_media_download_body),
    SavingTip(Icons.Rounded.SystemUpdate, R.string.tip_updates_title, R.string.tip_updates_body),
    SavingTip(Icons.Rounded.DataSaverOn, R.string.tip_data_saver_title, R.string.tip_data_saver_body),
)

/** Astuces pour économiser la data mobile, sous la liste des téléchargements. */
@Composable
private fun SavingTipsCard(modifier: Modifier = Modifier) {
    DataKeeperCard(modifier = modifier) {
        Text(stringResource(R.string.tips_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SavingTips.forEach { tip ->
                Row {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            tip.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(tip.title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(tip.body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddLinkSheet(state: DownloadsState, onIntent: (DownloadsIntent) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.downloads_add_link), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.downloads_sheet_explanation),
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
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fileName,
                onValueChange = { onIntent(DownloadsIntent.FileNameChanged(it)) },
                label = { Text(stringResource(R.string.downloads_file_name_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.downloads_schedule_label), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadSchedule.entries.forEach { schedule ->
                    FilterChip(
                        selected = state.schedule == schedule,
                        onClick = { onIntent(DownloadsIntent.ScheduleSelected(schedule)) },
                        label = { Text(stringResource(schedule.labelRes)) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
            Button(
                onClick = { onIntent(DownloadsIntent.Submit) },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.Wifi, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (state.schedule == DownloadSchedule.AS_SOON_AS_WIFI) R.string.downloads_submit_next_wifi
                        else R.string.downloads_submit,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private val DownloadSchedule.labelRes: Int
    get() = when (this) {
        DownloadSchedule.AS_SOON_AS_WIFI -> R.string.schedule_asap
        DownloadSchedule.IN_ONE_HOUR -> R.string.schedule_one_hour
        DownloadSchedule.TONIGHT -> R.string.schedule_tonight
    }

@ScreenPreviews
@Composable
private fun DownloadsContentPreview() {
    PreviewSurface(padded = false) {
        DownloadsContent(
            state = DownloadsState(downloads = PreviewData.downloads, isOnWifi = true),
            onIntent = {},
            showAddSheet = false,
            onShowAddSheet = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun DownloadsContentOfflinePreview() {
    PreviewSurface(padded = false) {
        DownloadsContent(
            state = DownloadsState(downloads = PreviewData.downloads, isOnWifi = false),
            onIntent = {},
            showAddSheet = false,
            onShowAddSheet = {},
        )
    }
}
