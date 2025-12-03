package com.ruslan.terminalssh.feature.terminal.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruslan.terminalssh.domain.model.SavedConnection

@Composable
fun ConnectScreen(
    onNavigateToTerminal: (Long) -> Unit,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ConnectEffect.NavigateToTerminal -> onNavigateToTerminal(effect.connectionId)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.handleIntent(ConnectIntent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ConnectContent(
            state = state,
            onIntent = viewModel::handleIntent,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun ConnectContent(
    state: ConnectState,
    onIntent: (ConnectIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "SSH Connection",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.savedConnections.isNotEmpty()) {
            item {
                SavedConnectionsList(
                    connections = state.savedConnections,
                    selectedConnectionId = state.selectedConnectionId,
                    onSelectConnection = { onIntent(ConnectIntent.SelectConnection(it)) },
                    onDeleteConnection = { onIntent(ConnectIntent.DeleteConnection(it)) },
                    onAddNew = { onIntent(ConnectIntent.ClearSelection) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            ConnectionForm(
                state = state,
                onIntent = onIntent
            )
        }
    }
}

@Composable
private fun SavedConnectionsList(
    connections: List<SavedConnection>,
    selectedConnectionId: Long?,
    onSelectConnection: (SavedConnection) -> Unit,
    onDeleteConnection: (SavedConnection) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Saved Connections",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            connections.forEach { connection ->
                SavedConnectionItem(
                    connection = connection,
                    isSelected = connection.id == selectedConnectionId,
                    onSelect = { onSelectConnection(connection) },
                    onDelete = { onDeleteConnection(connection) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddNew() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add new connection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SavedConnectionItem(
    connection: SavedConnection,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onSelect() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = connection.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${connection.username}@${connection.host}:${connection.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ConnectionForm(
    state: ConnectState,
    onIntent: (ConnectIntent) -> Unit
) {
    OutlinedTextField(
        value = state.host,
        onValueChange = { onIntent(ConnectIntent.UpdateHost(it)) },
        label = { Text("Host") },
        placeholder = { Text("192.168.1.1") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !state.isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.port,
        onValueChange = { onIntent(ConnectIntent.UpdatePort(it)) },
        label = { Text("Port") },
        placeholder = { Text("22") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !state.isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.username,
        onValueChange = { onIntent(ConnectIntent.UpdateUsername(it)) },
        label = { Text("Username") },
        placeholder = { Text("admin") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !state.isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.password,
        onValueChange = { onIntent(ConnectIntent.UpdatePassword(it)) },
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        enabled = !state.isLoading
    )

    AnimatedVisibility(visible = state.selectedConnectionId == null) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.isLoading) {
                        onIntent(ConnectIntent.ToggleSaveConnection)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.saveConnection,
                    onCheckedChange = { onIntent(ConnectIntent.ToggleSaveConnection) },
                    enabled = !state.isLoading
                )
                Text(
                    text = "Save connection",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = state.saveConnection) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.connectionName,
                        onValueChange = { onIntent(ConnectIntent.UpdateConnectionName(it)) },
                        label = { Text("Connection name (optional)") },
                        placeholder = { Text("My Server") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isLoading
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { onIntent(ConnectIntent.Connect) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !state.isLoading
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Connect")
        }
    }
}
