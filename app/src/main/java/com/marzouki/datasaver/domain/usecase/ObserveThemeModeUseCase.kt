package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.ThemeMode
import com.marzouki.datasaver.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
