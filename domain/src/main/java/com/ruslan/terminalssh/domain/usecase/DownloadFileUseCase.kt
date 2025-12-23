package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.SftpRepository
import com.ruslan.terminalssh.domain.repository.TransferProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(
    private val repository: SftpRepository
) {
    operator fun invoke(remotePath: String, localPath: String): Flow<TransferProgress> =
        repository.downloadFile(remotePath, localPath)
}
