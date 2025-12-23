package com.ruslan.terminalssh.data.repository

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.data.sftp.DemoSftpClient
import com.ruslan.terminalssh.data.sftp.SftpClient
import com.ruslan.terminalssh.domain.model.FileEntry
import com.ruslan.terminalssh.domain.repository.SftpRepository
import com.ruslan.terminalssh.domain.repository.TransferProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SftpRepositoryImpl @Inject constructor(
    private val sftpClient: SftpClient,
    private val demoSftpClient: DemoSftpClient
) : SftpRepository {

    private var isDemoMode = false

    fun setDemoMode(isDemo: Boolean) {
        isDemoMode = isDemo
    }

    override suspend fun openSftpChannel(): Result<Unit> {
        return if (isDemoMode) {
            demoSftpClient.openChannel()
        } else {
            sftpClient.openChannel()
        }
    }

    override suspend fun closeSftpChannel() {
        if (isDemoMode) {
            demoSftpClient.closeChannel()
        } else {
            sftpClient.closeChannel()
        }
    }

    override suspend fun listDirectory(path: String): Result<List<FileEntry>> {
        return if (isDemoMode) {
            demoSftpClient.listDirectory(path)
        } else {
            sftpClient.listDirectory(path)
        }
    }

    override suspend fun getCurrentDirectory(): Result<String> {
        return if (isDemoMode) {
            demoSftpClient.getCurrentDirectory()
        } else {
            sftpClient.getCurrentDirectory()
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> {
        return if (isDemoMode) {
            demoSftpClient.createDirectory(path)
        } else {
            sftpClient.createDirectory(path)
        }
    }

    override suspend fun deleteFile(path: String, isDirectory: Boolean): Result<Unit> {
        return if (isDemoMode) {
            demoSftpClient.deleteFile(path, isDirectory)
        } else {
            sftpClient.deleteFile(path, isDirectory)
        }
    }

    override fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<TransferProgress> {
        return if (isDemoMode) {
            demoSftpClient.downloadFile(remotePath, localPath)
        } else {
            sftpClient.downloadFile(remotePath, localPath)
        }
    }

    override fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<TransferProgress> {
        return if (isDemoMode) {
            demoSftpClient.uploadFile(localPath, remotePath)
        } else {
            sftpClient.uploadFile(localPath, remotePath)
        }
    }
}
