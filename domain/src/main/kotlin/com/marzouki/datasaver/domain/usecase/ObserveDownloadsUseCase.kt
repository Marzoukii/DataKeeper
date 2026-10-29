package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.ScheduledDownload
import com.marzouki.datasaver.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveDownloadsUseCase @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke(): Flow<List<ScheduledDownload>> =
        repository.observeDownloads().map { list -> list.sortedByDescending { it.createdAtMillis } }
}
