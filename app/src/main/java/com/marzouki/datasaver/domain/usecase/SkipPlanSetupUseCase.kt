package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import javax.inject.Inject

/**
 * « Je n'ai pas de forfait mobile » : le tableau de bord s'affiche sans quota. Après un changement
 * de SIM, le forfait de l'ancienne est retiré.
 */
class SkipPlanSetupUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
    private val setQuota: SetQuotaUseCase,
) {
    suspend operator fun invoke() {
        setQuota(null)
        repository.setPlanOperatorKey(operatorRepository.dataOperator()?.key)
        repository.setPlanSetupDone(true)
    }
}
