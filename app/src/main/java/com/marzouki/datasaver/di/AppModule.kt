package com.marzouki.datasaver.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.marzouki.datasaver.data.connectivity.ConnectivityRepositoryImpl
import com.marzouki.datasaver.data.download.WorkManagerDownloadRepository
import com.marzouki.datasaver.data.operator.OperatorRepositoryImpl
import com.marzouki.datasaver.data.quota.NotificationQuotaAlertNotifier
import com.marzouki.datasaver.data.quota.QuotaRepositoryImpl
import com.marzouki.datasaver.data.quota.WorkManagerQuotaMonitorScheduler
import com.marzouki.datasaver.data.usage.UsageRepositoryImpl
import com.marzouki.datasaver.domain.repository.ConnectivityRepository
import com.marzouki.datasaver.domain.repository.DownloadRepository
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaAlertNotifier
import com.marzouki.datasaver.domain.repository.QuotaMonitorScheduler
import com.marzouki.datasaver.domain.repository.QuotaRepository
import com.marzouki.datasaver.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "datakeeper_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

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
    abstract fun bindOperatorRepository(impl: OperatorRepositoryImpl): OperatorRepository

    @Binds
    abstract fun bindQuotaMonitorScheduler(impl: WorkManagerQuotaMonitorScheduler): QuotaMonitorScheduler

    @Binds
    abstract fun bindQuotaAlertNotifier(impl: NotificationQuotaAlertNotifier): QuotaAlertNotifier
}
