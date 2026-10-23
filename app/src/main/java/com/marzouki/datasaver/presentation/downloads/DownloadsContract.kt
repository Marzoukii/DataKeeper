package com.marzouki.datasaver.presentation.downloads

import com.marzouki.datasaver.domain.enums.DownloadSchedule
import com.marzouki.datasaver.domain.model.ScheduledDownload

data class DownloadsState(
    val url: String = "",
    val fileName: String = "",
    val schedule: DownloadSchedule = DownloadSchedule.AS_SOON_AS_WIFI,
    val urlError: Boolean = false,
    val downloads: List<ScheduledDownload> = emptyList(),
    /** Réseau non facturé disponible : les téléchargements peuvent tourner. */
    val isOnWifi: Boolean = false,
)

sealed interface DownloadsIntent {
    data class UrlChanged(val value: String) : DownloadsIntent
    data class FileNameChanged(val value: String) : DownloadsIntent
    data class ScheduleSelected(val schedule: DownloadSchedule) : DownloadsIntent
    data object Submit : DownloadsIntent
    data class Cancel(val id: String) : DownloadsIntent
    data object ClearFinished : DownloadsIntent
}

sealed interface DownloadsEffect {
    data class Scheduled(val fileName: String) : DownloadsEffect
}
