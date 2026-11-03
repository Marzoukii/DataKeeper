package com.marzouki.datasaver.domain.model

import com.marzouki.datasaver.domain.enums.TunisianOperator
import java.text.Normalizer
import kotlin.math.roundToLong

/**
 * Solde data reconnu dans un SMS d'opérateur, par exemple « Il vous reste 19,3 Go valable
 * jusqu'au 01/12 ». [remainingBytes] est le volume restant, [operator] l'opérateur de l'expéditeur
 * quand il est reconnu.
 */
data class OperatorBalanceSms(
    val operator: TunisianOperator?,
    val remainingBytes: Long,
    val rawBody: String,
)

/**
 * Lecture passive du solde : on n'interroge rien, on lit seulement le SMS que l'opérateur a déjà
 * envoyé à l'appareil. Reconnaît Ooredoo, Tunisie Telecom et Orange par l'expéditeur, et extrait le
 * volume restant d'un texte en français ou en arabe. Aucune donnée ne quitte l'appareil.
 */
object OperatorBalanceSmsParser {

    /** Expéditeur → opérateur tunisien, d'après son identifiant SMS (« Ooredoo », « TT », « Orange »). */
    fun operatorOf(sender: String?): TunisianOperator? {
        val normalized = normalize(sender)
        if (normalized.isEmpty()) return null
        val compact = normalized.replace(" ", "")
        return when {
            "ooredoo" in compact || "tunisiana" in compact -> TunisianOperator.OOREDOO
            "orange" in compact -> TunisianOperator.ORANGE
            "tunisietelecom" in compact || "telecom" in compact || normalized == "tt" ||
                normalized.startsWith("tt ") || "ittissalat" in compact -> TunisianOperator.TUNISIE_TELECOM
            else -> null
        }
    }

    /**
     * Solde lu dans [body], ou null si le SMS ne contient pas de solde data crédible. Un SMS d'un
     * expéditeur opérateur reconnu suffit ; sinon le texte doit annoncer explicitement un reste
     * (« il reste », « solde », « تبقى »…) pour éviter de prendre un volume promotionnel.
     */
    fun parse(sender: String?, body: String): OperatorBalanceSms? {
        if (body.isBlank()) return null
        val operator = operatorOf(sender)
        val amounts = DATA_AMOUNT.findAll(body).toList()
        if (amounts.isEmpty()) return null

        val lower = body.lowercase()
        val hasRemainingWord = REMAINING_WORDS.any { it in lower }
        // Un expéditeur opérateur reconnu suffit ; sinon il faut un mot de « reste » explicite.
        if (operator == null && !hasRemainingWord) return null

        val chosen = chooseRemaining(lower, amounts) ?: return null
        val bytes = toBytes(chosen) ?: return null
        return OperatorBalanceSms(operator, bytes, body.trim())
    }

    /**
     * Parmi les volumes du SMS, celui qui suit un mot de « reste » (« il reste 19 Go »), sinon le
     * premier (le terme de gauche d'un « 19 Go sur 25 Go »). [lower] est le corps en minuscules,
     * de même longueur que celui où les volumes ont été repérés.
     */
    private fun chooseRemaining(lower: String, amounts: List<MatchResult>): MatchResult? {
        val afterKeyword = amounts.firstOrNull { match ->
            val before = lower.substring((match.range.first - REMAINING_LOOKBEHIND).coerceAtLeast(0), match.range.first)
            REMAINING_WORDS.any { before.contains(it) }
        }
        return afterKeyword ?: amounts.first()
    }

    private fun toBytes(match: MatchResult): Long? {
        val number = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unit = normalize(match.groupValues[2])
        val factor = when {
            unit.startsWith("g") -> 1_000_000_000.0
            unit.startsWith("m") -> 1_000_000.0
            unit.startsWith("k") -> 1_000.0
            else -> return null
        }
        if (number <= 0.0) return null
        return (number * factor).roundToLong()
    }

    private fun normalize(value: String?): String =
        Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .replace(Regex("[\\u0640]"), "") // tatweel arabe
            .trim()

    /** Nombre (19 ou 19,3) suivi d'une unité de données (Go/Gb/G, Mo/Mb/M, Ko/Kb/K), unités longues d'abord. */
    private val DATA_AMOUNT = Regex(
        "([0-9]+(?:[.,][0-9]+)?)\\s*(Go|GB|G|Mo|MB|M|Ko|KB|K)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** Mots signalant un reste, en français et en arabe (normalisés, sans accents ni diacritiques). */
    private val REMAINING_WORDS = listOf(
        "reste", "restant", "solde", "disponible", "dispo", "il te reste", "il vous reste",
        "تبقى", "متبقي", "المتبقي", "رصيد", "باقي", "يتبقى",
    )

    private const val REMAINING_LOOKBEHIND = 40
}

/**
 * Solde détecté dans une notification (souvent un SMS) de l'opérateur, en attente que l'utilisateur
 * confirme avant de recaler le forfait. [operatorName] est le nom affiché quand il est reconnu.
 */
data class OperatorBalanceReading(
    val remainingBytes: Long,
    val operatorName: String?,
    val atMillis: Long,
)
