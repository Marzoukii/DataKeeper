package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.repository.OperatorRepository
import javax.inject.Inject

class HasPhoneStatePermissionUseCase @Inject constructor(private val repository: OperatorRepository) {
    operator fun invoke(): Boolean = repository.hasPhoneStatePermission()
}
