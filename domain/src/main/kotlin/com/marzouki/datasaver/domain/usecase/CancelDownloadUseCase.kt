package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.DownloadRepository
import javax.inject.Inject

class CancelDownloadUseCase @Inject constructor(private val repository: DownloadRepository) {
    operator fun invoke(id: String) = repository.cancel(id)
}
