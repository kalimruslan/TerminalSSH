package com.ruslan.terminalssh.feature.sftp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.FileEntry
import com.ruslan.terminalssh.domain.usecase.CloseSftpChannelUseCase
import com.ruslan.terminalssh.domain.usecase.CreateDirectoryUseCase
import com.ruslan.terminalssh.domain.usecase.DeleteFileUseCase
import com.ruslan.terminalssh.domain.usecase.DownloadFileUseCase
import com.ruslan.terminalssh.domain.usecase.GetCurrentDirectoryUseCase
import com.ruslan.terminalssh.domain.usecase.ListDirectoryUseCase
import com.ruslan.terminalssh.domain.usecase.OpenSftpChannelUseCase
import com.ruslan.terminalssh.domain.usecase.UploadFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SftpViewModel @Inject constructor(
    private val openSftpChannelUseCase: OpenSftpChannelUseCase,
    private val closeSftpChannelUseCase: CloseSftpChannelUseCase,
    private val listDirectoryUseCase: ListDirectoryUseCase,
    private val getCurrentDirectoryUseCase: GetCurrentDirectoryUseCase,
    private val createDirectoryUseCase: CreateDirectoryUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val uploadFileUseCase: UploadFileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SftpState())
    val state: StateFlow<SftpState> = _state.asStateFlow()

    private val _effects = Channel<SftpEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        openChannelAndLoadDirectory()
    }

    fun handleIntent(intent: SftpIntent) {
        when (intent) {
            // Навигация
            is SftpIntent.LoadCurrentDirectory -> loadCurrentDirectory()
            is SftpIntent.NavigateToDirectory -> navigateToDirectory(intent.path)
            is SftpIntent.NavigateUp -> navigateUp()
            is SftpIntent.NavigateToPathSegment -> navigateToPathSegment(intent.index)
            is SftpIntent.Refresh -> refresh()
            is SftpIntent.DismissError -> dismissError()
            is SftpIntent.Close -> close()

            // CRUD операции
            is SftpIntent.CreateDirectory -> createDirectory(intent.name)
            is SftpIntent.DeleteFile -> deleteFile(intent.file)
            is SftpIntent.DownloadFile -> downloadFile(intent.file)
            is SftpIntent.UploadFile -> uploadFile(intent.localPath, intent.fileName)

            // Диалоги
            is SftpIntent.ShowCreateDirectoryDialog -> _state.update { it.copy(showCreateDirectoryDialog = true) }
            is SftpIntent.HideCreateDirectoryDialog -> _state.update { it.copy(showCreateDirectoryDialog = false) }
            is SftpIntent.ShowDeleteConfirmDialog -> _state.update { it.copy(showDeleteConfirmDialog = true, fileToDelete = intent.file) }
            is SftpIntent.HideDeleteConfirmDialog -> _state.update { it.copy(showDeleteConfirmDialog = false, fileToDelete = null) }

            // Контекстное меню
            is SftpIntent.ShowContextMenu -> _state.update { it.copy(showContextMenu = true, selectedFile = intent.file) }
            is SftpIntent.HideContextMenu -> _state.update { it.copy(showContextMenu = false, selectedFile = null) }

            // FAB меню
            is SftpIntent.ToggleFabMenu -> _state.update { it.copy(showFabMenu = !it.showFabMenu) }
            is SftpIntent.HideFabMenu -> _state.update { it.copy(showFabMenu = false) }
            is SftpIntent.RequestUploadFile -> {
                _state.update { it.copy(showFabMenu = false) }
                viewModelScope.launch { _effects.send(SftpEffect.PickFileForUpload) }
            }
        }
    }

    private fun openChannelAndLoadDirectory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = openSftpChannelUseCase()) {
                is Result.Success -> {
                    loadCurrentDirectory()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to open SFTP channel"
                        )
                    }
                }
                is Result.Loading -> { /* Ignore loading state */ }
            }
        }
    }

    private fun loadCurrentDirectory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val pathResult = getCurrentDirectoryUseCase()) {
                is Result.Success -> {
                    val path = pathResult.data
                    loadDirectory(path)
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = pathResult.exception.message ?: "Failed to get current directory"
                        )
                    }
                }
                is Result.Loading -> { /* Ignore loading state */ }
            }
        }
    }

    private fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadDirectory(path)
        }
    }

    private fun navigateUp() {
        val currentPath = _state.value.currentPath
        if (currentPath == "/") return

        val parentPath = currentPath.substringBeforeLast("/").ifEmpty { "/" }
        navigateToDirectory(parentPath)
    }

    private fun navigateToPathSegment(index: Int) {
        val segments = _state.value.pathHistory
        if (index < 0 || index >= segments.size) return

        val targetPath = if (index == 0) "/" else segments.take(index + 1).joinToString("/").replace("//", "/")
        navigateToDirectory(targetPath)
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadDirectory(_state.value.currentPath)
        }
    }

    private suspend fun loadDirectory(path: String) {
        when (val result = listDirectoryUseCase(path)) {
            is Result.Success -> {
                val pathSegments = buildPathHistory(path)
                _state.update {
                    it.copy(
                        isLoading = false,
                        currentPath = path,
                        files = result.data,
                        pathHistory = pathSegments,
                        error = null
                    )
                }
            }
            is Result.Error -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Failed to list directory"
                    )
                }
            }
            is Result.Loading -> { /* Ignore loading state */ }
        }
    }

    private fun buildPathHistory(path: String): List<String> {
        if (path == "/") return listOf("/")

        val segments = mutableListOf("/")
        val parts = path.trim('/').split("/")
        parts.forEach { part ->
            if (part.isNotEmpty()) {
                segments.add(part)
            }
        }
        return segments
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun close() {
        viewModelScope.launch {
            closeSftpChannelUseCase()
            _effects.send(SftpEffect.NavigateBack)
        }
    }

    // CRUD операции
    private fun createDirectory(name: String) {
        val currentPath = _state.value.currentPath
        val newPath = "$currentPath/$name".replace("//", "/")

        viewModelScope.launch {
            _state.update { it.copy(showCreateDirectoryDialog = false) }

            when (val result = createDirectoryUseCase(newPath)) {
                is Result.Success -> {
                    _effects.send(SftpEffect.ShowSuccess("Directory created: $name"))
                    refresh()
                }
                is Result.Error -> {
                    _effects.send(SftpEffect.ShowError(result.exception.message ?: "Failed to create directory"))
                }
                is Result.Loading -> { /* Ignore */ }
            }
        }
    }

    private fun deleteFile(file: FileEntry) {
        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmDialog = false, fileToDelete = null) }

            when (val result = deleteFileUseCase(file.path, file.isDirectory)) {
                is Result.Success -> {
                    _effects.send(SftpEffect.ShowSuccess("Deleted: ${file.name}"))
                    refresh()
                }
                is Result.Error -> {
                    _effects.send(SftpEffect.ShowError(result.exception.message ?: "Failed to delete"))
                }
                is Result.Loading -> { /* Ignore */ }
            }
        }
    }

    private fun downloadFile(file: FileEntry) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    showContextMenu = false,
                    selectedFile = null,
                    isTransferring = true,
                    transferFileName = file.name,
                    transferType = TransferType.DOWNLOAD,
                    transferProgress = null
                )
            }

            // localPath будет передан из UI через MediaStore
            val localPath = "" // Placeholder - реальный путь будет определён в UI

            downloadFileUseCase(file.path, localPath)
                .catch { e ->
                    _state.update {
                        it.copy(
                            isTransferring = false,
                            transferProgress = null,
                            transferType = TransferType.NONE
                        )
                    }
                    _effects.send(SftpEffect.ShowError(e.message ?: "Download failed"))
                }
                .collect { progress ->
                    _state.update { it.copy(transferProgress = progress) }

                    if (progress.isComplete) {
                        _state.update {
                            it.copy(
                                isTransferring = false,
                                transferProgress = null,
                                transferType = TransferType.NONE
                            )
                        }
                        _effects.send(SftpEffect.ShowSuccess("Downloaded: ${file.name}"))
                    }
                }
        }
    }

    private fun uploadFile(localPath: String, fileName: String) {
        val currentPath = _state.value.currentPath
        val remotePath = "$currentPath/$fileName".replace("//", "/")

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isTransferring = true,
                    transferFileName = fileName,
                    transferType = TransferType.UPLOAD,
                    transferProgress = null
                )
            }

            uploadFileUseCase(localPath, remotePath)
                .catch { e ->
                    _state.update {
                        it.copy(
                            isTransferring = false,
                            transferProgress = null,
                            transferType = TransferType.NONE
                        )
                    }
                    _effects.send(SftpEffect.ShowError(e.message ?: "Upload failed"))
                }
                .collect { progress ->
                    _state.update { it.copy(transferProgress = progress) }

                    if (progress.isComplete) {
                        _state.update {
                            it.copy(
                                isTransferring = false,
                                transferProgress = null,
                                transferType = TransferType.NONE
                            )
                        }
                        _effects.send(SftpEffect.ShowSuccess("Uploaded: $fileName"))
                        refresh()
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            closeSftpChannelUseCase()
        }
    }
}
