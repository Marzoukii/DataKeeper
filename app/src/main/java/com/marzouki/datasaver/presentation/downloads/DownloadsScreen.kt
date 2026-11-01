package com.marzouki.datasaver.presentation.downloads

import com.marzouki.datasaver.ui.components.SectionTitle
import androidx.compose.material.icons.rounded.Lightbulb
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
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.components.formatDinarsRounded
import com.marzouki.datasaver.ui.components.ThinBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
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

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screenHorizontal, end = Spacing.screenHorizontal, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.betweenCards),
        ) {
            item {
                ScreenHeader(
                    overline = stringResource(R.string.downloads_overline),
                    title = stringResource(R.string.downloads_title),
                ) {
                    HeaderIconButton(
                        Icons.Rounded.Add,
                        stringResource(R.string.downloads_add_link),
                        onClick = { onShowAddSheet(true) },
                        primary = true,
                    )
                }
            }
            item { WifiStatusBanner(state.isOnWifi, pending, Modifier.padding(top = 4.dp)) }
            item { StatsRow(state, available) }
            item { SegmentedTabs(selectedTab, pending.size, available.size, onSelect = { selectedTab = it }) }
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
            if (shown.isNotEmpty()) {
                item {
                    DataKeeperCard(contentPadding = 0.dp) {
                        shown.forEachIndexed { index, download ->
                            if (index > 0) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            DownloadRow(download, onCancel = { onIntent(DownloadsIntent.Cancel(download.id)) })
                        }
                    }
                }
            }
            item { SavingTipsCard(Modifier.padding(top = 4.dp)) }
            if (state.downloads.any { it.state.isFinished }) {
                item {
                    TextButton(
                        onClick = { onIntent(DownloadsIntent.ClearFinished) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.downloads_clear_finished), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

    }

    if (showAddSheet) {
        AddLinkSheet(state, onIntent, onDismiss = { onShowAddSheet(false) })
    }
}

/** Bandeau d'état : sarcelle pâle en Wi-Fi (« Actif »), gris en attente du Wi-Fi ; résumé de la file. */
@Composable
private fun WifiStatusBanner(isOnWifi: Boolean, pending: List<ScheduledDownload>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container = if (isOnWifi) colors.secondaryContainer else colors.surfaceVariant
    val content = if (isOnWifi) colors.onSecondaryContainer else colors.onSurface
    val running = pending.count { it.state == DownloadState.RUNNING }
    val waiting = pending.count { it.state == DownloadState.WAITING_FOR_WIFI }
    val queuedBytes = pending.sumOf { it.totalBytes ?: 0L }
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isOnWifi) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = if (isOnWifi) DataColors.wifi else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (isOnWifi) R.string.downloads_wifi_connected else R.string.downloads_wifi_waiting),
                style = MaterialTheme.typography.titleSmall,
                color = content,
            )
            Text(
                if (running + waiting == 0) {
                    stringResource(R.string.downloads_queue_empty)
                } else {
                    stringResource(R.string.downloads_queue_summary, running, waiting, queuedBytes.toReadableSize())
                },
                style = MaterialTheme.typography.bodySmall.numeric(),
                color = content,
            )
        }
        if (isOnWifi) {
            Text(
                stringResource(R.string.downloads_active),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DataColors.wifi)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Deux cartes : la data mobile utilisée par les téléchargements (0 Mo) et ce qu'ils ont épargné. */
@Composable
private fun StatsRow(state: DownloadsState, available: List<ScheduledDownload>) {
    val savedBytes = available.sumOf { it.totalBytes ?: 0L }
    val price = state.pricePerGb
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            icon = Icons.Rounded.Download,
            iconContainer = MaterialTheme.colorScheme.surfaceVariant,
            iconContent = MaterialTheme.colorScheme.onSurface,
            value = 0L.toReadableSize(),
            valueColor = MaterialTheme.colorScheme.onSurface,
            label = stringResource(R.string.downloads_stat_mobile),
        )
        StatCard(
            icon = Icons.Rounded.Savings,
            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
            iconContent = DataColors.wifi,
            value = if (price != null) formatDinarsRounded(price.costMillimes(savedBytes)) else savedBytes.toReadableSize(),
            valueColor = DataColors.wifi,
            label = stringResource(if (price != null) R.string.downloads_stat_saved_money else R.string.downloads_stat_saved_data),
        )
    }
}

@Composable
private fun RowScope.StatCard(
    icon: ImageVector,
    iconContainer: Color,
    iconContent: Color,
    value: String,
    valueColor: Color,
    label: String,
) {
    DataKeeperCard(modifier = Modifier.weight(1f), contentPadding = 16.dp) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconContent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall.numeric(), color = valueColor, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Onglets « À télécharger » / « Disponibles » en pilule grise, l'onglet actif en carte blanche. */
@Composable
private fun SegmentedTabs(selected: Int, pendingCount: Int, availableCount: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            TAB_PENDING to stringResource(R.string.downloads_tab_pending, pendingCount),
            TAB_AVAILABLE to stringResource(R.string.downloads_tab_available, availableCount),
        ).forEach { (tab, label) ->
            val active = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(
                        if (active) {
                            Modifier.shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.08f))
                        } else {
                            Modifier
                        },
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Ligne de la file : pastille fichier, nom, état et taille, barre de progression, bouton annuler. */
@Composable
private fun DownloadRow(download: ScheduledDownload, onCancel: () -> Unit) {
    val running = download.state == DownloadState.RUNNING
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (running) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(download.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(downloadStatusText(download), download.totalBytes?.toReadableSize()).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val progress = download.progress
            if (running && progress != null) {
                ThinBar(progress / 100f, MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer, height = 6.dp)
            }
        }
        if (!download.state.isFinished) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_cancel), onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (running) Icons.Rounded.Close else Icons.Rounded.Schedule,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
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

/** Astuces numérotées pour économiser la data mobile, sous la file des téléchargements. */
@Composable
private fun SavingTipsCard(modifier: Modifier = Modifier) {
    DataKeeperCard(modifier = modifier, contentPadding = 18.dp) {
        SectionTitle(
            stringResource(R.string.tips_title),
            Icons.Rounded.Lightbulb,
        )
        Spacer(Modifier.height(6.dp))
        SavingTips.forEachIndexed { index, tip ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(10.dp))
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
