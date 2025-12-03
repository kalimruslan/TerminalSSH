package com.ruslan.terminalssh.data.repository

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.data.ssh.SshClient
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.model.ConnectionState
import com.ruslan.terminalssh.domain.model.TerminalOutput
import com.ruslan.terminalssh.domain.repository.SshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshRepositoryImpl @Inject constructor(
    private val sshClient: SshClient
) : SshRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val terminalOutput: Flow<TerminalOutput> = sshClient.output

    override suspend fun connect(config: ConnectionConfig): Result<Unit> {
        _connectionState.value = ConnectionState.Connecting

        return when (val result = sshClient.connect(config)) {
            is Result.Success -> {
                _connectionState.value = ConnectionState.Connected
                Result.Success(Unit)
            }
            is Result.Error -> {
                _connectionState.value = ConnectionState.Error(result.exception.message ?: "Connection failed")
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun disconnect() {
        sshClient.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun executeCommand(command: String): Result<Unit> {
        return sshClient.executeCommand(command)
    }
}
