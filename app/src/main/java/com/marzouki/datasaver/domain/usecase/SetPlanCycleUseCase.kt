package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.MonthPeriod
import com.marzouki.datasaver.domain.model.PlanValidity
import com.marzouki.datasaver.domain.repository.QuotaRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Cycle du forfait : pass de [validityDays] jours depuis [purchaseMillis] (30 jours pour un
 * Flexi 25 Go, 4 pour un 220 Mo), ou, avec null, chaque mois le jour de [purchaseMillis].
 */
class SetPlanCycleUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val setCycleStartDay: SetCycleStartDayUseCase,
) {
    suspend operator fun invoke(purchaseMillis: Long, validityDays: Int?) {
        if (validityDays == null) {
            val day = Calendar.getInstance().apply { timeInMillis = purchaseMillis }.get(Calendar.DAY_OF_MONTH)
            repository.setPlanValidity(null)
            setCycleStartDay(day)
        } else {
            repository.setPlanValidity(PlanValidity(purchaseMillis, validityDays.coerceIn(1, MonthPeriod.MAX_VALIDITY_DAYS)))
            // Le cycle courant change : l'alerte doit pouvoir se déclencher sur le nouveau.
            repository.setLastAlertedMonth(null)
        }
    }
}
