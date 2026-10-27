package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.ThemeRepository
import javax.inject.Inject

class ApplySavedThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    suspend operator fun invoke() = repository.applySavedThemeMode()
}
