package com.ruslan.terminalssh.feature.sftp

import com.ruslan.terminalssh.domain.model.FileEntry
import com.ruslan.terminalssh.domain.repository.TransferProgress

data class SftpState(
    val isLoading: Boolean = true,
    val currentPath: String = "/",
    val files: List<FileEntry> = emptyList(),
    val pathHistory: List<String> = listOf("/"),
    val error: String? = null,

    // Диалоги
    val showCreateDirectoryDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val fileToDelete: FileEntry? = null,

    // Контекстное меню (BottomSheet)
    val showContextMenu: Boolean = false,
    val selectedFile: FileEntry? = null,

    // FAB меню
    val showFabMenu: Boolean = false,

    // Прогресс передачи
    val transferProgress: TransferProgress? = null,
    val isTransferring: Boolean = false,
    val transferFileName: String = "",
    val transferType: TransferType = TransferType.NONE
)

enum class TransferType {
    NONE, DOWNLOAD, UPLOAD
}

sealed class SftpIntent {
    // Навигация
    data object LoadCurrentDirectory : SftpIntent()
    data class NavigateToDirectory(val path: String) : SftpIntent()
    data object NavigateUp : SftpIntent()
    data class NavigateToPathSegment(val index: Int) : SftpIntent()
    data object Refresh : SftpIntent()
    data object DismissError : SftpIntent()
    data object Close : SftpIntent()

    // CRUD операции
    data class CreateDirectory(val name: String) : SftpIntent()
    data class DeleteFile(val file: FileEntry) : SftpIntent()
    data class DownloadFile(val file: FileEntry) : SftpIntent()
    data class UploadFile(val localPath: String, val fileName: String) : SftpIntent()

    // Диалоги
    data object ShowCreateDirectoryDialog : SftpIntent()
    data object HideCreateDirectoryDialog : SftpIntent()
    data class ShowDeleteConfirmDialog(val file: FileEntry) : SftpIntent()
    data object HideDeleteConfirmDialog : SftpIntent()

    // Контекстное меню
    data class ShowContextMenu(val file: FileEntry) : SftpIntent()
    data object HideContextMenu : SftpIntent()

    // FAB меню
    data object ToggleFabMenu : SftpIntent()
    data object HideFabMenu : SftpIntent()
    data object RequestUploadFile : SftpIntent()
}

sealed class SftpEffect {
    data object NavigateBack : SftpEffect()
    data class ShowError(val message: String) : SftpEffect()
    data class ShowSuccess(val message: String) : SftpEffect()
    data object PickFileForUpload : SftpEffect()
}
