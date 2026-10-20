package com.marzouki.datasaver.presentation.downloads

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.CancelDownloadUseCase
import com.marzouki.datasaver.domain.usecase.ClearFinishedDownloadsUseCase
import com.marzouki.datasaver.domain.usecase.ObserveDownloadsUseCase
import com.marzouki.datasaver.domain.usecase.ObserveWifiConnectionUseCase
import com.marzouki.datasaver.domain.usecase.ScheduleDownloadUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    observeDownloads: ObserveDownloadsUseCase,
    observeWifiConnection: ObserveWifiConnectionUseCase,
    private val scheduleDownload: ScheduleDownloadUseCase,
    private val cancelDownload: CancelDownloadUseCase,
    private val clearFinishedDownloads: ClearFinishedDownloadsUseCase,
) : MviViewModel<DownloadsState, DownloadsIntent, DownloadsEffect>(DownloadsState()) {

    init {
        observeDownloads()
            .onEach { downloads -> setState { copy(downloads = downloads) } }
            .launchIn(viewModelScope)
        observeWifiConnection()
            .onEach { onWifi -> setState { copy(isOnWifi = onWifi) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.UrlChanged -> setState { copy(url = intent.value, urlError = false) }
            is DownloadsIntent.FileNameChanged -> setState { copy(fileName = intent.value) }
            is DownloadsIntent.ScheduleSelected -> setState { copy(schedule = intent.schedule) }
            DownloadsIntent.Submit -> submit()
            is DownloadsIntent.Cancel -> cancelDownload(intent.id)
            DownloadsIntent.ClearFinished -> clearFinishedDownloads()
        }
    }

    private fun submit() {
        val state = currentState
        scheduleDownload(state.url, state.fileName, state.schedule)
            .onSuccess { fileName ->
                setState { copy(url = "", fileName = "", urlError = false) }
                sendEffect(DownloadsEffect.Scheduled(fileName))
            }
            .onFailure { setState { copy(urlError = true) } }
    }
}
