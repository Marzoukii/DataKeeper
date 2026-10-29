package com.marzouki.datasaver.di

import com.marzouki.datasaver.data.analytics.FirebaseAnalyticsTracker
import com.marzouki.datasaver.data.analytics.FirebaseCrashReporter
import com.marzouki.datasaver.data.connectivity.ConnectivityRepositoryImpl
import com.marzouki.datasaver.data.download.WorkManagerDownloadRepository
import com.marzouki.datasaver.data.language.LanguageRepositoryImpl
import com.marzouki.datasaver.data.operator.OperatorRepositoryImpl
import com.marzouki.datasaver.data.quota.NotificationQuotaAlertNotifier
import com.marzouki.datasaver.data.quota.QuotaRepositoryImpl
import com.marzouki.datasaver.data.quota.WorkManagerQuotaMonitorScheduler
import com.marzouki.datasaver.data.speedtest.SpeedTestRepositoryImpl
import com.marzouki.datasaver.data.tethering.TetheringRepositoryImpl
import com.marzouki.datasaver.data.tethering.WorkManagerTetheringMonitorScheduler
import com.marzouki.datasaver.data.theme.ThemeRepositoryImpl
import com.marzouki.datasaver.data.usage.UsageRepositoryImpl
import com.marzouki.datasaver.data.wifi.WifiRepositoryImpl
import com.marzouki.datasaver.domain.repository.AnalyticsTracker
import com.marzouki.datasaver.domain.repository.ConnectivityRepository
import com.marzouki.datasaver.domain.repository.CrashReporter
import com.marzouki.datasaver.domain.repository.DownloadRepository
import com.marzouki.datasaver.domain.repository.LanguageRepository
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.SpeedTestRepository
import com.marzouki.datasaver.domain.repository.TetheringMonitorScheduler
import com.marzouki.datasaver.domain.repository.TetheringRepository
import com.marzouki.datasaver.domain.repository.ThemeRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import com.marzouki.datasaver.domain.repository.WifiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Relie chaque interface du domaine à son implémentation de la couche data. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindQuotaRepository(impl: QuotaRepositoryImpl): QuotaRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: WorkManagerDownloadRepository): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityRepository(impl: ConnectivityRepositoryImpl): ConnectivityRepository

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(impl: SpeedTestRepositoryImpl): SpeedTestRepository

    // Singleton : son suivi de la SIM est partagé entre tous les écrans.
    @Binds
    @Singleton
    abstract fun bindOperatorRepository(impl: OperatorRepositoryImpl): OperatorRepository

    @Binds
    abstract fun bindLanguageRepository(impl: LanguageRepositoryImpl): LanguageRepository

    @Binds
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    abstract fun bindWifiRepository(impl: WifiRepositoryImpl): WifiRepository

    // Singleton : son suivi du partage de connexion est partagé entre tous les écrans.
    @Binds
    @Singleton
    abstract fun bindTetheringRepository(impl: TetheringRepositoryImpl): TetheringRepository

    @Binds
    abstract fun bindTetheringMonitorScheduler(impl: WorkManagerTetheringMonitorScheduler): TetheringMonitorScheduler

    @Binds
    abstract fun bindQuotaMonitorScheduler(impl: WorkManagerQuotaMonitorScheduler): QuotaMonitorScheduler

    @Binds
    abstract fun bindQuotaAlertNotifier(impl: NotificationQuotaAlertNotifier): QuotaAlertNotifier

    @Binds
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter

    @Binds
    abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker
}
