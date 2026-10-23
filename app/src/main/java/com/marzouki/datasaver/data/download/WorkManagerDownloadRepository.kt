package com.marzouki.datasaver.data.download

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.marzouki.datasaver.domain.enums.DownloadSchedule
import com.marzouki.datasaver.domain.enums.DownloadState
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Les téléchargements sont des requêtes WorkManager contraintes à [NetworkType.UNMETERED] :
 * ils ne consomment jamais la data mobile. Les métadonnées sont portées par les tags.
 */
class WorkManagerDownloadRepository @Inject constructor(
    private val workManager: WorkManager,
) : DownloadRepository {

    override fun observeDownloads(): Flow<List<ScheduledDownload>> =
        workManager.getWorkInfosByTagFlow(TAG_DOWNLOAD).map { infos -> infos.mapNotNull { it.toDomain() } }

    override fun schedule(url: String, fileName: String, schedule: DownloadSchedule) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMillis(schedule), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .setInputData(workDataOf(DownloadWorker.KEY_URL to url, DownloadWorker.KEY_FILE_NAME to fileName))
            .addTag(TAG_DOWNLOAD)
            .addTag(TAG_URL + url)
            .addTag(TAG_FILE + fileName)
            .addTag(TAG_CREATED + System.currentTimeMillis())
            .build()
        workManager.enqueue(request)
    }

    override fun cancel(id: String) {
        workManager.cancelWorkById(UUID.fromString(id))
    }

    override fun clearFinished() {
        workManager.pruneWork()
    }

    private fun initialDelayMillis(schedule: DownloadSchedule, now: Long = System.currentTimeMillis()): Long =
        when (schedule) {
            DownloadSchedule.AS_SOON_AS_WIFI -> 0L
            DownloadSchedule.IN_ONE_HOUR -> TimeUnit.HOURS.toMillis(1)
            DownloadSchedule.TONIGHT -> {
                val tonight = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, NIGHT_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
                }
                tonight.timeInMillis - now
            }
        }

    private fun WorkInfo.toDomain(): ScheduledDownload? {
        fun tagValue(prefix: String) = tags.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
        val url = tagValue(TAG_URL) ?: return null
        return ScheduledDownload(
            id = id.toString(),
            url = url,
            fileName = tagValue(TAG_FILE).orEmpty(),
            state = when (state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.WAITING_FOR_WIFI
                WorkInfo.State.RUNNING -> DownloadState.RUNNING
                WorkInfo.State.SUCCEEDED -> DownloadState.SUCCEEDED
                WorkInfo.State.FAILED -> DownloadState.FAILED
                WorkInfo.State.CANCELLED -> DownloadState.CANCELLED
            },
            progress = when (state) {
                WorkInfo.State.RUNNING -> progress.getInt(DownloadWorker.KEY_PROGRESS, -1).takeIf { it >= 0 }
                WorkInfo.State.SUCCEEDED -> 100
                else -> null
            },
            earliestStartMillis = nextScheduleTimeMillis
                .takeIf { state == WorkInfo.State.ENQUEUED && it != Long.MAX_VALUE },
            createdAtMillis = tagValue(TAG_CREATED)?.toLongOrNull() ?: 0L,
            totalBytes = when (state) {
                WorkInfo.State.RUNNING -> progress.getLong(DownloadWorker.KEY_TOTAL_BYTES, -1L)
                WorkInfo.State.SUCCEEDED -> outputData.getLong(DownloadWorker.KEY_TOTAL_BYTES, -1L)
                else -> -1L
            }.takeIf { it > 0 },
        )
    }

    private companion object {
        const val TAG_DOWNLOAD = "datakeeper-download"
        const val TAG_URL = "url="
        const val TAG_FILE = "file="
        const val TAG_CREATED = "created="
        const val NIGHT_HOUR = 2
    }
}
