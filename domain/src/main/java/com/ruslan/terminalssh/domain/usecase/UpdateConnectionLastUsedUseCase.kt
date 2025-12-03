package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import javax.inject.Inject

class UpdateConnectionLastUsedUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.updateLastUsed(id)
    }
}
