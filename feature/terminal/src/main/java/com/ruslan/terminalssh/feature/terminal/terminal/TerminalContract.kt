package com.ruslan.terminalssh.feature.terminal.terminal

import com.ruslan.terminalssh.domain.model.FavoriteCommand
import com.ruslan.terminalssh.domain.model.TerminalOutput

data class TerminalState(
    val outputs: List<TerminalOutput> = emptyList(),
    val currentCommand: String = "",
    val isConnected: Boolean = true,
    val connectionId: Long = 0,
    val favoriteCommands: List<FavoriteCommand> = emptyList(),
    val showFavoritesDropdown: Boolean = false,
    val isCurrentCommandFavorite: Boolean = false
)

sealed class TerminalIntent {
    data class UpdateCommand(val command: String) : TerminalIntent()
    data object ExecuteCommand : TerminalIntent()
    data object Disconnect : TerminalIntent()
    data object ToggleFavoritesDropdown : TerminalIntent()
    data class SelectFavoriteCommand(val command: FavoriteCommand) : TerminalIntent()
    data object ToggleCurrentCommandFavorite : TerminalIntent()
}

sealed class TerminalEffect {
    data object NavigateBack : TerminalEffect()
    data object ScrollToBottom : TerminalEffect()
}
