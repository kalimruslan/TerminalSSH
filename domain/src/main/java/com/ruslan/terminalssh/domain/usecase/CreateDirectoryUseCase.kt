package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.repository.SftpRepository
import javax.inject.Inject

class CreateDirectoryUseCase @Inject constructor(
    private val repository: SftpRepository
) {
    suspend operator fun invoke(path: String): Result<Unit> =
        repository.createDirectory(path)
}
