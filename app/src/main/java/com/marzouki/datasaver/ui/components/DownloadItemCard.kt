package com.marzouki.datasaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.R
import com.marzouki.datasaver.domain.enums.DownloadState
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.ui.theme.DataColors
import com.marzouki.datasaver.ui.theme.numeric
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val VIDEO = setOf("MP4", "MKV", "WEBM", "MOV", "AVI", "M4V")
private val AUDIO = setOf("MP3", "M4A", "AAC", "OGG", "OPUS", "FLAC", "WAV")

/** Carte d'un téléchargement planifié : badge d'extension, titre, taille, progression et état. */
@Composable
fun DownloadItemCard(
    item: ScheduledDownload,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    val extension = item.fileName.substringAfterLast('.', "").uppercase(Locale.ROOT).take(4)
    val stateColor = item.stateColor()

    DataKeeperCard(modifier, contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExtensionBadge(extension)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    item.totalBytes?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it.toReadableSize(), style = MaterialTheme.typography.labelLarge.numeric())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Progress(item, stateColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        statusText(item),
                        style = MaterialTheme.typography.labelMedium,
                        color = stateColor,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp),
                    )
                    if (onCancel != null && !item.state.isFinished) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionBadge(extension: String) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (extension) {
        in VIDEO -> colors.tertiaryContainer to colors.onTertiaryContainer
        in AUDIO -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    val description = stringResource(R.string.downloads_file_type, extension)
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            extension.ifEmpty { "?" },
            style = MaterialTheme.typography.labelMedium.numeric(),
            color = content,
        )
    }
}

@Composable
private fun Progress(item: ScheduledDownload, color: Color) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(CircleShape)
    val track = MaterialTheme.colorScheme.surfaceVariant
    when (item.state) {
        DownloadState.RUNNING -> {
            val progress = item.progress
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = modifier,
                    color = color,
                    trackColor = track,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            } else {
                LinearProgressIndicator(modifier = modifier, color = color, trackColor = track, strokeCap = StrokeCap.Round, gapSize = 0.dp)
            }
        }
        DownloadState.SUCCEEDED -> ThinBar(1f, color)
        else -> ThinBar(0f, color)
    }
}

@Composable
private fun ScheduledDownload.stateColor(): Color = when (state) {
    DownloadState.RUNNING -> DataColors.wifi
    DownloadState.SUCCEEDED -> DataColors.success
    DownloadState.FAILED -> MaterialTheme.colorScheme.error
    DownloadState.WAITING_FOR_WIFI, DownloadState.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
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

val DownloadState.isFinished: Boolean
    get() = this == DownloadState.SUCCEEDED || this == DownloadState.FAILED || this == DownloadState.CANCELLED

@ThemePreviews
@Composable
private fun DownloadItemCardPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewData.downloads.forEach { DownloadItemCard(it, onCancel = {}) }
        }
    }
}
