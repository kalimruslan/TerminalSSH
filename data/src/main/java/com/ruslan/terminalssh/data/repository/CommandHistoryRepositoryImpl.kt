package com.ruslan.terminalssh.data.repository

import com.ruslan.terminalssh.data.database.dao.CommandHistoryDao
import com.ruslan.terminalssh.data.database.entity.CommandHistoryEntity
import com.ruslan.terminalssh.domain.model.CommandHistory
import com.ruslan.terminalssh.domain.repository.CommandHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandHistoryRepositoryImpl @Inject constructor(
    private val commandHistoryDao: CommandHistoryDao
) : CommandHistoryRepository {

    companion object {
        private const val MAX_HISTORY_SIZE = 500
    }

    override fun getHistoryForConnection(connectionId: Long): Flow<List<CommandHistory>> {
        return commandHistoryDao.getHistoryForConnection(connectionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addCommand(connectionId: Long, command: String) {
        val entity = CommandHistoryEntity(
            connectionId = connectionId,
            command = command,
            executedAt = System.currentTimeMillis()
        )
        commandHistoryDao.insert(entity)
        commandHistoryDao.trimHistory(connectionId, MAX_HISTORY_SIZE)
    }

    override suspend fun clearHistory(connectionId: Long) {
        commandHistoryDao.clearHistory(connectionId)
    }

    override suspend fun getCommandAt(connectionId: Long, index: Int): CommandHistory? {
        return commandHistoryDao.getCommandAt(connectionId, index)?.toDomain()
    }

    override suspend fun getHistorySize(connectionId: Long): Int {
        return commandHistoryDao.getHistorySize(connectionId)
    }

    private fun CommandHistoryEntity.toDomain() = CommandHistory(
        id = id,
        connectionId = connectionId,
        command = command,
        executedAt = executedAt
    )
}
