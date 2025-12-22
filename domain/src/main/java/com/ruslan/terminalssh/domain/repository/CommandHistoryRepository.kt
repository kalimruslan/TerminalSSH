package com.ruslan.terminalssh.domain.repository

import com.ruslan.terminalssh.domain.model.CommandHistory
import kotlinx.coroutines.flow.Flow

interface CommandHistoryRepository {
    fun getHistoryForConnection(connectionId: Long): Flow<List<CommandHistory>>
    suspend fun addCommand(connectionId: Long, command: String)
    suspend fun clearHistory(connectionId: Long)
    suspend fun getCommandAt(connectionId: Long, index: Int): CommandHistory?
    suspend fun getHistorySize(connectionId: Long): Int
}
