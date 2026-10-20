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
