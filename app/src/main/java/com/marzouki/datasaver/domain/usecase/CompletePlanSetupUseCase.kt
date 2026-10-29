package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import javax.inject.Inject

/**
 * Enregistre en une fois ce que demande l'écran de configuration : forfait, jour de
 * renouvellement, solde restant chez l'opérateur et prix du forfait (ces deux derniers facultatifs).
 */
class CompletePlanSetupUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
    private val setQuota: SetQuotaUseCase,
    private val setPlanCycle: SetPlanCycleUseCase,
    private val setOperatorBalance: SetOperatorBalanceUseCase,
    private val setPlanPrice: SetPlanPriceUseCase,
    private val rememberPlan: RememberPlanUseCase,
    private val clock: Clock,
) {
    /**
     * @param purchaseMillis jour d'achat (ou de renouvellement) du forfait.
     * @param validityDays durée du pass en jours ; null s'il se renouvelle chaque mois à cette date.
     */
    suspend operator fun invoke(
        limitBytes: Long,
        purchaseMillis: Long,
        validityDays: Int?,
        remainingBytes: Long?,
        planPriceMillimes: Long?,
        nowMillis: Long = clock.nowMillis(),
    ) {
        setPlanCycle(purchaseMillis, validityDays)
        setQuota(limitBytes)
        // Sans solde saisi, le décompte part du début du cycle.
        setOperatorBalance(remainingBytes, nowMillis)
        // Prix laissé vide : celui d'un forfait précédent (autre SIM) ne vaut pas pour celui-ci.
        setPlanPrice(planPriceMillimes)
        val operatorKey = operatorRepository.dataOperator()?.key
        if (operatorKey != null) rememberPlan(SavedPlan(operatorKey, limitBytes, planPriceMillimes, validityDays))
        repository.setPlanOperatorKey(operatorKey)
        repository.setPlanSetupDone(true)
    }
}
