package com.marzouki.datasaver.domain.model

import java.util.Calendar
import java.util.Locale

/** Mois calendaire en cours, de son premier jour à minuit jusqu'à [endMillis]. */
data class MonthPeriod(
    val startMillis: Long,
    val endMillis: Long,
    /** Identifiant stable du mois, ex. "2026-10". */
    val key: String,
) {
    companion object {
        fun current(nowMillis: Long = System.currentTimeMillis()): MonthPeriod {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val key = String.format(
                Locale.ROOT, "%04d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
            )
            return MonthPeriod(startMillis = calendar.timeInMillis, endMillis = nowMillis, key = key)
        }
    }
}
