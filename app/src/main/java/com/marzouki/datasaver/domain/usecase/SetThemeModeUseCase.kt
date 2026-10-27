package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.ThemeMode
import com.marzouki.datasaver.domain.repository.ThemeRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
