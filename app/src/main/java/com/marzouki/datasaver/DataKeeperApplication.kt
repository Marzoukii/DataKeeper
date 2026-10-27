package com.marzouki.datasaver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.marzouki.datasaver.domain.usecase.ApplySavedThemeModeUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class DataKeeperApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var applySavedThemeMode: ApplySavedThemeModeUseCase

    override fun onCreate() {
        super.onCreate()
        // Avant la première activité, sinon elle s'afficherait un instant dans le mauvais thème.
        // Une seule préférence locale à lire : l'attente est imperceptible.
        runBlocking { applySavedThemeMode() }
    }

    // WorkManager est initialisé à la demande (initializer par défaut retiré du manifeste)
    // afin que les workers reçoivent leurs dépendances Hilt.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
