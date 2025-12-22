package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.CommandHistoryRepository
import javax.inject.Inject

class AddToHistoryUseCase @Inject constructor(
    private val repository: CommandHistoryRepository
) {
    suspend operator fun invoke(connectionId: Long, command: String) {
        if (command.isNotBlank()) {
            repository.addCommand(connectionId, command)
        }
    }
}
