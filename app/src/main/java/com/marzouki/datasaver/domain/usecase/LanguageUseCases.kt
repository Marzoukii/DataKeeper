package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.AppLanguageChoice
import com.marzouki.datasaver.domain.repository.LanguageRepository
import javax.inject.Inject

class GetAppLanguageUseCase @Inject constructor(private val repository: LanguageRepository) {
    operator fun invoke(): AppLanguageChoice = repository.current()
}

class SetAppLanguageUseCase @Inject constructor(private val repository: LanguageRepository) {
    operator fun invoke(choice: AppLanguageChoice) {
        if (choice != repository.current()) repository.set(choice)
    }
}
