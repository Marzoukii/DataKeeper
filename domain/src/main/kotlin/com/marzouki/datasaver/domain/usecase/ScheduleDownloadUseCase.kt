package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.DownloadSchedule
import com.marzouki.datasaver.domain.model.InvalidDownloadUrlException
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.DownloadRepository
import java.net.URI
import javax.inject.Inject

class ScheduleDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository,
    private val clock: Clock,
) {

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
            .ifBlank { "download-${clock.nowMillis()}" }
        repository.schedule(trimmedUrl, name, schedule)
        return Result.success(name)
    }

    private fun sanitizeFileName(raw: String): String =
        raw.trim().replace(Regex("""[\\/:*?"<>|]"""), "_").trimStart('.')
}
