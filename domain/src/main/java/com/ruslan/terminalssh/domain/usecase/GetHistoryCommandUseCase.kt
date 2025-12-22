package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.model.CommandHistory
import com.ruslan.terminalssh.domain.repository.CommandHistoryRepository
import javax.inject.Inject

class GetHistoryCommandUseCase @Inject constructor(
    private val repository: CommandHistoryRepository
) {
    suspend operator fun invoke(connectionId: Long, index: Int): CommandHistory? {
        return repository.getCommandAt(connectionId, index)
    }
}
