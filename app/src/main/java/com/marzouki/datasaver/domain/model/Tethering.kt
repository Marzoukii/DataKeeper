package com.marzouki.datasaver.domain.model

/**
 * Partage de connexion en cours depuis [startMillis]. [startKnown] est faux quand il était déjà
 * allumé la première fois que l'appli l'a vu : il a commencé plus tôt. [baselineBytes] est le
 * compteur de data mobile du téléphone à ce moment-là.
 */
data class TetheringSession(val startMillis: Long, val startKnown: Boolean, val baselineBytes: Long)

/** Alerte quand la data mobile d'une même session de partage atteint [thresholdBytes]. */
data class TetheringAlert(val enabled: Boolean, val thresholdBytes: Long) {
    companion object {
        const val GB = 1_000_000_000L
        val DEFAULT = TetheringAlert(enabled = false, thresholdBytes = GB)

        /** Seuils proposés : 500 Mo, 1, 2 et 5 Go. */
        val THRESHOLDS = listOf(GB / 2, GB, 2 * GB, 5 * GB)
    }
}

/** Data mobile consommée depuis le début de [session], mesurée à [nowMillis]. */
data class TetheringUsage(val session: TetheringSession, val usedBytes: Long, val nowMillis: Long) {

    val elapsedMillis: Long get() = (nowMillis - session.startMillis).coerceAtLeast(0)

    /** Rythme moyen depuis le début ; null les deux premières minutes, trop courtes pour une moyenne. */
    val averageBytesPerHour: Long?
        get() {
            if (elapsedMillis < MIN_AVERAGE_MILLIS || usedBytes <= 0) return null
            return usedBytes * HOUR_MILLIS / elapsedMillis
        }

    /** Prochain palier à surveiller : le seuil d'alerte s'il est encore devant, sinon le Go entier suivant. */
    fun nextTargetBytes(alert: TetheringAlert): Long =
        if (alert.enabled && usedBytes < alert.thresholdBytes) {
            alert.thresholdBytes
        } else {
            (usedBytes / TetheringAlert.GB + 1) * TetheringAlert.GB
        }

    /** Temps restant avant [targetBytes] au rythme [bytesPerHour] ; null sans rythme. */
    fun millisToReach(targetBytes: Long, bytesPerHour: Long?): Long? {
        if (bytesPerHour == null || bytesPerHour <= 0) return null
        return (targetBytes - usedBytes).coerceAtLeast(0) * HOUR_MILLIS / bytesPerHour
    }

    private companion object {
        const val MIN_AVERAGE_MILLIS = 2 * 60 * 1000L
    }
}

/** Relevé du compteur de la session à un instant, pour mesurer le débit. */
data class TetheringSample(val atMillis: Long, val usedBytes: Long)

/** Débit moyen entre le premier et le dernier relevé, en octets par seconde ; null sur moins d'une seconde. */
fun List<TetheringSample>.bytesPerSecond(): Long? {
    val first = firstOrNull() ?: return null
    val last = last()
    val millis = last.atMillis - first.atMillis
    if (millis < 1_000) return null
    return (last.usedBytes - first.usedBytes).coerceAtLeast(0) * 1_000 / millis
}

/** Relevés des [windowMillis] dernières millisecondes avant [nowMillis]. */
fun List<TetheringSample>.since(nowMillis: Long, windowMillis: Long): List<TetheringSample> =
    filter { it.atMillis >= nowMillis - windowMillis }

private const val HOUR_MILLIS = 60 * 60 * 1000L
