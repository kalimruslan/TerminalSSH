package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.SshRepository
import javax.inject.Inject

class DisconnectSshUseCase @Inject constructor(
    private val repository: SshRepository
) {
    suspend operator fun invoke() {
        repository.disconnect()
    }
}
