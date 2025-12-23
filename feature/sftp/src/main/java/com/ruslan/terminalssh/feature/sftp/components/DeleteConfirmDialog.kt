package com.ruslan.terminalssh.feature.sftp.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ruslan.terminalssh.domain.model.FileEntry

@Composable
fun DeleteConfirmDialog(
    file: FileEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val itemType = if (file.isDirectory) "directory" else "file"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $itemType") },
        text = {
            Text("Are you sure you want to delete \"${file.name}\"?\n\nThis action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
