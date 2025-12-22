package com.ruslan.terminalssh.feature.connect

import com.ruslan.terminalssh.domain.model.SavedConnection

data class ConnectState(
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedConnections: List<SavedConnection> = emptyList(),
    val selectedConnectionId: Long? = null,
    val saveConnection: Boolean = false,
    val connectionName: String = ""
)

sealed class ConnectIntent {
    data class UpdateHost(val host: String) : ConnectIntent()
    data class UpdatePort(val port: String) : ConnectIntent()
    data class UpdateUsername(val username: String) : ConnectIntent()
    data class UpdatePassword(val password: String) : ConnectIntent()
    data class UpdateConnectionName(val name: String) : ConnectIntent()
    data object Connect : ConnectIntent()
    data object ConnectDemo : ConnectIntent()
    data object DismissError : ConnectIntent()
    data class SelectConnection(val connection: SavedConnection) : ConnectIntent()
    data class DeleteConnection(val connection: SavedConnection) : ConnectIntent()
    data object ToggleSaveConnection : ConnectIntent()
    data object ClearSelection : ConnectIntent()
}

sealed class ConnectEffect {
    data class NavigateToTerminal(val connectionId: Long) : ConnectEffect()
}
