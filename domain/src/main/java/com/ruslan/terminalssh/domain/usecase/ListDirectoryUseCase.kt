package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.FileEntry
import com.ruslan.terminalssh.domain.repository.SftpRepository
import javax.inject.Inject

class ListDirectoryUseCase @Inject constructor(
    private val repository: SftpRepository
) {
    suspend operator fun invoke(path: String): Result<List<FileEntry>> =
        repository.listDirectory(path)
}
