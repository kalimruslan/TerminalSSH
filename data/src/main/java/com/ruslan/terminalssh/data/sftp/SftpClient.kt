package com.ruslan.terminalssh.data.sftp

import com.ruslan.terminalssh.core.common.di.IoDispatcher
import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.data.ssh.SshClient
import com.ruslan.terminalssh.domain.model.FileEntry
import com.ruslan.terminalssh.domain.repository.TransferProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.apache.sshd.sftp.client.SftpClient as MinaSftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SftpClient @Inject constructor(
    private val sshClient: SshClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var sftpChannel: MinaSftpClient? = null

    suspend fun openChannel(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val session = sshClient.getSession()
                ?: return@withContext Result.Error(IllegalStateException("SSH not connected"))
            sftpChannel = SftpClientFactory.instance().createSftpClient(session)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun closeChannel() = withContext(ioDispatcher) {
        try {
            sftpChannel?.close()
        } catch (e: Exception) {
            // Ignore
        }
        sftpChannel = null
    }

    suspend fun listDirectory(path: String): Result<List<FileEntry>> = withContext(ioDispatcher) {
        try {
            val client = sftpChannel
                ?: return@withContext Result.Error(IllegalStateException("SFTP not opened"))

            val handle = client.openDir(path)
            val entries = mutableListOf<FileEntry>()

            try {
                var dirEntries = client.readDir(handle)
                while (dirEntries != null) {
                    for (entry in dirEntries) {
                        if (entry.filename != "." && entry.filename != "..") {
                            val attrs = entry.attributes
                            entries.add(
                                FileEntry(
                                    name = entry.filename,
                                    path = "$path/${entry.filename}".replace("//", "/"),
                                    isDirectory = attrs.isDirectory,
                                    size = attrs.size,
                                    permissions = formatPermissions(attrs.permissions),
                                    modifiedTime = attrs.modifyTime?.toMillis() ?: 0L,
                                    owner = attrs.owner ?: ""
                                )
                            )
                        }
                    }
                    dirEntries = client.readDir(handle)
                }
            } finally {
                client.close(handle)
            }

            // Сортировка: папки первые, затем по имени
            Result.Success(entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getCurrentDirectory(): Result<String> = withContext(ioDispatcher) {
        try {
            val client = sftpChannel
                ?: return@withContext Result.Error(IllegalStateException("SFTP not opened"))
            Result.Success(client.canonicalPath("."))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createDirectory(path: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val client = sftpChannel
                ?: return@withContext Result.Error(IllegalStateException("SFTP not opened"))
            client.mkdir(path)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteFile(path: String, isDirectory: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            val client = sftpChannel
                ?: return@withContext Result.Error(IllegalStateException("SFTP not opened"))
            if (isDirectory) {
                client.rmdir(path)
            } else {
                client.remove(path)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun downloadFile(remotePath: String, localPath: String): Flow<TransferProgress> = flow {
        val client = sftpChannel
            ?: throw IllegalStateException("SFTP not opened")

        val attrs = client.stat(remotePath)
        val totalBytes = attrs.size

        emit(TransferProgress(0, totalBytes, false))

        val handle = client.open(remotePath, EnumSet.of(MinaSftpClient.OpenMode.Read))
        try {
            FileOutputStream(localPath).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesTransferred = 0L
                var offset = 0L

                while (true) {
                    val bytesRead = client.read(handle, offset, buffer, 0, buffer.size)
                    if (bytesRead <= 0) break

                    outputStream.write(buffer, 0, bytesRead)
                    bytesTransferred += bytesRead
                    offset += bytesRead

                    emit(TransferProgress(bytesTransferred, totalBytes, false))
                }

                emit(TransferProgress(totalBytes, totalBytes, true))
            }
        } finally {
            client.close(handle)
        }
    }.flowOn(ioDispatcher)

    fun uploadFile(localPath: String, remotePath: String): Flow<TransferProgress> = flow {
        val client = sftpChannel
            ?: throw IllegalStateException("SFTP not opened")

        val localFile = File(localPath)
        val totalBytes = localFile.length()

        emit(TransferProgress(0, totalBytes, false))

        val handle = client.open(
            remotePath,
            EnumSet.of(MinaSftpClient.OpenMode.Write, MinaSftpClient.OpenMode.Create, MinaSftpClient.OpenMode.Truncate)
        )
        try {
            FileInputStream(localFile).use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesTransferred = 0L
                var offset = 0L

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead <= 0) break

                    client.write(handle, offset, buffer, 0, bytesRead)
                    bytesTransferred += bytesRead
                    offset += bytesRead

                    emit(TransferProgress(bytesTransferred, totalBytes, false))
                }

                emit(TransferProgress(totalBytes, totalBytes, true))
            }
        } finally {
            client.close(handle)
        }
    }.flowOn(ioDispatcher)

    private fun formatPermissions(permissions: Int): String {
        val sb = StringBuilder()
        sb.append(if ((permissions and 0x4000) != 0) "d" else "-")
        sb.append(if ((permissions and 0x100) != 0) "r" else "-")
        sb.append(if ((permissions and 0x80) != 0) "w" else "-")
        sb.append(if ((permissions and 0x40) != 0) "x" else "-")
        sb.append(if ((permissions and 0x20) != 0) "r" else "-")
        sb.append(if ((permissions and 0x10) != 0) "w" else "-")
        sb.append(if ((permissions and 0x8) != 0) "x" else "-")
        sb.append(if ((permissions and 0x4) != 0) "r" else "-")
        sb.append(if ((permissions and 0x2) != 0) "w" else "-")
        sb.append(if ((permissions and 0x1) != 0) "x" else "-")
        return sb.toString()
    }

    companion object {
        private const val BUFFER_SIZE = 8192 // 8KB
    }
}
