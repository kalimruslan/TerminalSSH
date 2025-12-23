package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.SftpRepository
import javax.inject.Inject

class CloseSftpChannelUseCase @Inject constructor(
    private val repository: SftpRepository
) {
    suspend operator fun invoke() = repository.closeSftpChannel()
}
