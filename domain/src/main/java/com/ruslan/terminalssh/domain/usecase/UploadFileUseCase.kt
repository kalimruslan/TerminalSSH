package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.SftpRepository
import com.ruslan.terminalssh.domain.repository.TransferProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(
    private val repository: SftpRepository
) {
    operator fun invoke(localPath: String, remotePath: String): Flow<TransferProgress> =
        repository.uploadFile(localPath, remotePath)
}
