package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.MobileOperator
import com.marzouki.datasaver.domain.repository.OperatorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Opérateur de la SIM des données, mis à jour dès qu'on change de SIM ou qu'on bascule la data. */
class ObserveDataOperatorUseCase @Inject constructor(private val repository: OperatorRepository) {
    operator fun invoke(): Flow<MobileOperator?> = repository.observeDataOperator()
}
