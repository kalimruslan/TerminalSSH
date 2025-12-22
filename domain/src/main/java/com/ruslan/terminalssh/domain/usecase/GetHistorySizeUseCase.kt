package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.CommandHistoryRepository
import javax.inject.Inject

class GetHistorySizeUseCase @Inject constructor(
    private val repository: CommandHistoryRepository
) {
    suspend operator fun invoke(connectionId: Long): Int {
        return repository.getHistorySize(connectionId)
    }
}
