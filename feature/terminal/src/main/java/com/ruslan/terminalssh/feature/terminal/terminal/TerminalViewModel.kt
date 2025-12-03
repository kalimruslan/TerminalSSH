package com.ruslan.terminalssh.feature.terminal.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruslan.terminalssh.domain.model.ConnectionState
import com.ruslan.terminalssh.domain.model.FavoriteCommand
import com.ruslan.terminalssh.domain.repository.SshRepository
import com.ruslan.terminalssh.domain.usecase.AddToFavoritesUseCase
import com.ruslan.terminalssh.domain.usecase.DisconnectSshUseCase
import com.ruslan.terminalssh.domain.usecase.ExecuteCommandUseCase
import com.ruslan.terminalssh.domain.usecase.GetFavoriteCommandsUseCase
import com.ruslan.terminalssh.domain.usecase.RemoveFromFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SshRepository,
    private val executeCommandUseCase: ExecuteCommandUseCase,
    private val disconnectSshUseCase: DisconnectSshUseCase,
    private val getFavoriteCommandsUseCase: GetFavoriteCommandsUseCase,
    private val addToFavoritesUseCase: AddToFavoritesUseCase,
    private val removeFromFavoritesUseCase: RemoveFromFavoritesUseCase
) : ViewModel() {

    private val connectionId: Long = savedStateHandle.get<Long>("connectionId") ?: 0L

    private val _state = MutableStateFlow(TerminalState(connectionId = connectionId))
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val _effect = Channel<TerminalEffect>()
    val effect = _effect.receiveAsFlow()

    private val outputBuffer = mutableListOf<com.ruslan.terminalssh.domain.model.TerminalOutput>()
    private val bufferLock = Any()

    init {
        observeTerminalOutput()
        observeConnectionState()
        if (connectionId > 0) {
            observeFavoriteCommands()
        }
    }

    private fun observeTerminalOutput() {
        // Collect outputs into buffer
        viewModelScope.launch {
            repository.terminalOutput.collect { output ->
                synchronized(bufferLock) {
                    outputBuffer.add(output)
                }
            }
        }

        // Flush buffer to UI every 100ms
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                val toAdd = synchronized(bufferLock) {
                    if (outputBuffer.isEmpty()) {
                        emptyList()
                    } else {
                        val list = outputBuffer.toList()
                        outputBuffer.clear()
                        list
                    }
                }

                if (toAdd.isEmpty()) continue

                _state.update { currentState ->
                    // Filter duplicates by ID and by content (last 50 lines)
                    val existingIds = currentState.outputs.map { it.id }.toSet()
                    val recentTexts = currentState.outputs.takeLast(50).map { it.text.trim() }.toSet()

                    val newItems = toAdd.filter { output ->
                        output.id !in existingIds && output.text.trim() !in recentTexts
                    }
                    if (newItems.isEmpty()) return@update currentState

                    val maxOutputs = 100
                    val combined = currentState.outputs + newItems
                    val trimmed = if (combined.size > maxOutputs) {
                        combined.takeLast(maxOutputs)
                    } else {
                        combined
                    }
                    currentState.copy(outputs = trimmed)
                }
                _effect.send(TerminalEffect.ScrollToBottom)
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            repository.connectionState.collect { connectionState ->
                when (connectionState) {
                    is ConnectionState.Disconnected -> {
                        _state.update { it.copy(isConnected = false) }
                    }
                    is ConnectionState.Connected -> {
                        _state.update { it.copy(isConnected = true) }
                    }
                    is ConnectionState.Error -> {
                        _state.update { it.copy(isConnected = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeFavoriteCommands() {
        viewModelScope.launch {
            getFavoriteCommandsUseCase(connectionId).collect { commands ->
                _state.update { currentState ->
                    val isFavorite = commands.any { it.command == currentState.currentCommand.trim() }
                    currentState.copy(
                        favoriteCommands = commands,
                        isCurrentCommandFavorite = isFavorite
                    )
                }
            }
        }
    }

    fun handleIntent(intent: TerminalIntent) {
        when (intent) {
            is TerminalIntent.UpdateCommand -> updateCommand(intent.command)
            is TerminalIntent.ExecuteCommand -> executeCommand()
            is TerminalIntent.Disconnect -> disconnect()
            is TerminalIntent.ToggleFavoritesDropdown -> toggleFavoritesDropdown()
            is TerminalIntent.SelectFavoriteCommand -> selectFavoriteCommand(intent.command)
            is TerminalIntent.ToggleCurrentCommandFavorite -> toggleCurrentCommandFavorite()
        }
    }

    private fun updateCommand(command: String) {
        _state.update { currentState ->
            val isFavorite = currentState.favoriteCommands.any { it.command == command.trim() }
            currentState.copy(
                currentCommand = command,
                isCurrentCommandFavorite = isFavorite
            )
        }
    }

    private fun executeCommand() {
        val command = _state.value.currentCommand.trim()
        if (command.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(currentCommand = "", showFavoritesDropdown = false) }
            executeCommandUseCase(command)
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            disconnectSshUseCase()
            _effect.send(TerminalEffect.NavigateBack)
        }
    }

    private fun toggleFavoritesDropdown() {
        _state.update { it.copy(showFavoritesDropdown = !it.showFavoritesDropdown) }
    }

    private fun selectFavoriteCommand(command: FavoriteCommand) {
        _state.update {
            it.copy(
                currentCommand = command.command,
                showFavoritesDropdown = false,
                isCurrentCommandFavorite = true
            )
        }
    }

    private fun toggleCurrentCommandFavorite() {
        val currentCommand = _state.value.currentCommand.trim()
        if (currentCommand.isEmpty() || connectionId <= 0) return

        viewModelScope.launch {
            if (_state.value.isCurrentCommandFavorite) {
                removeFromFavoritesUseCase(connectionId, currentCommand)
            } else {
                addToFavoritesUseCase(connectionId, currentCommand)
            }
        }
    }
}
