package com.marzouki.datasaver.domain.model

import com.marzouki.datasaver.domain.enums.DownloadState

data class ScheduledDownload(
    val id: String,
    val url: String,
    val fileName: String,
    val state: DownloadState,
    /** Progression 0..100, ou null si inconnue. */
    val progress: Int?,
    /** Heure au plus tôt à laquelle le téléchargement peut démarrer, si elle est connue. */
    val earliestStartMillis: Long?,
    val createdAtMillis: Long,
    /** Taille du fichier, connue une fois le téléchargement démarré. */
    val totalBytes: Long? = null,
)

/** L'utilisateur n'a pas accordé l'accès aux données d'utilisation (PACKAGE_USAGE_STATS). */
class UsageAccessDeniedException : Exception("Usage access not granted")

class InvalidDownloadUrlException : IllegalArgumentException("Invalid download URL")
