package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.DownloadRepository
import javax.inject.Inject

class ClearFinishedDownloadsUseCase @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke() = repository.clearFinished()
}
