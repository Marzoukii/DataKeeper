package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.OperatorChange
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Nouvelle SIM des données en attente de son forfait (opérateur et heure du changement) ; null sinon. */
class ObserveOperatorChangeUseCase @Inject constructor(private val repository: QuotaRepository) {
    operator fun invoke(): Flow<OperatorChange?> = repository.operatorChange.distinctUntilChanged()
}
