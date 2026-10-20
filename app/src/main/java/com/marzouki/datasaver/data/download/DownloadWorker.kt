package com.marzouki.datasaver.data.download

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.marzouki.datasaver.di.IoDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

/**
 * Télécharge un fichier dans le stockage privé de l'application pour un usage hors ligne.
 * N'est exécuté par WorkManager que sur un réseau non facturé (cf. [WorkManagerDownloadRepository]).
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return@withContext Result.failure()

        val directory = downloadDirectory(applicationContext).apply { mkdirs() }
        val target = File(directory, fileName)
        val partial = File(directory, "$fileName.part")

        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    return@withContext if (code >= 500) retryOrFail() else Result.failure()
                }
                val totalBytes = connection.contentLengthLong
                var lastProgress = -1
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var copied = 0L
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (totalBytes > 0) {
                                val progress = (copied * 100 / totalBytes).toInt()
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    setProgress(workDataOf(KEY_PROGRESS to progress))
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) return@withContext Result.failure()
            Result.success(workDataOf(KEY_FILE_PATH to target.absolutePath))
        } catch (e: CancellationException) {
            partial.delete()
            throw e
        } catch (_: IOException) {
            // Perte du Wi-Fi en cours de route, serveur injoignable… : WorkManager
            // retentera quand les contraintes réseau seront de nouveau satisfaites.
            partial.delete()
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()

    companion object {
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_PROGRESS = "progress"

        private const val TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_ATTEMPTS = 5

        fun downloadDirectory(context: Context): File =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads")
    }
}
