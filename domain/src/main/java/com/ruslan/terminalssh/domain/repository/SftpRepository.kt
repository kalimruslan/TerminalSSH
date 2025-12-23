package com.ruslan.terminalssh.domain.repository

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.FileEntry
import kotlinx.coroutines.flow.Flow

interface SftpRepository {
    suspend fun openSftpChannel(): Result<Unit>
    suspend fun closeSftpChannel()
    suspend fun listDirectory(path: String): Result<List<FileEntry>>
    suspend fun getCurrentDirectory(): Result<String>

    // CRUD операции
    suspend fun createDirectory(path: String): Result<Unit>
    suspend fun deleteFile(path: String, isDirectory: Boolean): Result<Unit>
    fun downloadFile(remotePath: String, localPath: String): Flow<TransferProgress>
    fun uploadFile(localPath: String, remotePath: String): Flow<TransferProgress>
}

data class TransferProgress(
    val bytesTransferred: Long,
    val totalBytes: Long,
    val isComplete: Boolean
)
