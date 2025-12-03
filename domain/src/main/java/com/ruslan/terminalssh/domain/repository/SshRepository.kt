package com.ruslan.terminalssh.domain.repository

import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.model.ConnectionState
import com.ruslan.terminalssh.domain.model.TerminalOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SshRepository {
    val connectionState: StateFlow<ConnectionState>
    val terminalOutput: Flow<TerminalOutput>

    suspend fun connect(config: ConnectionConfig): Result<Unit>
    suspend fun disconnect()
    suspend fun executeCommand(command: String): Result<Unit>
}
