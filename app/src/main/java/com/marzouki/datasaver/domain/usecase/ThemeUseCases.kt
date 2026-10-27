package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.ThemeMode
import com.marzouki.datasaver.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class ApplySavedThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    suspend operator fun invoke() = repository.applySavedThemeMode()
}
