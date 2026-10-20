package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.DownloadSchedule
import com.marzouki.datasaver.domain.model.InvalidDownloadUrlException
import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URI
import javax.inject.Inject

class ObserveDownloadsUseCase @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke(): Flow<List<ScheduledDownload>> =
        repository.observeDownloads().map { list -> list.sortedByDescending { it.createdAtMillis } }
}

class ScheduleDownloadUseCase @Inject constructor(private val repository: DownloadRepository) {

    /**
     * Planifie le téléchargement ; le nom de fichier est déduit de l'URL s'il est vide.
     * Échoue avec [InvalidDownloadUrlException] si l'URL n'est pas en http(s).
     */
    operator fun invoke(url: String, fileName: String, schedule: DownloadSchedule): Result<String> {
        val trimmedUrl = url.trim()
        val uri = runCatching { URI(trimmedUrl) }.getOrNull()
        if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return Result.failure(InvalidDownloadUrlException())
        }
        val name = sanitizeFileName(fileName.ifBlank { uri.path.orEmpty().substringAfterLast('/') })
            .ifBlank { "download-${System.currentTimeMillis()}" }
        repository.schedule(trimmedUrl, name, schedule)
        return Result.success(name)
    }

    private fun sanitizeFileName(raw: String): String =
        raw.trim().replace(Regex("""[\\/:*?"<>|]"""), "_").trimStart('.')
}

class CancelDownloadUseCase @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke(id: String) = repository.cancel(id)
}

class ClearFinishedDownloadsUseCase @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke() = repository.clearFinished()
}
