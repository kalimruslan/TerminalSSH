package com.ruslan.terminalssh.data.sftp

import com.ruslan.terminalssh.core.common.di.IoDispatcher
import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.FileEntry
import com.ruslan.terminalssh.domain.repository.TransferProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoSftpClient @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var isOpened = false
    private var currentUser = "demo"

    // Виртуальная файловая система (изменяемая для поддержки CRUD)
    private val virtualFileSystem: MutableMap<String, MutableList<FileEntry>> by lazy {
        val now = System.currentTimeMillis()
        mutableMapOf(
            "/" to mutableListOf(
                FileEntry("bin", "/bin", true, 4096, "drwxr-xr-x", now),
                FileEntry("etc", "/etc", true, 4096, "drwxr-xr-x", now),
                FileEntry("home", "/home", true, 4096, "drwxr-xr-x", now),
                FileEntry("usr", "/usr", true, 4096, "drwxr-xr-x", now),
                FileEntry("var", "/var", true, 4096, "drwxr-xr-x", now),
                FileEntry("tmp", "/tmp", true, 4096, "drwxrwxrwt", now)
            ),
            "/home" to mutableListOf(
                FileEntry("demo", "/home/demo", true, 4096, "drwxr-xr-x", now),
                FileEntry("guest", "/home/guest", true, 4096, "drwxr-xr-x", now)
            ),
            "/home/demo" to mutableListOf(
                FileEntry("documents", "/home/demo/documents", true, 4096, "drwxr-xr-x", now),
                FileEntry("downloads", "/home/demo/downloads", true, 4096, "drwxr-xr-x", now),
                FileEntry("projects", "/home/demo/projects", true, 4096, "drwxr-xr-x", now),
                FileEntry(".bashrc", "/home/demo/.bashrc", false, 256, "-rw-r--r--", now),
                FileEntry(".profile", "/home/demo/.profile", false, 128, "-rw-r--r--", now)
            ),
            "/home/demo/documents" to mutableListOf(
                FileEntry("readme.txt", "/home/demo/documents/readme.txt", false, 1024, "-rw-r--r--", now),
                FileEntry("notes.md", "/home/demo/documents/notes.md", false, 2048, "-rw-r--r--", now),
                FileEntry("report.pdf", "/home/demo/documents/report.pdf", false, 102400, "-rw-r--r--", now)
            ),
            "/home/demo/downloads" to mutableListOf(
                FileEntry("file1.zip", "/home/demo/downloads/file1.zip", false, 51200, "-rw-r--r--", now),
                FileEntry("image.png", "/home/demo/downloads/image.png", false, 25600, "-rw-r--r--", now)
            ),
            "/home/demo/projects" to mutableListOf(
                FileEntry("app", "/home/demo/projects/app", true, 4096, "drwxr-xr-x", now),
                FileEntry("website", "/home/demo/projects/website", true, 4096, "drwxr-xr-x", now),
                FileEntry("scripts", "/home/demo/projects/scripts", true, 4096, "drwxr-xr-x", now)
            ),
            "/home/demo/projects/app" to mutableListOf(
                FileEntry("src", "/home/demo/projects/app/src", true, 4096, "drwxr-xr-x", now),
                FileEntry("build.gradle", "/home/demo/projects/app/build.gradle", false, 2048, "-rw-r--r--", now),
                FileEntry("README.md", "/home/demo/projects/app/README.md", false, 512, "-rw-r--r--", now)
            ),
            "/home/guest" to mutableListOf(
                FileEntry("welcome.txt", "/home/guest/welcome.txt", false, 64, "-rw-r--r--", now)
            ),
            "/etc" to mutableListOf(
                FileEntry("passwd", "/etc/passwd", false, 1024, "-rw-r--r--", now),
                FileEntry("hosts", "/etc/hosts", false, 256, "-rw-r--r--", now),
                FileEntry("ssh", "/etc/ssh", true, 4096, "drwxr-xr-x", now)
            ),
            "/tmp" to mutableListOf(
                FileEntry("cache", "/tmp/cache", true, 4096, "drwxrwxrwt", now),
                FileEntry("session.tmp", "/tmp/session.tmp", false, 128, "-rw-rw-rw-", now)
            )
        )
    }

    // Виртуальные размеры файлов для эмуляции скачивания
    private val virtualFileSizes = mutableMapOf<String, Long>()

    suspend fun openChannel(): Result<Unit> = withContext(ioDispatcher) {
        delay(300) // Эмуляция задержки
        isOpened = true
        Result.Success(Unit)
    }

    suspend fun closeChannel() = withContext(ioDispatcher) {
        isOpened = false
    }

    suspend fun listDirectory(path: String): Result<List<FileEntry>> = withContext(ioDispatcher) {
        if (!isOpened) {
            return@withContext Result.Error(IllegalStateException("SFTP not opened"))
        }

        delay(200 + (0..300).random().toLong()) // Эмуляция сетевой задержки

        val normalizedPath = path.trimEnd('/')
        val entries = virtualFileSystem[normalizedPath]

        if (entries != null) {
            Result.Success(entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })))
        } else {
            // Возвращаем пустой список для несуществующих, но валидных путей
            Result.Success(emptyList())
        }
    }

    suspend fun getCurrentDirectory(): Result<String> = withContext(ioDispatcher) {
        if (!isOpened) {
            return@withContext Result.Error(IllegalStateException("SFTP not opened"))
        }
        Result.Success("/home/$currentUser")
    }

    suspend fun createDirectory(path: String): Result<Unit> = withContext(ioDispatcher) {
        if (!isOpened) {
            return@withContext Result.Error(IllegalStateException("SFTP not opened"))
        }

        delay(200) // Эмуляция задержки

        val parentPath = path.substringBeforeLast("/").ifEmpty { "/" }
        val dirName = path.substringAfterLast("/")

        val parentEntries = virtualFileSystem[parentPath]
        if (parentEntries == null) {
            return@withContext Result.Error(IllegalStateException("Parent directory not found"))
        }

        // Проверяем, не существует ли уже
        if (parentEntries.any { it.name == dirName }) {
            return@withContext Result.Error(IllegalStateException("Directory already exists"))
        }

        // Добавляем в родительскую директорию
        val newEntry = FileEntry(
            name = dirName,
            path = path,
            isDirectory = true,
            size = 4096,
            permissions = "drwxr-xr-x",
            modifiedTime = System.currentTimeMillis()
        )
        parentEntries.add(newEntry)

        // Создаём пустую директорию
        virtualFileSystem[path] = mutableListOf()

        Result.Success(Unit)
    }

    suspend fun deleteFile(path: String, isDirectory: Boolean): Result<Unit> = withContext(ioDispatcher) {
        if (!isOpened) {
            return@withContext Result.Error(IllegalStateException("SFTP not opened"))
        }

        delay(200) // Эмуляция задержки

        val parentPath = path.substringBeforeLast("/").ifEmpty { "/" }
        val fileName = path.substringAfterLast("/")

        val parentEntries = virtualFileSystem[parentPath]
        if (parentEntries == null) {
            return@withContext Result.Error(IllegalStateException("Parent directory not found"))
        }

        // Удаляем из родительской директории
        val removed = parentEntries.removeIf { it.name == fileName }
        if (!removed) {
            return@withContext Result.Error(IllegalStateException("File not found"))
        }

        // Если это директория, удаляем её содержимое
        if (isDirectory) {
            virtualFileSystem.remove(path)
            // Удаляем все вложенные директории
            virtualFileSystem.keys.filter { it.startsWith("$path/") }.forEach {
                virtualFileSystem.remove(it)
            }
        }

        Result.Success(Unit)
    }

    fun downloadFile(remotePath: String, localPath: String): Flow<TransferProgress> = flow {
        if (!isOpened) {
            throw IllegalStateException("SFTP not opened")
        }

        // Находим размер файла
        val parentPath = remotePath.substringBeforeLast("/").ifEmpty { "/" }
        val fileName = remotePath.substringAfterLast("/")
        val entries = virtualFileSystem[parentPath] ?: throw IllegalStateException("Directory not found")
        val file = entries.find { it.name == fileName } ?: throw IllegalStateException("File not found")

        if (file.isDirectory) {
            throw IllegalStateException("Cannot download directory")
        }

        val totalBytes = file.size
        val chunkSize = 8192L
        var bytesTransferred = 0L

        emit(TransferProgress(0, totalBytes, false))

        // Эмуляция скачивания
        while (bytesTransferred < totalBytes) {
            delay(50) // Эмуляция сетевой задержки
            bytesTransferred = minOf(bytesTransferred + chunkSize, totalBytes)
            emit(TransferProgress(bytesTransferred, totalBytes, bytesTransferred >= totalBytes))
        }
    }.flowOn(ioDispatcher)

    fun uploadFile(localPath: String, remotePath: String): Flow<TransferProgress> = flow {
        if (!isOpened) {
            throw IllegalStateException("SFTP not opened")
        }

        // Эмуляция размера локального файла (10KB - 100KB)
        val totalBytes = (10240L..102400L).random()
        val chunkSize = 8192L
        var bytesTransferred = 0L

        emit(TransferProgress(0, totalBytes, false))

        // Эмуляция загрузки
        while (bytesTransferred < totalBytes) {
            delay(50) // Эмуляция сетевой задержки
            bytesTransferred = minOf(bytesTransferred + chunkSize, totalBytes)
            emit(TransferProgress(bytesTransferred, totalBytes, false))
        }

        // Добавляем файл в виртуальную ФС
        val parentPath = remotePath.substringBeforeLast("/").ifEmpty { "/" }
        val fileName = remotePath.substringAfterLast("/")

        val parentEntries = virtualFileSystem.getOrPut(parentPath) { mutableListOf() }

        // Удаляем старую версию, если есть
        parentEntries.removeIf { it.name == fileName }

        // Добавляем новый файл
        parentEntries.add(
            FileEntry(
                name = fileName,
                path = remotePath,
                isDirectory = false,
                size = totalBytes,
                permissions = "-rw-r--r--",
                modifiedTime = System.currentTimeMillis()
            )
        )

        emit(TransferProgress(totalBytes, totalBytes, true))
    }.flowOn(ioDispatcher)

    fun setCurrentUser(username: String) {
        currentUser = username.ifBlank { "demo" }
    }
}
