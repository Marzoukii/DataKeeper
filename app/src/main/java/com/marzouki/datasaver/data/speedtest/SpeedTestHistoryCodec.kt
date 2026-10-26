package com.marzouki.datasaver.data.speedtest

import com.marzouki.datasaver.domain.enums.NetworkGeneration
import com.marzouki.datasaver.domain.model.SpeedTestResult
import com.marzouki.datasaver.domain.model.SpeedTestServer

/**
 * Historique des tests en une seule préférence texte : un test par ligne, champs séparés par « | ».
 * Une ligne illisible (format d'une version future, préférence abîmée) est ignorée.
 */
internal object SpeedTestHistoryCodec {
    private const val SEPARATOR = '|'

    fun encode(results: List<SpeedTestResult>): String = results.joinToString("\n") { result ->
        listOf(
            result.atMillis.toString(),
            result.downloadMbps.toString(),
            result.uploadMbps.toString(),
            result.pingMs.toString(),
            if (result.onWifi) "1" else "0",
            result.generation?.name.orEmpty(),
            result.operatorName.clean(),
            result.server?.sponsor.clean(),
            result.server?.name.clean(),
        ).joinToString(SEPARATOR.toString())
    }

    fun decode(encoded: String?): List<SpeedTestResult> =
        encoded.orEmpty().lineSequence().mapNotNull(::decodeLine).toList()

    private fun decodeLine(line: String): SpeedTestResult? {
        val fields = line.split(SEPARATOR)
        if (fields.size < 9) return null
        return SpeedTestResult(
            atMillis = fields[0].toLongOrNull() ?: return null,
            downloadMbps = fields[1].toDoubleOrNull() ?: return null,
            uploadMbps = fields[2].toDoubleOrNull() ?: return null,
            pingMs = fields[3].toIntOrNull() ?: return null,
            onWifi = fields[4] == "1",
            generation = NetworkGeneration.entries.firstOrNull { it.name == fields[5] },
            operatorName = fields[6].ifEmpty { null },
            server = if (fields[7].isEmpty() && fields[8].isEmpty()) null else SpeedTestServer(fields[7], fields[8]),
        )
    }

    /** Les séparateurs n'ont pas leur place dans un nom d’opérateur ou de serveur. */
    private fun String?.clean(): String = orEmpty().replace(SEPARATOR, ' ').replace('\n', ' ').trim()
}
