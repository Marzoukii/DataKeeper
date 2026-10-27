package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.SavedPlan
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Forfaits déjà saisis pour l'opérateur de la SIM des données, suivi en direct ; vide sans SIM. */
class ObserveSavedPlansUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
) {
    operator fun invoke(): Flow<List<SavedPlan>> =
        combine(repository.savedPlans, operatorRepository.observeDataOperator()) { plans, operator ->
            if (operator == null) emptyList() else plans.filter { it.operatorKey == operator.key }
        }.distinctUntilChanged()
}
