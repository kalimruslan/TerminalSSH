package com.ruslan.terminalssh.data.repository

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.data.ssh.DemoSshClient
import com.ruslan.terminalssh.data.ssh.SshClient
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.model.ConnectionState
import com.ruslan.terminalssh.domain.model.TerminalOutput
import com.ruslan.terminalssh.domain.repository.SshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshRepositoryImpl @Inject constructor(
    private val sshClient: SshClient,
    private val demoSshClient: DemoSshClient,
    private val sftpRepositoryImpl: SftpRepositoryImpl
) : SshRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val terminalOutput: Flow<TerminalOutput> = merge(
        sshClient.output,
        demoSshClient.output
    )

    private var isDemoMode = false

    override suspend fun connect(config: ConnectionConfig): Result<Unit> {
        _connectionState.value = ConnectionState.Connecting
        isDemoMode = config.isDemoMode
        sftpRepositoryImpl.setDemoMode(isDemoMode)

        val result = if (isDemoMode) {
            demoSshClient.connect(config)
        } else {
            sshClient.connect(config)
        }

        return when (result) {
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
        if (isDemoMode) {
            demoSshClient.disconnect()
        } else {
            sshClient.disconnect()
        }
        isDemoMode = false
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun executeCommand(command: String): Result<Unit> {
        return if (isDemoMode) {
            demoSshClient.executeCommand(command)
        } else {
            sshClient.executeCommand(command)
        }
    }
}
