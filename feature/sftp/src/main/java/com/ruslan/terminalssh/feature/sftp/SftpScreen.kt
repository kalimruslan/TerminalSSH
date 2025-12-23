package com.ruslan.terminalssh.feature.sftp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruslan.terminalssh.feature.sftp.components.CreateDirectoryDialog
import com.ruslan.terminalssh.feature.sftp.components.DeleteConfirmDialog
import com.ruslan.terminalssh.feature.sftp.components.FileContextMenu
import com.ruslan.terminalssh.feature.sftp.components.FileItem
import com.ruslan.terminalssh.feature.sftp.components.PathBreadcrumb
import com.ruslan.terminalssh.feature.sftp.components.TransferProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpScreen(
    onNavigateBack: () -> Unit,
    viewModel: SftpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Get file name from URI
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                if (nameIndex >= 0) c.getString(nameIndex) else "uploaded_file"
            } ?: "uploaded_file"

            // Copy to cache and get path
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.let { stream ->
                val cacheFile = java.io.File(context.cacheDir, fileName)
                cacheFile.outputStream().use { output ->
                    stream.copyTo(output)
                }
                viewModel.handleIntent(SftpIntent.UploadFile(cacheFile.absolutePath, fileName))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SftpEffect.NavigateBack -> onNavigateBack()
                is SftpEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is SftpEffect.ShowSuccess -> snackbarHostState.showSnackbar(effect.message)
                is SftpEffect.PickFileForUpload -> filePickerLauncher.launch("*/*")
            }
        }
    }

    // Dialogs
    if (state.showCreateDirectoryDialog) {
        CreateDirectoryDialog(
            onConfirm = { name -> viewModel.handleIntent(SftpIntent.CreateDirectory(name)) },
            onDismiss = { viewModel.handleIntent(SftpIntent.HideCreateDirectoryDialog) }
        )
    }

    if (state.showDeleteConfirmDialog && state.fileToDelete != null) {
        DeleteConfirmDialog(
            file = state.fileToDelete!!,
            onConfirm = { viewModel.handleIntent(SftpIntent.DeleteFile(state.fileToDelete!!)) },
            onDismiss = { viewModel.handleIntent(SftpIntent.HideDeleteConfirmDialog) }
        )
    }

    // Context menu (BottomSheet)
    if (state.showContextMenu && state.selectedFile != null) {
        FileContextMenu(
            file = state.selectedFile!!,
            onDownload = { viewModel.handleIntent(SftpIntent.DownloadFile(state.selectedFile!!)) },
            onDelete = {
                viewModel.handleIntent(SftpIntent.HideContextMenu)
                viewModel.handleIntent(SftpIntent.ShowDeleteConfirmDialog(state.selectedFile!!))
            },
            onDismiss = { viewModel.handleIntent(SftpIntent.HideContextMenu) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sftp_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(SftpIntent.Close) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sftp_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.handleIntent(SftpIntent.Refresh) },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.sftp_refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Mini FABs (visible when expanded)
                AnimatedVisibility(
                    visible = state.showFabMenu,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        SmallFloatingActionButton(
                            onClick = {
                                viewModel.handleIntent(SftpIntent.HideFabMenu)
                                viewModel.handleIntent(SftpIntent.RequestUploadFile)
                            }
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Upload file")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SmallFloatingActionButton(
                            onClick = {
                                viewModel.handleIntent(SftpIntent.HideFabMenu)
                                viewModel.handleIntent(SftpIntent.ShowCreateDirectoryDialog)
                            }
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Create directory")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { viewModel.handleIntent(SftpIntent.ToggleFabMenu) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                PathBreadcrumb(
                    pathSegments = state.pathHistory,
                    onSegmentClick = { index ->
                        viewModel.handleIntent(SftpIntent.NavigateToPathSegment(index))
                    }
                )
            }

            HorizontalDivider()

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = { viewModel.handleIntent(SftpIntent.Refresh) }
                            ) {
                                Text(stringResource(R.string.sftp_retry))
                            }
                        }
                    }
                }

                state.files.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.sftp_empty_directory),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.currentPath != "/") {
                            item {
                                ParentDirectoryItem(
                                    onClick = { viewModel.handleIntent(SftpIntent.NavigateUp) }
                                )
                                HorizontalDivider()
                            }
                        }

                        items(
                            items = state.files,
                            key = { it.path }
                        ) { file ->
                            FileItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.handleIntent(SftpIntent.NavigateToDirectory(file.path))
                                    }
                                },
                                onLongClick = {
                                    viewModel.handleIntent(SftpIntent.ShowContextMenu(file))
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
            } // End of Column

            // Transfer progress overlay
            TransferProgressBar(
                isVisible = state.isTransferring,
                fileName = state.transferFileName,
                transferType = state.transferType,
                progress = state.transferProgress,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun ParentDirectoryItem(
    onClick: () -> Unit
) {
    FileItem(
        file = com.ruslan.terminalssh.domain.model.FileEntry(
            name = "..",
            path = "..",
            isDirectory = true,
            size = 0,
            permissions = "",
            modifiedTime = 0
        ),
        onClick = onClick
    )
}
