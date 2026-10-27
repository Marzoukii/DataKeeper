package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.TetheringAlert
import com.marzouki.datasaver.domain.repository.TetheringRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTetheringAlertUseCase @Inject constructor(private val repository: TetheringRepository) {
    operator fun invoke(): Flow<TetheringAlert> = repository.alert
}
