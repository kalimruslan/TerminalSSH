package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.repository.SshRepository
import javax.inject.Inject

class ExecuteCommandUseCase @Inject constructor(
    private val repository: SshRepository
) {
    suspend operator fun invoke(command: String): Result<Unit> {
        return repository.executeCommand(command)
    }
}
