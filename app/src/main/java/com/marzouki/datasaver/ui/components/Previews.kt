package com.marzouki.datasaver.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marzouki.datasaver.domain.model.AppDataUsage
import com.marzouki.datasaver.domain.model.AppKind
import com.marzouki.datasaver.domain.model.CycleOutcome
import com.marzouki.datasaver.domain.model.DataQuota
import com.marzouki.datasaver.domain.model.DataUsage
import com.marzouki.datasaver.domain.model.DayUsage
import com.marzouki.datasaver.domain.model.DownloadState
import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PricePerGb
import com.marzouki.datasaver.domain.model.QuotaStatus
import com.marzouki.datasaver.domain.model.QuotaStreak
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.ui.theme.DataKeeperTheme
import java.util.Calendar

/** Aperçu clair + sombre. */
@Preview(name = "Clair", showBackground = true, backgroundColor = 0xFFF4F6F9)
@Preview(
    name = "Sombre",
    showBackground = true,
    backgroundColor = 0xFF0B0F14,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ThemePreviews

/** Aperçu d'écran complet, clair + sombre. */
@Preview(name = "Clair", showBackground = true, backgroundColor = 0xFFF4F6F9, widthDp = 390, heightDp = 1100)
@Preview(
    name = "Sombre",
    showBackground = true,
    backgroundColor = 0xFF0B0F14,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    widthDp = 390,
    heightDp = 1100,
)
annotation class ScreenPreviews

@Composable
fun PreviewSurface(padded: Boolean = true, content: @Composable () -> Unit) {
    DataKeeperTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(if (padded) Modifier.padding(16.dp) else Modifier) { content() }
        }
    }
}

/** Données factices partagées par les aperçus. */
object PreviewData {
    private const val GB = 1_000_000_000L
    private const val MB = 1_000_000L

    private val now: Long = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 15, 0) }.timeInMillis

    val period: MonthPeriod = MonthPeriod.current(now)

    val usage = DataUsage(wifiBytes = 18_400 * MB, mobileBytes = 3_200 * MB, period = period)

    val quota = DataQuota(limitBytes = 5 * GB)
    val quotaStatus = QuotaStatus(quota, usedBytes = 3_200 * MB)
    val quotaAlertStatus = QuotaStatus(quota, usedBytes = 4_300 * MB)
    val quotaExceededStatus = QuotaStatus(quota, usedBytes = 5_400 * MB)

    val pricePerGb = PricePerGb(millimes = 2_000)
    val streak = QuotaStreak.from(List(3) { CycleOutcome.WITHIN_QUOTA } + CycleOutcome.EXCEEDED)
    /** Data mobile de chaque jour du cycle jusqu'au 20 octobre, irrégulière, 3,2 Go en tout. */
    val cycleDailyMobile: List<Long> = List(period.currentDay) { day -> 110L + (day * 37) % 100 }
        .let { weights -> weights.map { it * 3_200 * MB / weights.sum() } }

    val days: List<DayUsage> = listOf(
        2_100L to 180L, 3_400L to 90L, 1_800L to 420L, 2_900L to 120L,
        3_800L to 60L, 1_200L to 650L, 2_600L to 210L,
    ).mapIndexed { index, (wifi, mobile) ->
        DayUsage(now - (6 - index) * 86_400_000L, wifi * MB, mobile * MB)
    }

    val topApps = listOf(
        AppDataUsage(AppKind.APP, "com.instagram.android", "Instagram", 1_250 * MB),
        AppDataUsage(AppKind.APP, "com.google.android.youtube", "YouTube", 820 * MB),
        AppDataUsage(AppKind.SYSTEM, null, null, 310 * MB),
    )

    val downloads = listOf(
        download("1", "Conférence Compose.mp4", DownloadState.RUNNING, 42, 860 * MB),
        download("2", "Épisode 112 - Le podcast tech.mp3", DownloadState.RUNNING, 78, 95 * MB),
        download("3", "Guide de voyage Lisbonne.pdf", DownloadState.WAITING_FOR_WIFI, null, null),
        download("4", "Série - S01E04.mkv", DownloadState.SUCCEEDED, 100, 1_400 * MB),
        download("5", "Archive photos.zip", DownloadState.FAILED, null, null),
    )

    private fun download(id: String, name: String, state: DownloadState, progress: Int?, size: Long?) =
        ScheduledDownload(
            id = id,
            url = "https://exemple.fr/$id",
            fileName = name,
            state = state,
            progress = progress,
            earliestStartMillis = null,
            createdAtMillis = now - id.toLong() * 60_000,
            totalBytes = size,
        )
}
