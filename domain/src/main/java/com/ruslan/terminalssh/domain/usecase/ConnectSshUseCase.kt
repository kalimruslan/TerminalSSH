package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.repository.SshRepository
import javax.inject.Inject

class ConnectSshUseCase @Inject constructor(
    private val repository: SshRepository
) {
    suspend operator fun invoke(config: ConnectionConfig): Result<Unit> {
        return repository.connect(config)
    }
}
