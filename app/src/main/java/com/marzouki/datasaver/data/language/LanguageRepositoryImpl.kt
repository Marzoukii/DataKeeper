package com.marzouki.datasaver.data.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.marzouki.datasaver.domain.enums.AppLanguageChoice
import com.marzouki.datasaver.domain.repository.LanguageRepository
import javax.inject.Inject

/**
 * Langue propre à l'appli : Android 13+ la gère lui-même (Paramètres › Applis › Langue) ; avant,
 * AppCompat la mémorise et l'applique à l'activité. Changer de langue recrée l'activité.
 */
class LanguageRepositoryImpl @Inject constructor() : LanguageRepository {

    override fun current(): AppLanguageChoice =
        AppLanguageChoice.fromTag(AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag())

    override fun set(choice: AppLanguageChoice) {
        val locales = choice.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
