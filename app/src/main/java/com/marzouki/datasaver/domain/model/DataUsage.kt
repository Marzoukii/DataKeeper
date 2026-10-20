package com.marzouki.datasaver.domain.model

/** Trafic de l'appareil (réception + émission) sur une période, réparti par type de réseau. */
data class DataUsage(
    val wifiBytes: Long,
    val mobileBytes: Long,
    val period: MonthPeriod,
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes

    /** Part du trafic passée par le Wi-Fi, entre 0 et 1. */
    val wifiShare: Float get() = if (totalBytes == 0L) 0f else wifiBytes.toFloat() / totalBytes
}

/** Trafic d'une journée calendaire locale commençant à [dayStartMillis]. */
data class DayUsage(
    val dayStartMillis: Long,
    val wifiBytes: Long,
    val mobileBytes: Long,
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

/** Data mobile consommée par une application (ou une catégorie système) sur le cycle. */
data class AppDataUsage(
    val kind: AppKind,
    /** Paquet de l'application, null pour les catégories système. */
    val packageName: String?,
    /** Nom affiché de l'application, s'il a pu être résolu. */
    val label: String?,
    val mobileBytes: Long,
)

enum class AppKind { APP, SYSTEM, TETHERING, REMOVED }
