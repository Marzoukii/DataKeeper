package com.marzouki.datasaver.domain.model

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
)

enum class DownloadState { WAITING_FOR_WIFI, RUNNING, SUCCEEDED, FAILED, CANCELLED }

/** Quand lancer un téléchargement, toujours sous réserve d'être sur un réseau non facturé. */
enum class DownloadSchedule { AS_SOON_AS_WIFI, IN_ONE_HOUR, TONIGHT }

/** L'utilisateur n'a pas accordé l'accès aux données d'utilisation (PACKAGE_USAGE_STATS). */
class UsageAccessDeniedException : Exception("Usage access not granted")

class InvalidDownloadUrlException : IllegalArgumentException("Invalid download URL")
