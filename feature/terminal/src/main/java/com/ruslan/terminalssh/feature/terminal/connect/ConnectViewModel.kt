package com.ruslan.terminalssh.feature.terminal.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.model.SavedConnection
import com.ruslan.terminalssh.domain.usecase.ConnectSshUseCase
import com.ruslan.terminalssh.domain.usecase.DeleteConnectionUseCase
import com.ruslan.terminalssh.domain.usecase.GetSavedConnectionsUseCase
import com.ruslan.terminalssh.domain.usecase.SaveConnectionUseCase
import com.ruslan.terminalssh.domain.usecase.UpdateConnectionLastUsedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val connectSshUseCase: ConnectSshUseCase,
    private val getSavedConnectionsUseCase: GetSavedConnectionsUseCase,
    private val saveConnectionUseCase: SaveConnectionUseCase,
    private val deleteConnectionUseCase: DeleteConnectionUseCase,
    private val updateConnectionLastUsedUseCase: UpdateConnectionLastUsedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectState())
    val state: StateFlow<ConnectState> = _state.asStateFlow()

    private val _effect = Channel<ConnectEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadSavedConnections()
    }

    private fun loadSavedConnections() {
        viewModelScope.launch {
            getSavedConnectionsUseCase().collect { connections ->
                _state.update { it.copy(savedConnections = connections) }
            }
        }
    }

    fun handleIntent(intent: ConnectIntent) {
        when (intent) {
            is ConnectIntent.UpdateHost -> _state.update { it.copy(host = intent.host) }
            is ConnectIntent.UpdatePort -> _state.update { it.copy(port = intent.port) }
            is ConnectIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is ConnectIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is ConnectIntent.UpdateConnectionName -> _state.update { it.copy(connectionName = intent.name) }
            is ConnectIntent.Connect -> connect()
            is ConnectIntent.DismissError -> _state.update { it.copy(error = null) }
            is ConnectIntent.SelectConnection -> selectConnection(intent.connection)
            is ConnectIntent.DeleteConnection -> deleteConnection(intent.connection)
            is ConnectIntent.ToggleSaveConnection -> _state.update { it.copy(saveConnection = !it.saveConnection) }
            is ConnectIntent.ClearSelection -> clearSelection()
        }
    }

    private fun selectConnection(connection: SavedConnection) {
        _state.update {
            it.copy(
                selectedConnectionId = connection.id,
                host = connection.host,
                port = connection.port.toString(),
                username = connection.username,
                password = connection.password,
                connectionName = connection.name,
                saveConnection = false
            )
        }
    }

    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedConnectionId = null,
                host = "",
                port = "22",
                username = "",
                password = "",
                connectionName = "",
                saveConnection = false
            )
        }
    }

    private fun deleteConnection(connection: SavedConnection) {
        viewModelScope.launch {
            deleteConnectionUseCase(connection)
            if (_state.value.selectedConnectionId == connection.id) {
                clearSelection()
            }
        }
    }

    private fun connect() {
        val currentState = _state.value

        if (currentState.host.isBlank()) {
            _state.update { it.copy(error = "Host is required") }
            return
        }
        if (currentState.username.isBlank()) {
            _state.update { it.copy(error = "Username is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val config = ConnectionConfig(
                host = currentState.host.trim(),
                port = currentState.port.toIntOrNull() ?: 22,
                username = currentState.username.trim(),
                password = currentState.password
            )

            when (val result = connectSshUseCase(config)) {
                is Result.Success -> {
                    val connectionId = saveOrUpdateConnection(currentState)
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(ConnectEffect.NavigateToTerminal(connectionId))
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.exception.message ?: "Connection failed")
                    }
                }
                is Result.Loading -> {
                    // Already loading
                }
            }
        }
    }

    private suspend fun saveOrUpdateConnection(currentState: ConnectState): Long {
        return if (currentState.selectedConnectionId != null) {
            updateConnectionLastUsedUseCase(currentState.selectedConnectionId)
            currentState.selectedConnectionId
        } else if (currentState.saveConnection) {
            val name = currentState.connectionName.ifBlank {
                "${currentState.username}@${currentState.host}"
            }
            val connection = SavedConnection(
                name = name,
                host = currentState.host.trim(),
                port = currentState.port.toIntOrNull() ?: 22,
                username = currentState.username.trim(),
                password = currentState.password,
                lastUsedAt = System.currentTimeMillis()
            )
            saveConnectionUseCase(connection)
        } else {
            0L
        }
    }
}
